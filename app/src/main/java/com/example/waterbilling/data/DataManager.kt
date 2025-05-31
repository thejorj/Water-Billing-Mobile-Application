package com.example.waterbilling.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException
import com.example.waterbilling.data.ExcelSheet
import android.net.Uri
import com.example.waterbilling.utils.ExcelUtils

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "water_billing")

class DataManager(private val context: Context) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
    }
    
    private object PreferencesKeys {
        val EXCEL_DATA = stringPreferencesKey("excel_data")
        val LAST_MODIFIED = longPreferencesKey("last_modified")
        val AUTOSAVE_ENABLED = booleanPreferencesKey("autosave_enabled")
        val BILL_COUNT = intPreferencesKey("bill_count")
        val CURRENT_FILENAME = stringPreferencesKey("current_filename")
    }
    
    val isAutosaveEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTOSAVE_ENABLED] ?: true
        }

    private val billCount: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BILL_COUNT] ?: 0
        }

    val currentFilename: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CURRENT_FILENAME]
        }

    suspend fun setCurrentFilename(filename: String?) {
        context.dataStore.edit { preferences ->
            if (filename != null) {
                preferences[PreferencesKeys.CURRENT_FILENAME] = filename
            } else {
                preferences.remove(PreferencesKeys.CURRENT_FILENAME)
            }
        }
    }

    suspend fun setAutosaveEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOSAVE_ENABLED] = enabled
        }
    }

    private suspend fun incrementBillCount() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[PreferencesKeys.BILL_COUNT] ?: 0
            preferences[PreferencesKeys.BILL_COUNT] = (currentCount + 1) % 5
        }
    }

    private suspend fun resetBillCount() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BILL_COUNT] = 0
        }
    }

    private suspend fun shouldAutoSave(): Boolean {
        val count = billCount.first()
        val isEnabled = isAutosaveEnabled.first()
        return isEnabled && count == 4 // Will become 0 after increment
    }

    private suspend fun createAutoSaveFile() {
        val currentData = getCurrentData() ?: return
        val filename = currentFilename.first() ?: return
        
        // Create a consistent autosave filename for each original file
        val autoSaveFileName = "AS_${filename}"
        
        // Use Downloads directory for better visibility
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val autoSaveFile = java.io.File(downloadsDir, autoSaveFileName)
        
        try {
            val uri = android.net.Uri.fromFile(autoSaveFile)
            ExcelUtils.exportExcel(context, uri, currentData)
            
            // Show notification about successful auto-save
            android.widget.Toast.makeText(
                context,
                "Auto-saved to: $autoSaveFileName",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Failed to create auto-save file: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }

    suspend fun saveExcelData(excelSheet: ExcelSheet, forceWrite: Boolean = false, isSuccessfulBill: Boolean = false) {
        try {
            if (forceWrite || isAutosaveEnabled.first()) {
                val jsonString = json.encodeToString(excelSheet)
                context.dataStore.edit { preferences ->
                    preferences[PreferencesKeys.EXCEL_DATA] = jsonString
                    preferences[PreferencesKeys.LAST_MODIFIED] = System.currentTimeMillis()
                }

                if (isSuccessfulBill) {
                    if (shouldAutoSave()) {
                        createAutoSaveFile()
                    }
                    incrementBillCount()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw IOException("Failed to save data: ${e.message}")
        }
    }
    
    val excelData: Flow<ExcelSheet?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            try {
                preferences[PreferencesKeys.EXCEL_DATA]?.let { jsonString ->
                    json.decodeFromString<ExcelSheet>(jsonString)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    suspend fun clearData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        resetBillCount()
    }

    suspend fun getCurrentData(): ExcelSheet? {
        return try {
            excelData.first()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createBackup(uri: Uri): Boolean {
        return try {
            val currentData = getCurrentData()
            if (currentData != null) {
                ExcelUtils.exportExcel(context, uri, currentData)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromBackup(uri: Uri): Boolean {
        return try {
            val restoredData = ExcelUtils.readExcel(context, uri)
            saveExcelData(restoredData)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getDataSummary(): DataSummary? {
        return getCurrentData()?.let { data ->
            val totalRows = data.rows.size
            val billedRows = data.rows.count { row ->
                (row.columns["Current"]?.toDoubleOrNull() ?: 0.0) > 0
            }
            val unbilledRows = totalRows - billedRows
            
            DataSummary(
                totalRecords = totalRows,
                billedRecords = billedRows,
                unbilledRecords = unbilledRows,
                sampleRecords = emptyList()
            )
        }
    }
}

data class DataSummary(
    val totalRecords: Int,
    val billedRecords: Int,
    val unbilledRecords: Int,
    val sampleRecords: List<SampleRecord>
)

data class SampleRecord(
    val name: String,
    val current: String,
    val previous: String
) 
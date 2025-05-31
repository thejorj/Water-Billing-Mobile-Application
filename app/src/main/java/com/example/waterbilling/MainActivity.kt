package com.example.waterbilling

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.waterbilling.data.ExcelSheet
import com.example.waterbilling.data.ExcelRow
import com.example.waterbilling.data.DataManager
import com.example.waterbilling.data.DataSummary
import com.example.waterbilling.ui.ExcelGrid
import com.example.waterbilling.ui.theme.DaloyTheme
import com.example.waterbilling.utils.ExcelUtils
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.documentfile.provider.DocumentFile
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import com.example.waterbilling.ui.BillingSummaryCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {
    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataManager = DataManager(this)
        enableEdgeToEdge()
        setContent {
            DaloyTheme {
                MainScreen(dataManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(dataManager: DataManager) {
    var excelData by remember { mutableStateOf<ExcelSheet?>(null) }
    var dataSummary by remember { mutableStateOf<DataSummary?>(null) }
    var currentFilename by remember { mutableStateOf<String?>(null) }
    var showImportConfirmation by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var isAutosaveEnabled by remember { mutableStateOf(true) }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Collect current filename
    LaunchedEffect(Unit) {
        dataManager.currentFilename.collect { filename ->
            currentFilename = filename
        }
    }

    // Helper function to handle import
    fun handleImport(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            
            val filename = DocumentFile.fromSingleUri(context, uri)?.name
            currentFilename = filename
            
            isLoading = true
            errorMessage = null
            try {
                val importedData = ExcelUtils.readExcel(context, uri)
                excelData = importedData
                scope.launch {
                    try {
                        dataManager.setCurrentFilename(filename)
                        dataManager.saveExcelData(importedData)
                        Toast.makeText(context, "Excel file imported successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to save imported data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ExcelImport", "Error importing excel file", e)
                errorMessage = "Error importing file: ${e.message}"
                Toast.makeText(context, "Error importing file", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        } catch (e: Exception) {
            Log.e("Permission", "Error taking persistable permission", e)
        }
    }

    // Helper function to handle restore
    fun handleRestore(uri: Uri) {
        scope.launch {
            isLoading = true
            try {
                val success = dataManager.restoreFromBackup(uri)
                if (success) {
                    Toast.makeText(context, "Data restored successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to restore data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error restoring data: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    // Create backup launcher
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let { safeUri ->
            scope.launch {
                isLoading = true
                try {
                    val backupSuccess = dataManager.createBackup(safeUri)
                    if (backupSuccess) {
                        Toast.makeText(context, "Backup created successfully", Toast.LENGTH_SHORT).show()
                        // Process pending import or restore after backup
                        pendingImportUri?.let { importUri ->
                            handleImport(importUri)
                            pendingImportUri = null
                        }
                        pendingRestoreUri?.let { restoreUri ->
                            handleRestore(restoreUri)
                            pendingRestoreUri = null
                        }
                    } else {
                        Toast.makeText(context, "Failed to create backup", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                    showImportConfirmation = false
                    showRestoreConfirmation = false
                }
            }
        }
    }

    // Create document launcher for exports
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let { safeUri ->
            scope.launch {
                try {
                    val dataToExport = dataManager.getCurrentData()
                    if (dataToExport != null) {
                        ExcelUtils.exportExcel(context, safeUri, dataToExport)
                        Toast.makeText(context, "File exported successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ExcelExport", "Error exporting excel file", e)
                    Toast.makeText(context, "Error exporting file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Document picker launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { safeUri ->
            if (excelData != null) {
                pendingImportUri = safeUri
                showImportConfirmation = true
            } else {
                handleImport(safeUri)
            }
        }
    }

    // Restore launcher
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { safeUri ->
            if (excelData != null) {
                pendingRestoreUri = safeUri
                showRestoreConfirmation = true
            } else {
                handleRestore(safeUri)
            }
        }
    }

    // Load saved data when the screen starts
    LaunchedEffect(Unit) {
        dataManager.excelData.collect { savedData ->
            excelData = savedData
        }
    }

    // Load data summary when showing confirmation dialog
    LaunchedEffect(showClearConfirmation) {
        if (showClearConfirmation) {
            dataSummary = dataManager.getDataSummary()
        }
    }

    // Load autosave state
    LaunchedEffect(Unit) {
        dataManager.isAutosaveEnabled.collect { enabled ->
            isAutosaveEnabled = enabled
        }
    }

    // Function to handle row updates with autosave
    fun updateRow(updatedRow: ExcelRow) {
        excelData?.let { currentData ->
            val updatedRows = currentData.rows.map { row ->
                if (row.id == updatedRow.id) updatedRow else row
            }
            val updatedSheet = ExcelSheet(currentData.headers, updatedRows)
            excelData = updatedSheet
            // Save changes to DataStore
            scope.launch {
                try {
                    // Check if this update represents a successful bill by comparing Current column values
                    val oldRow = currentData.rows.find { it.id == updatedRow.id }
                    val oldCurrentValue = oldRow?.columns?.get("Current")?.toDoubleOrNull() ?: 0.0
                    val newCurrentValue = updatedRow.columns["Current"]?.toDoubleOrNull() ?: 0.0
                    val isSuccessfulBill = oldCurrentValue == 0.0 && newCurrentValue > 0.0

                    dataManager.saveExcelData(updatedSheet, forceWrite = !isAutosaveEnabled, isSuccessfulBill = isSuccessfulBill)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save changes: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { !it }) {
            showPermissionDialog = true
        }
    }

    // Import Confirmation Dialog
    if (showImportConfirmation) {
        AlertDialog(
            onDismissRequest = { 
                showImportConfirmation = false 
                pendingImportUri = null
            },
            title = { Text("Import New Data?") },
            text = { 
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "You have existing data loaded. Importing new data will replace the current data:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    dataSummary?.let { summary ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Current Records: ${summary.totalRecords}")
                                Text("Billed: ${summary.billedRecords}")
                                Text("Unbilled: ${summary.unbilledRecords}")
                            }
                        }
                    }
                    
                    Text(
                        "This action cannot be undone!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Backup and Import button
                    Button(
                        onClick = {
                            showImportConfirmation = false
                            createBackupLauncher.launch("water_billing_backup_${System.currentTimeMillis()}.xlsx")
                            // After backup is created, proceed with import
                            pendingImportUri?.let { uri ->
                                handleImport(uri)
                            }
                            pendingImportUri = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Backup & Import")
                    }
                    // Import without backup button
                    Button(
                        onClick = {
                            showImportConfirmation = false
                            pendingImportUri?.let { uri ->
                                handleImport(uri)
                            }
                            pendingImportUri = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Import Without Backup")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showImportConfirmation = false
                        pendingImportUri = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { 
                showRestoreConfirmation = false
                pendingRestoreUri = null
            },
            title = { Text("Restore From Backup?") },
            text = { 
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "You have existing data loaded. Restoring from backup will replace the current data:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    dataSummary?.let { summary ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Current Records: ${summary.totalRecords}")
                                Text("Billed: ${summary.billedRecords}")
                                Text("Unbilled: ${summary.unbilledRecords}")
                            }
                        }
                    }
                    
                    Text(
                        "This action cannot be undone!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Backup and Restore button
                    Button(
                        onClick = {
                            showRestoreConfirmation = false
                            createBackupLauncher.launch("water_billing_backup_${System.currentTimeMillis()}.xlsx")
                            // After backup is created, proceed with restore
                            pendingRestoreUri?.let { uri ->
                                handleRestore(uri)
                            }
                            pendingRestoreUri = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Backup & Restore")
                    }
                    // Restore without backup button
                    Button(
                        onClick = {
                            showRestoreConfirmation = false
                            pendingRestoreUri?.let { uri ->
                                handleRestore(uri)
                            }
                            pendingRestoreUri = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Restore Without Backup")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showRestoreConfirmation = false 
                        pendingRestoreUri = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { Text("Storage permission is required to access Excel files. Please grant the permission in Settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Data Confirmation Dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear All Data?") },
            text = { 
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "This will permanently delete all data:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    dataSummary?.let { summary ->
                        // Data summary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Total Records: ${summary.totalRecords}")
                                Text("Billed Records: ${summary.billedRecords}")
                                Text("Unbilled Records: ${summary.unbilledRecords}")
                            }
                        }
                    } ?: Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Total Records: 0")
                            Text("Billed Records: 0")
                            Text("Unbilled Records: 0")
                        }
                    }
                    
                    Text(
                        "This action cannot be undone!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Backup and Clear button
                    Button(
                        onClick = {
                            createBackupLauncher.launch("water_billing_backup_${System.currentTimeMillis()}.xlsx")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Backup & Clear")
                    }
                    // Clear without backup button
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    dataManager.clearData()
                                    Toast.makeText(context, "Data cleared successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error clearing data: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    showClearConfirmation = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Clear Without Backup")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DALOY",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    // Autosave toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "Autosave",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp),
                            color = if (excelData != null) MaterialTheme.colorScheme.onSurface 
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                        Switch(
                            checked = isAutosaveEnabled && excelData != null,
                            onCheckedChange = { enabled ->
                                if (excelData != null) {
                                    scope.launch {
                                        try {
                                            dataManager.setAutosaveEnabled(enabled)
                                            isAutosaveEnabled = enabled
                                            Toast.makeText(
                                                context,
                                                if (enabled) "Autosave enabled" else "Autosave disabled",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Failed to change autosave setting",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            enabled = excelData != null
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // Import Excel option
                            DropdownMenuItem(
                                text = { Text("Import Excel") },
                                onClick = {
                                    showMenu = false
                                    val permissions = mutableListOf<String>()
                                    
                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                    
                                    if (permissions.isNotEmpty() && permissions.any { 
                                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED 
                                    }) {
                                        permissionLauncher.launch(permissions.toTypedArray())
                                    } else {
                                        openDocumentLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                    }
                                }
                            )
                            
                            // Export Excel option
                            DropdownMenuItem(
                                text = { Text("Export Excel") },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        val dataToExport = dataManager.getCurrentData()
                                        if (dataToExport != null) {
                                            createDocumentLauncher.launch("water_billing_data.xlsx")
                                        } else {
                                            Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            
                            // Restore from backup option
                            DropdownMenuItem(
                                text = { Text("Restore Backup") },
                                onClick = {
                                    showMenu = false
                                    restoreLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                }
                            )
                            
                            // Clear Data option
                            DropdownMenuItem(
                                text = { Text("Clear Data", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showClearConfirmation = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                errorMessage?.let { error ->
    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                excelData?.let { data ->
                    ExcelGrid(
                        excelSheet = data,
                        onRowUpdate = { updatedRow -> updateRow(updatedRow) },
                        filename = currentFilename,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Import an Excel file to view data",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
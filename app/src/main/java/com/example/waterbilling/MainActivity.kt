package com.example.waterbilling

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import android.content.Context
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.*
import androidx.compose.ui.platform.testTag
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.*
import androidx.compose.foundation.text.KeyboardOptions
import com.example.waterbilling.ui.BillingSummaryCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.waterbilling.data.DevSettings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.*
import android.os.Environment
import java.io.File
import androidx.core.content.FileProvider

@Composable
fun ExpirationTimer(
    expirationTime: Long,
    modifier: Modifier = Modifier,
    onExpired: () -> Unit
) {
    var remainingTime by remember { mutableStateOf(0L) }
    var isExpired by remember { mutableStateOf(false) }
    
    // Update timer every second
    LaunchedEffect(expirationTime) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            val timeLeft = if (expirationTime > 0) expirationTime - currentTime else 0
            
            if (timeLeft <= 0 && !isExpired && expirationTime > 0) {
                isExpired = true
                onExpired()
            }
            
            remainingTime = timeLeft
            delay(1000) // Update every second
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val isLowTime = remainingTime in 1..24 * 60 * 60 * 1000 // Less than 24 hours but greater than 0
        Icon(
            if (isLowTime) Icons.Default.Warning else Icons.Default.Info,
            contentDescription = if (isLowTime) "Warning: Time running low" else "Time remaining",
            tint = when {
                remainingTime <= 0 -> MaterialTheme.colorScheme.error
                isLowTime -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
        )
        
        val text = when {
            expirationTime <= 0 -> "No duration set"
            remainingTime <= 0 -> "Expired"
            else -> {
                val days = remainingTime / (24 * 60 * 60 * 1000)
                val hours = (remainingTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
                val minutes = (remainingTime % (60 * 60 * 1000)) / (60 * 1000)
                val seconds = (remainingTime % (60 * 1000)) / 1000
                String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds)
            }
        }
        
        Text(
            text = text,
            color = when {
                remainingTime <= 0 -> MaterialTheme.colorScheme.error
                isLowTime -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
fun InstructionsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = "Instructions")
                Text("How to Use DALOY")
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Getting Started",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "1. Import your Excel file using the Import Excel option in the menu\n" +
                        "2. The app will display your data in a grid format\n" +
                        "3. You can start entering readings in the 'Current' column"
                    )
                }
                
                item {
                    Text(
                        "Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "• Autosave: Toggle to automatically save changes\n" +
                        "• Export Excel: Save your data to an Excel file\n" +
                        "• Restore Backup: Recover data from a backup file\n" +
                        "• Clear Data: Remove all data (create a backup first!)"
                    )
                }
                
                item {
                    Text(
                        "Entering Readings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "1. Locate the customer's row\n" +
                        "2. Enter the current meter reading\n" +
                        "3. The consumption and amount will calculate automatically\n" +
                        "4. Changes are saved based on your autosave setting"
                    )
                }
                
                item {
                    Text(
                        "Tips",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "• Keep autosave ON for automatic data backup\n" +
                        "• Create regular backups of your data\n" +
                        "• Check the timer to monitor app duration\n" +
                        "• Contact support if you need duration extension"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

class MainActivity : ComponentActivity() {
    private lateinit var dataManager: DataManager
    private lateinit var devSettings:   DevSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        dataManager = DataManager(this)
        devSettings = DevSettings(this)
        
        setContent {
            DaloyTheme {
                MainScreen(dataManager, devSettings)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    dataManager: DataManager,
    devSettings: DevSettings
) {
    val context = LocalContext.current
    var showExpirationDialog by remember { mutableStateOf(false) }
    var showDevOptions by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    val isDevMode by devSettings.isDevModeEnabled.collectAsState(initial = false)
    var titleClickCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    var currentExpirationTime by remember { 
        mutableStateOf(
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getLong("app_expiration_time", 0)
        )
    }

    // Update expiration time when SharedPreferences changes
    LaunchedEffect(Unit) {
        while(true) {
            currentExpirationTime = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getLong("app_expiration_time", 0)
            delay(1000) // Check every second
        }
    }

    // Check if app has no duration set or is expired
    val isDurationInvalid = currentExpirationTime <= 0 || System.currentTimeMillis() > currentExpirationTime

    // Force show expiration dialog if duration is invalid
    LaunchedEffect(isDurationInvalid) {
        if (isDurationInvalid) {
            showExpirationDialog = true
        }
    }

    // Prevent closing the expiration dialog if duration is invalid
    if (showExpirationDialog && isDurationInvalid) {
        var hasData by remember { mutableStateOf(false) }
        
        // Check if there's data to export
        LaunchedEffect(Unit) {
            val currentData = dataManager.getCurrentData()
            hasData = currentData != null && currentData.rows.isNotEmpty()
        }

        AlertDialog(
            onDismissRequest = { /* Do nothing to prevent dismissal */ },
            title = { 
                Text(
                    "DALOY",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        showPasswordDialog = true
                    }
                )
            },
            text = { 
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        if (currentExpirationTime <= 0) 
                            "Please set the application duration to continue." 
                        else "Contact the Developer to continue using"
                    )
                    
                    if ( hasData) {
                        Text(
                            "You can export your data before exiting:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasData) {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        // Create emergency backup in Downloads/Backup
                                        val backupUri = createBackupDirectory(context)
                                        if (backupUri != null) {
                                            val backupSuccess = dataManager.createBackup(backupUri)
                                            if (backupSuccess) {
                                                Toast.makeText(
                                                    context, 
                                                    "Emergency backup created in Downloads/Backup folder", 
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Failed to create emergency backup: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Export Data")
                        }
                    }
                    
                    Button(
                        onClick = {
                            // Exit the app
                            android.os.Process.killProcess(android.os.Process.myPid())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Exit App")
                    }
                }
            }
        )
    }

    // Password Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPasswordDialog = false 
                password = ""
                passwordError = false
            },
            title = { Text("Enter Developer Password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            passwordError = false
                        },
                        label = { Text("Password") },
                        isError = passwordError,
                        supportingText = if (passwordError) {
                            { Text("Incorrect password") }
                        } else null,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Check password
                        if (password == "daloy2025") {
                            showPasswordDialog = false
                            showDevOptions = true
                            password = ""
                            passwordError = false
                        } else {
                            passwordError = true
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPasswordDialog = false
                        password = ""
                        passwordError = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    var excelData by remember { mutableStateOf<ExcelSheet?>(null) }
    var dataSummary by remember { mutableStateOf<DataSummary?>(null) }
    var currentFilename by remember { mutableStateOf<String?>(null) }
    var showImportConfirmation by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var isAutosaveEnabled by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }

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
                    val backupUri = createBackupDirectory(context)
                    if (backupUri != null) {
                        val backupSuccess = dataManager.createBackup(backupUri)
                        if (backupSuccess) {
                            Toast.makeText(context, "Backup created successfully in Downloads/Backup folder", Toast.LENGTH_SHORT).show()
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

    // Developer Options Dialog
    if (showDevOptions) {
        var selectedDurationType by remember { mutableStateOf("Months") }
        var durationValue by remember { mutableStateOf("1") }
        var showSuccessMessage by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showDevOptions = false },
            title = { Text("Set App Duration") },
            text = { 
                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Duration Value Input
                    OutlinedTextField(
                        value = durationValue,
                        onValueChange = { 
                            // Only allow numbers
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                durationValue = it
                            }
                        },
                        label = { Text("Duration") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Duration Type Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Minutes", "Hours", "Days", "Months").forEach { type ->
                            FilterChip(
                                selected = selectedDurationType == type,
                                onClick = { selectedDurationType = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    // Apply Duration Button
                    Button(
                        onClick = {
                            scope.launch {
                                val durationInMillis = when (selectedDurationType) {
                                    "Minutes" -> durationValue.toLongOrNull()?.let { it * 60 * 1000 }
                                    "Hours" -> durationValue.toLongOrNull()?.let { it * 60 * 60 * 1000 }
                                    "Days" -> durationValue.toLongOrNull()?.let { it * 24 * 60 * 60 * 1000 }
                                    "Months" -> durationValue.toLongOrNull()?.let { it * 30L * 24 * 60 * 60 * 1000 }
                                    else -> null
                                }

                                if (durationInMillis != null) {
                                    val currentTime = System.currentTimeMillis()
                                    val expirationTime = currentTime + durationInMillis
                                    
                                    // Save the expiration time to SharedPreferences
                                    context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                                        .edit()
                                        .putLong("app_expiration_time", expirationTime)
                                        .apply()
                                    
                                    showSuccessMessage = "App duration set to $durationValue ${selectedDurationType.lowercase()}"
                                    
                                    // Dismiss both dialogs after a short delay
                                    delay(1000)
                                    showDevOptions = false
                                    showExpirationDialog = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = durationValue.isNotEmpty() && durationValue.toIntOrNull() != null && durationValue.toInt() > 0
                    ) {
                        Text("Apply Duration")
                    }

                    // Success Message
                    AnimatedVisibility(
                        visible = showSuccessMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = showSuccessMessage ?: "",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Divider()

                    // Current Duration Info
                    val expirationTime = remember {
                        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                            .getLong("app_expiration_time", 0)
                    }
                    
                    if (expirationTime > 0) {
                        Text(
                            "Current Duration Info:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        val currentTime = System.currentTimeMillis()
                        val remainingTime = expirationTime - currentTime
                        val remainingDays = remainingTime / (24 * 60 * 60 * 1000)
                        val remainingHours = (remainingTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
                        
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        Text("Expiration: ${dateFormat.format(Date(expirationTime))}")
                        Text(
                            "Remaining Time: $remainingDays days, $remainingHours hours",
                            color = if (remainingTime > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDevOptions = false }) {
                    Text("Close")
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            showPasswordDialog = true
                        }
                    )
                },
                actions = {
                    // Timer display
                    ExpirationTimer(
                        expirationTime = currentExpirationTime,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        onExpired = {
                            showExpirationDialog = true
                        }
                    )
                    
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
                            enabled = excelData != null,
                            modifier = Modifier.testTag("autosave_switch")
                        )
                    }

                    Box {
                        var showInstructionsDialog by remember { mutableStateOf(false) }
                        
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
                            // Instructions option
                            DropdownMenuItem(
                                text = { Text("Instructions") },
                                leadingIcon = { 
                                    Icon(Icons.Default.Info, contentDescription = "Instructions")
                                },
                                onClick = {
                                    showMenu = false
                                    showInstructionsDialog = true
                                }
                            )

                            Divider()
                            
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
                            
                            // Clear Data option with backup
                            DropdownMenuItem(
                                text = { Text("Clear Data", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showClearConfirmation = true
                                }
                            )
                        }

                        if (showInstructionsDialog) {
                            InstructionsDialog(onDismiss = { showInstructionsDialog = false })
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

// Add this function after the imports and before the first composable
private fun createBackupDirectory(context: Context): Uri? {
    try {
        // Get the Downloads directory
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val backupFolder = File(downloadsFolder, "Backup")

        // Create the Backup directory if it doesn't exist
        if (!backupFolder.exists()) {
            val success = backupFolder.mkdirs()
            if (!success) {
                Toast.makeText(context, "Failed to create backup directory", Toast.LENGTH_SHORT).show()
                return null
            }
        }

        // Create a unique backup file name with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupFile = File(backupFolder, "water_billing_backup_${timestamp}.xlsx")
        
        // Use FileProvider to get content URI
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            backupFile
        )
    } catch (e: Exception) {
        Toast.makeText(context, "Error creating backup directory: ${e.message}", Toast.LENGTH_SHORT).show()
        return null
    }
}
package com.example.waterbilling.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.waterbilling.data.ExcelRow
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@Composable
fun ExcelDetailModal(
    row: ExcelRow,
    onDismiss: () -> Unit,
    onUpdate: (ExcelRow) -> Unit
) {
    var currentValue by remember { mutableStateOf(row.columns["Current"] ?: "") }
    val previousValue = row.columns["Previous"] ?: "0"
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var forEvaluation by remember { mutableStateOf(row.columns["ForEvaluation"]?.equals("true", ignoreCase = true) ?: false) }
    val context = LocalContext.current

    // Function to validate values
    fun validateValues(): Boolean {
        if (currentValue.isBlank()) return true
        
        try {
            val current = currentValue.toDoubleOrNull() ?: 0.0
            val previous = previousValue.toDoubleOrNull() ?: 0.0

            if (current < previous) {
                showError = true
                errorMessage = "Current value cannot be less than Previous value"
                Toast.makeText(context, "Current value must be greater than Previous value", Toast.LENGTH_SHORT).show()
                return false
            }

            showError = false
            errorMessage = ""
            return true

        } catch (e: Exception) {
            showError = true
            errorMessage = "Invalid number format"
            Toast.makeText(context, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    // Function to calculate consumed value
    fun calculateConsumed(): String {
        if (currentValue.isBlank()) return ""
        return try {
            val current = currentValue.toDoubleOrNull() ?: 0.0
            val previous = previousValue.toDoubleOrNull() ?: 0.0
            val consumed = current - previous
            // Return empty if consumed is negative, otherwise return the consumed value
            if (consumed < 0) "" else consumed.toString()
        } catch (e: Exception) {
            ""
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Row Details",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Error message
                if (showError) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // For Evaluation Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "For Evaluation",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "For Evaluation",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF2196F3)
                        )
                    }
                    Switch(
                        checked = forEvaluation,
                        onCheckedChange = { forEvaluation = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF2196F3),
                            checkedTrackColor = Color(0xFF2196F3).copy(alpha = 0.5f)
                        )
                    )
                }

                Divider(modifier = Modifier.padding(bottom = 16.dp))

                row.columns.forEach { (header, value) ->
                    if (header != "ForEvaluation") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            when (header) {
                                "Current" -> {
                                    OutlinedTextField(
                                        value = currentValue,
                                        onValueChange = { newValue ->
                                            currentValue = newValue
                                            // Clear error when typing
                                            showError = false
                                            errorMessage = ""
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        isError = showError,
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                        )
                                    )
                                }
                                "Previous" -> {
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                "Consumed" -> {
                                    Text(
                                        text = calculateConsumed(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                else -> {
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Divider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Changes cancelled", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (validateValues()) {
                                val updatedColumns = row.columns.toMutableMap()
                                updatedColumns["Current"] = currentValue
                                updatedColumns["Consumed"] = calculateConsumed()
                                updatedColumns["ForEvaluation"] = forEvaluation.toString()
                                onUpdate(ExcelRow(row.id, updatedColumns))
                                Toast.makeText(context, "Changes saved successfully", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
} 
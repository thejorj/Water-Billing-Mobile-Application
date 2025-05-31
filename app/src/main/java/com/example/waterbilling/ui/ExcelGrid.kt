package com.example.waterbilling.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.waterbilling.data.ExcelSheet
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import com.example.waterbilling.data.ExcelRow
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.Menu

enum class BillingFilter {
    ALL, BILLED, UNBILLED, FOR_EVALUATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelGrid(
    excelSheet: ExcelSheet,
    onRowUpdate: (ExcelRow) -> Unit,
    filename: String? = null,
    modifier: Modifier = Modifier
) {
    var selectedRow by remember { mutableStateOf<ExcelRow?>(null) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var currentFilter by remember { mutableStateOf(BillingFilter.ALL) }
    val horizontalScrollState = rememberScrollState()

    // Filter rows based on search query and billing status
    val filteredRows = remember(searchQuery.text, excelSheet.rows, currentFilter) {
        excelSheet.rows
            .filter { row ->
                val currentValue = row.columns["Current"]?.takeIf { it.isNotBlank() }
                val forEvaluation = row.columns["ForEvaluation"]?.equals("true", ignoreCase = true) ?: false
                
                when (currentFilter) {
                    BillingFilter.BILLED -> !currentValue.isNullOrBlank() && currentValue != "0"
                    BillingFilter.UNBILLED -> currentValue.isNullOrBlank() || currentValue == "0"
                    BillingFilter.FOR_EVALUATION -> forEvaluation
                    BillingFilter.ALL -> true
                }
            }
            .filter { row ->
                if (searchQuery.text.isEmpty()) true
                else row.columns.values.any { value ->
                    value.contains(searchQuery.text, ignoreCase = true)
                }
            }
    }

    // Calculate counts for summary
    val counts = remember(excelSheet.rows) {
        excelSheet.rows.fold(Triple(0, 0, 0)) { acc, row ->
            val currentValue = row.columns["Current"]?.takeIf { it.isNotBlank() }
            val forEvaluation = row.columns["ForEvaluation"]?.equals("true", ignoreCase = true) ?: false
            val isBilled = !currentValue.isNullOrBlank() && currentValue != "0"
            
            Triple(
                if (isBilled) acc.first + 1 else acc.first,
                if (!isBilled) acc.second + 1 else acc.second,
                if (forEvaluation) acc.third + 1 else acc.third
            )
        }
    }

    Column(modifier = modifier) {
        // Summary Card
        BillingSummaryCard(
            totalRows = excelSheet.rows.size,
            billedRows = counts.first,
            unbilledRows = counts.second,
            evaluationRows = counts.third,
            filename = filename,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Search and Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.text.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = TextFieldValue("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Filter Button
            var showFilterMenu by remember { mutableStateOf(false) }
            Box {
                FilledTonalButton(
                    onClick = { showFilterMenu = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = when (currentFilter) {
                            BillingFilter.ALL -> MaterialTheme.colorScheme.secondaryContainer
                            BillingFilter.BILLED -> Color(0xFF90EE90).copy(alpha = 0.2f)
                            BillingFilter.UNBILLED -> MaterialTheme.colorScheme.errorContainer
                            BillingFilter.FOR_EVALUATION -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Filter"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        when (currentFilter) {
                            BillingFilter.ALL -> "All"
                            BillingFilter.BILLED -> "Billed"
                            BillingFilter.UNBILLED -> "Unbilled"
                            BillingFilter.FOR_EVALUATION -> "For Evaluation"
                        }
                    )
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Records") },
                        onClick = {
                            currentFilter = BillingFilter.ALL
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Billed") },
                        onClick = {
                            currentFilter = BillingFilter.BILLED
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Unbilled") },
                        onClick = {
                            currentFilter = BillingFilter.UNBILLED
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("For Evaluation") },
                        onClick = {
                            currentFilter = BillingFilter.FOR_EVALUATION
                            showFilterMenu = false
                        }
                    )
                }
            }
        }

        // Grid Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(horizontalScrollState)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                excelSheet.headers.forEach { header ->
                    Text(
                        text = header,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .width(120.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Data rows
        if (filteredRows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        searchQuery.text.isNotEmpty() -> "No results found for '${searchQuery.text}'"
                        currentFilter != BillingFilter.ALL -> "No ${currentFilter.name.lowercase().replace('_', ' ')} records found"
                        else -> "No records found"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                items(filteredRows) { row ->
                    val currentValue = row.columns["Current"]?.takeIf { it.isNotBlank() }
                    val hasCurrentValue = !currentValue.isNullOrBlank() && currentValue != "0"
                    val forEvaluation = row.columns["ForEvaluation"]?.equals("true", ignoreCase = true) ?: false
                    
                    val rowBackground = when {
                        selectedRow?.id == row.id -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        forEvaluation -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        hasCurrentValue -> Color(0xFF90EE90).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRow = row },
                        color = rowBackground
                    ) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(horizontalScrollState)
                                .padding(vertical = 8.dp)
                        ) {
                            excelSheet.headers.forEach { header ->
                                val value = row.columns[header] ?: ""
                                val textColor = when {
                                    header == "Current" && hasCurrentValue -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Text(
                                    text = value,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .width(120.dp),
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Divider()
                }
            }
        }
    }

    // Show detail modal when a row is selected
    selectedRow?.let { row ->
        ExcelDetailModal(
            row = row,
            onDismiss = { selectedRow = null },
            onUpdate = { updatedRow ->
                onRowUpdate(updatedRow)
                selectedRow = null
            }
        )
    }
} 
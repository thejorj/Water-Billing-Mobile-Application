package com.example.waterbilling.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.example.waterbilling.data.ExcelRow
import com.example.waterbilling.data.ExcelSheet
import java.io.InputStream
import java.io.OutputStream
import org.apache.poi.poifs.filesystem.OfficeXmlFileException

class ExcelUtils {
    companion object {
        private val REQUIRED_COLUMNS = listOf("Current", "Consumed")
        private const val TAG = "ExcelUtils"

        fun readExcel(context: Context, uri: Uri): ExcelSheet {
            var inputStream: InputStream? = null
            var workbook: Workbook? = null
            
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Could not open file")
                
                workbook = try {
                    WorkbookFactory.create(inputStream)
                } catch (e: OfficeXmlFileException) {
                    Log.e(TAG, "Error creating workbook", e)
                    throw IllegalArgumentException("Invalid Excel file format. Please use .xlsx format.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating workbook", e)
                    throw IllegalArgumentException("Could not read Excel file: ${e.message}")
                }

                val sheet = workbook.getSheetAt(0)
                
                // Read headers
                val headerRow = sheet.getRow(0) ?: sheet.createRow(0)
                var headers = if (headerRow.lastCellNum > 0) {
                    (0 until headerRow.lastCellNum).map { 
                        headerRow.getCell(it)?.stringCellValue?.trim() ?: ""
                    }.filter { it.isNotEmpty() }
                } else {
                    emptyList()
                }

                if (headers.isEmpty()) {
                    throw IllegalArgumentException("Excel file has no headers")
                }

                // Check and add required columns if they don't exist
                val updatedHeaders = headers.toMutableList()
                var columnsAdded = false
                
                REQUIRED_COLUMNS.forEach { requiredColumn ->
                    if (!headers.contains(requiredColumn)) {
                        updatedHeaders.add(requiredColumn)
                        // Add the header to the Excel file
                        headerRow.createCell(updatedHeaders.size - 1).setCellValue(requiredColumn)
                        columnsAdded = true
                    }
                }

                headers = updatedHeaders.toList()
                
                // Read data rows
                val rows = (1..sheet.lastRowNum).mapNotNull { rowNum ->
                    try {
                        val row = sheet.getRow(rowNum) ?: sheet.createRow(rowNum)
                        val rowData = headers.mapIndexed { index, header ->
                            val cell = row.getCell(index) ?: if (REQUIRED_COLUMNS.contains(header)) {
                                // Initialize new required columns with "0"
                                row.createCell(index).apply { setCellValue("0") }
                            } else {
                                row.createCell(index)
                            }
                            header to (cell.toString().trim())
                        }.toMap()
                        
                        // Skip completely empty rows
                        if (rowData.values.all { it.isEmpty() }) {
                            null
                        } else {
                            ExcelRow(
                                id = rowNum.toString(),
                                columns = rowData
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading row $rowNum", e)
                        null
                    }
                }

                if (rows.isEmpty()) {
                    throw IllegalArgumentException("Excel file has no data rows")
                }

                // If we added columns, save the changes back to the file
                if (columnsAdded) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        workbook.write(outputStream)
                    } ?: throw IllegalStateException("Could not save changes to file")
                }
                
                return ExcelSheet(headers, rows)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading Excel file", e)
                throw e
            } finally {
                try {
                    inputStream?.close()
                    workbook?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing resources", e)
                }
            }
        }

        fun exportExcel(
            context: Context,
            uri: Uri,
            data: ExcelSheet
        ) {
            var workbook: Workbook? = null
            var outputStream: OutputStream? = null
            
            try {
                workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Sheet1")
                
                // Ensure required columns are included
                val updatedHeaders = data.headers.toMutableList()
                REQUIRED_COLUMNS.forEach { requiredColumn ->
                    if (!updatedHeaders.contains(requiredColumn)) {
                        updatedHeaders.add(requiredColumn)
                    }
                }
                
                // Write headers
                val headerRow = sheet.createRow(0)
                updatedHeaders.forEachIndexed { index, header ->
                    headerRow.createCell(index).setCellValue(header)
                }
                
                // Write data rows
                data.rows.forEachIndexed { rowIndex, excelRow ->
                    val row = sheet.createRow(rowIndex + 1)
                    updatedHeaders.forEachIndexed { colIndex, header ->
                        val value = excelRow.columns[header] ?: if (REQUIRED_COLUMNS.contains(header)) "0" else ""
                        row.createCell(colIndex).setCellValue(value)
                    }
                }
                
                // Save workbook
                outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Could not open file for writing")
                workbook.write(outputStream)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting Excel file", e)
                throw e
            } finally {
                try {
                    outputStream?.close()
                    workbook?.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing resources", e)
                }
            }
        }
    }
} 
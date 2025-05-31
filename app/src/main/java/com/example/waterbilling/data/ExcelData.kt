package com.example.waterbilling.data

import kotlinx.serialization.Serializable

@Serializable
data class ExcelRow(
    val id: String,
    val columns: Map<String, String>
)

@Serializable
data class ExcelSheet(
    val headers: List<String>,
    val rows: List<ExcelRow>
) 
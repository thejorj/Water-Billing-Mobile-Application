package com.example.waterbilling

import android.content.Context
import com.example.waterbilling.data.DataManager
import com.example.waterbilling.data.ExcelRow
import com.example.waterbilling.data.ExcelSheet
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class DataManagerTest {
    private lateinit var context: Context
    private lateinit var dataManager: DataManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        dataManager = DataManager(context)
        MockKAnnotations.init(this)
    }

    @Test
    fun `test autosave enabled by default`() = runBlocking {
        val isEnabled = dataManager.isAutosaveEnabled.first()
        assertTrue("Autosave should be enabled by default", isEnabled)
    }

    @Test
    fun `test excel data operations`() = runBlocking {
        // Create test data
        val headers = listOf("Name", "Amount", "Date")
        val row1 = ExcelRow("1", mapOf(
            "Name" to "John Doe",
            "Amount" to "100.00",
            "Date" to "2024-01-01"
        ))
        val row2 = ExcelRow("2", mapOf(
            "Name" to "Jane Smith",
            "Amount" to "150.00",
            "Date" to "2024-01-02"
        ))
        val testData = ExcelSheet(headers, listOf(row1, row2))

        // Test saving data
        dataManager.saveExcelData(testData)
        
        // Test retrieving data
        val savedData = dataManager.getCurrentData()
        assertNotNull("Saved data should not be null", savedData)
        assertEquals("Should have 2 rows", 2, savedData?.rows?.size)
        assertEquals("Headers should match", headers, savedData?.headers)
    }

    @Test
    fun `test data summary calculation`() = runBlocking {
        // Create test data with some billed and unbilled records
        val headers = listOf("Name", "Current")
        val billedRow = ExcelRow("1", mapOf(
            "Name" to "John Doe",
            "Current" to "100.00"
        ))
        val unbilledRow = ExcelRow("2", mapOf(
            "Name" to "Jane Smith",
            "Current" to "0.00"
        ))
        val testData = ExcelSheet(headers, listOf(billedRow, unbilledRow))

        // Save the test data
        dataManager.saveExcelData(testData)

        // Get data summary
        val summary = dataManager.getDataSummary()
        assertNotNull("Summary should not be null", summary)
        assertEquals("Total records should be 2", 2, summary?.totalRecords)
        assertEquals("Billed records should be 1", 1, summary?.billedRecords)
        assertEquals("Unbilled records should be 1", 1, summary?.unbilledRecords)
    }
} 
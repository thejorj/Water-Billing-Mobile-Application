package com.example.waterbilling.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.unit.Dp

private data class SummaryItemData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val value: String,
    val color: Color
)

@Composable
fun BillingSummaryCard(
    totalRows: Int,
    billedRows: Int,
    unbilledRows: Int,
    evaluationRows: Int = 0,
    filename: String? = null,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    val horizontalPadding = if (screenWidth < 600.dp) 8.dp else 16.dp
    val verticalPadding = if (screenWidth < 600.dp) 12.dp else 16.dp
    val itemSpacing = if (screenWidth < 600.dp) 8.dp else 16.dp

    val summaryItems = listOf(
        SummaryItemData(
            icon = Icons.Default.List,
            label = "Total Records",
            value = totalRows.toString(),
            color = MaterialTheme.colorScheme.tertiary
        ),
        SummaryItemData(
            icon = Icons.Default.CheckCircle,
            label = "Billed",
            value = billedRows.toString(),
            color = MaterialTheme.colorScheme.primary
        ),
        SummaryItemData(
            icon = Icons.Default.Warning,
            label = "Unbilled",
            value = unbilledRows.toString(),
            color = MaterialTheme.colorScheme.error
        ),
        SummaryItemData(
            icon = Icons.Default.Search,
            label = "For Evaluation",
            value = evaluationRows.toString(),
            color = Color(0xFF2196F3)
        )
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            // Filename section
            if (!filename.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = horizontalPadding, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "File",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = filename,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(itemSpacing))
            }

            // Statistics using LazyVerticalGrid
            LazyVerticalGrid(
                columns = if (screenWidth < 600.dp) GridCells.Fixed(2) else GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(summaryItems) { item ->
                    SummaryItem(
                        icon = item.icon,
                        label = item.label,
                        value = item.value,
                        color = item.color
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        color = color.copy(alpha = 0.1f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
} 
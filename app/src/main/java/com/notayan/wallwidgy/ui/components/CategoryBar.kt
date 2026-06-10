package com.notayan.wallwidgy.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CATEGORY_ICONS = mapOf(
    "nature" to Icons.Default.Landscape,
    "anime" to Icons.Default.Face,
    "art" to Icons.Default.Brush,
    "abstract" to Icons.Default.AutoAwesome,
    "cars" to Icons.Default.DirectionsCar,
    "architecture" to Icons.Default.Apartment,
    "minimal" to Icons.Default.Minimize,
    "tech" to Icons.Default.Memory,
    "amoled" to Icons.Default.InvertColors
)

@Composable
fun CategoryBar(
    categories: List<String>,
    selectedCategories: Set<String>,
    onCategoryToggled: (String) -> Unit,
    onClearAll: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.wrapContentHeight()
    ) {
        item {
            val isAllSelected = selectedCategories.isEmpty()
            Surface(
                onClick = onClearAll,
                shape = RoundedCornerShape(20.dp),
                color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                border = if (isAllSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    "All",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        items(categories) { categoryName ->
            val id = categoryName.lowercase()
            val isSelected = selectedCategories.any { it.removePrefix("#").trim().lowercase() == id }
            val icon = CATEGORY_ICONS[id] ?: Icons.AutoMirrored.Filled.Label
            
            Surface(
                onClick = { onCategoryToggled(id) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        categoryName.split(" ").joinToString(" ") { word ->
                            word.replaceFirstChar { it.uppercase() }
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

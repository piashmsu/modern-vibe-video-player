package com.piash.modernvibe.videoplayer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.piash.modernvibe.videoplayer.VibeApplication
import com.piash.modernvibe.videoplayer.data.FeatureCatalog
import com.piash.modernvibe.videoplayer.data.FeatureItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesScreen() {
    val app = LocalContext.current.applicationContext as VibeApplication
    val settings = app.settings

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { visible = true }

    val filtered by remember(query, selectedCategory, refreshTick) {
        derivedStateOf {
            FeatureCatalog.all.filter { item ->
                (selectedCategory == null || item.category == selectedCategory) &&
                (query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeTopAppBar(
            title = {
                Column {
                    Text("Features", fontWeight = FontWeight.Bold)
                    Text(
                        "${FeatureCatalog.totalCount} tunable toggles",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search features") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AssistChip(
                        onClick = { selectedCategory = null },
                        label = { Text("All") },
                        colors = if (selectedCategory == null)
                            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else AssistChipDefaults.assistChipColors()
                    )
                }
                items(FeatureCatalog.categories) { cat ->
                    AssistChip(
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) },
                        colors = if (selectedCategory == cat)
                            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else AssistChipDefaults.assistChipColors()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 4 }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.key }) { item ->
                    FeatureRow(item, settings.getBoolean(item.key, item.defaultEnabled)) { newValue ->
                        settings.setBoolean(item.key, newValue)
                        refreshTick++
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    item: FeatureItem,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

package com.piash.modernvibe.videoplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as VibeApplication
    val settings = app.settings
    var refresh by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeTopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Bulk actions", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            settings.toggleAll(FeatureCatalog.all, true)
                            refresh++
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Enable All") }
                    Button(
                        onClick = {
                            settings.toggleAll(FeatureCatalog.all, false)
                            refresh++
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) { Text("Disable All") }
                }
            }
            item {
                Button(
                    onClick = {
                        FeatureCatalog.all.forEach {
                            settings.setBoolean(it.key, it.defaultEnabled)
                        }
                        refresh++
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reset to Defaults") }
            }
            item {
                Text("Stats", style = MaterialTheme.typography.titleMedium)
            }
            item {
                val enabledCount = FeatureCatalog.all.count {
                    settings.getBoolean(it.key, it.defaultEnabled)
                }
                Text("Total features: ${FeatureCatalog.totalCount}", style = MaterialTheme.typography.bodyMedium)
                Text("Enabled: $enabledCount", style = MaterialTheme.typography.bodyMedium)
                Text("Categories: ${FeatureCatalog.categories.size}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

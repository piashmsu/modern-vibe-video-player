package com.piash.modernvibe.videoplayer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.piash.modernvibe.videoplayer.VibeApplication
import com.piash.modernvibe.videoplayer.data.ApiKey
import com.piash.modernvibe.videoplayer.data.FeatureCatalog
import com.piash.modernvibe.videoplayer.data.SecureKeyStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as VibeApplication
    val settings = app.settings
    val keyStore = app.keys
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
            item { SectionHeader("AI API Keys") }
            item {
                Text(
                    "Apnar nijer API key bosao. Keys encrypted-storage e save hobe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ApiKey.values().forEach { apiKey ->
                item(key = "key_${apiKey.name}_$refresh") {
                    ApiKeyRow(apiKey, keyStore) { refresh++ }
                }
            }

            item { SectionHeader("Bulk feature actions") }
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
            item { SectionHeader("Stats") }
            item {
                val enabledCount = FeatureCatalog.all.count {
                    settings.getBoolean(it.key, it.defaultEnabled)
                }
                Text("Total features: ${FeatureCatalog.totalCount}", style = MaterialTheme.typography.bodyMedium)
                Text("Enabled: $enabledCount", style = MaterialTheme.typography.bodyMedium)
                Text("Categories: ${FeatureCatalog.categories.size}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyRow(apiKey: ApiKey, store: SecureKeyStore, onChange: () -> Unit) {
    val context = LocalContext.current
    var value by remember { mutableStateOf(store.get(apiKey)) }
    var visible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(apiKey.label, fontWeight = FontWeight.SemiBold)
                Text(apiKey.provider, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apiKey.helpUrl))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                runCatching { context.startActivity(intent) }
            }) {
                Icon(Icons.Filled.OpenInNew, contentDescription = "Open ${apiKey.provider} dashboard")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            label = { Text("Paste your ${apiKey.provider} key") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (visible) "Hide" else "Show"
                    )
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    store.set(apiKey, value.trim())
                    onChange()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Save") }
            TextButton(
                onClick = {
                    value = ""
                    store.set(apiKey, "")
                    onChange()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Clear") }
        }
        if (store.isSet(apiKey)) {
            Spacer(Modifier.height(4.dp))
            Text(
                "${apiKey.provider} key set ✓",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

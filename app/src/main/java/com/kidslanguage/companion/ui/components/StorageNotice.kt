package com.kidslanguage.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.ui.theme.Neutral900

@Composable
fun StorageNotice(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, modifier = Modifier.weight(1f), color = Neutral900)
        Button(onClick = onRetry, modifier = Modifier.heightIn(min = 64.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text("重试", maxLines = 1, softWrap = false)
        }
    }
}

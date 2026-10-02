package com.kidslanguage.companion.feature.parent

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.kidslanguage.companion.ui.theme.Neutral900

@Composable
fun ProgressResetControl(state: ProgressResetState, onReset: () -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("清空练习记录？") },
            text = { Text("将清空所有语言的练习记录和历史累计数，学习设置会保留。此操作无法撤销。") },
            confirmButton = {
                TextButton(enabled = state != ProgressResetState.Running, onClick = {
                    confirmReset = false
                    onReset()
                }) { Text("清空记录") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("取消") } },
        )
    }
    Column {
        when (state) {
            ProgressResetState.Complete -> Text("清空操作已完成", color = Neutral900)
            is ProgressResetState.Failed -> Text(state.message, color = Neutral900)
            else -> Unit
        }
        Button(
            onClick = { confirmReset = true },
            enabled = state != ProgressResetState.Running,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C), contentColor = Color.White),
        ) { Text(if (state == ProgressResetState.Running) "正在清空…" else "清空练习记录") }
    }
}

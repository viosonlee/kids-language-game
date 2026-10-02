package com.kidslanguage.companion.feature.parent

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.ui.components.StorageNotice
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral700
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProgressSummary(state: ProgressReadState, onRetry: () -> Unit) {
    Column {
        when (state) {
            ProgressReadState.Loading -> Text("正在读取练习记录…")
            is ProgressReadState.Failed -> StorageNotice(state.message, onRetry)
            is ProgressReadState.Ready -> {
                val totals = state.totals
                val today = state.today
                val legacyCompletedCount = totals.legacyCompleted
                val totalText = "累计完成题目数：${totals.completed.toLong() + legacyCompletedCount} 题"
                Text(totalText, style = AppTypography.bodyLarge)
                Text("今日练习：${today.practiced} 题 · 完成：${today.completed} 题", style = AppTypography.bodyLarge)
                Text("最近练习：" + if (totals.lastPracticedAt == 0L) "暂无记录" else
                    Instant.ofEpochMilli(totals.lastPracticedAt).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                Text("跟读正常通过：${totals.normalPasses} 次 · 辅助通过：${totals.assistedPasses} 次")
                Text("选图答对：${totals.chooseCorrect} 次 · 再试：${totals.chooseWrong} 次")
                LanguageCode.entries.forEach { language ->
                    val row = state.languages.firstOrNull { it.language == language.code }
                    val name = when (language) {
                        LanguageCode.JA_JP -> "日语"
                        LanguageCode.ZH_CN -> "中文"
                        LanguageCode.EN_US -> "英语"
                    }
                    Text("$name：练习 ${row?.practiced ?: 0} 题 · 完成 ${row?.completed ?: 0} 题")
                }
                if (legacyCompletedCount > 0) {
                    Text("含旧版本历史累计 $legacyCompletedCount 题；历史记录没有日期和语言明细。", color = Neutral700)
                }
                Text("同一题反复尝试计为一道练习；重新开始练习会再次计数。统计只供家长观察。", color = Neutral700)
            }
        }
    }
}

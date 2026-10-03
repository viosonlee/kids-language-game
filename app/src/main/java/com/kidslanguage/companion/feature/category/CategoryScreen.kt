package com.kidslanguage.companion.feature.category

import com.kidslanguage.companion.ui.theme.isCompactLandscape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kidslanguage.companion.core.content.ContentRepository
import com.kidslanguage.companion.core.model.Category
import com.kidslanguage.companion.core.model.LanguageCode
import com.kidslanguage.companion.core.model.MascotState
import com.kidslanguage.companion.ui.components.CategoryCard
import com.kidslanguage.companion.ui.components.MascotView
import com.kidslanguage.companion.ui.theme.AppSpacing
import com.kidslanguage.companion.ui.theme.AppTypography
import com.kidslanguage.companion.ui.theme.Neutral50
import com.kidslanguage.companion.ui.theme.Neutral900
import com.kidslanguage.companion.ui.theme.Primary100
import com.kidslanguage.companion.ui.theme.Primary600

@Composable
fun CategoryScreen(
    language: LanguageCode,
    contentRepository: ContentRepository,
    onCategorySelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compact = isCompactLandscape()
    var categories by remember(language) { mutableStateOf<List<Category>>(emptyList()) }
    var loading by remember(language) { mutableStateOf(true) }

    LaunchedEffect(language) {
        categories = contentRepository.getCategories(language)
        loading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Neutral50)
            .padding(if (compact) 12.dp else AppSpacing.xl)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(if (compact) 48.dp else 64.dp)
                    .clip(CircleShape)
                    .background(Primary100)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Primary600,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.width(AppSpacing.xl))
            Text(
                text = "${language.displayName} · 选择场景",
                style = if (compact) AppTypography.titleMedium else AppTypography.titleLarge,
                color = Neutral900
            )
        }

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (categories.isEmpty()) {
            Text("这个语言暂时没有可用内容，请返回选择其他语言。",
                modifier = Modifier.align(Alignment.Center), color = Neutral900,
                style = AppTypography.bodyLarge)
        } else Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (compact) 56.dp else 90.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(0.72f)
                    .padding(end = AppSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else AppSpacing.lg),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(categories) { category ->
                    CategoryCard(
                        title = category.title,
                        iconEmoji = category.icon,
                        onClick = { onCategorySelected(category.id) },
                        modifier = Modifier.heightIn(min = if (compact) 112.dp else 176.dp)
                    )
                }
            }

            // Right Mascot
            Column(
                modifier = Modifier.weight(0.28f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MascotView(
                    state = MascotState.Idle,
                    size = if (compact) 160.dp else 260.dp
                )
            }
        }
    }
}

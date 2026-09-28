package com.kidslanguage.companion.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

    LaunchedEffect(language) {
        categories = contentRepository.getCategories(language)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Neutral50)
            .padding(AppSpacing.xl)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(64.dp)
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
                style = AppTypography.titleLarge,
                color = Neutral900
            )
        }

        // Content Area: Grid of Categories + Mascot on the right
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(0.72f)
                    .padding(end = AppSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
            ) {
                items(categories) { category ->
                    CategoryCard(
                        title = category.title,
                        iconEmoji = category.icon,
                        onClick = { onCategorySelected(category.id) },
                        modifier = Modifier.height(150.dp)
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
                    size = 260.dp
                )
            }
        }
    }
}

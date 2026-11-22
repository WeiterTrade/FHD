package com.fhdvideobox.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.fhdvideobox.app.data.CatalogItem
import com.fhdvideobox.app.ui.theme.FhdBlue

/** Подсвечивает элемент, когда на нём фокус пульта/клавиатуры или курсор мыши. */
@Composable
fun Modifier.focusHighlight(
    interaction: MutableInteractionSource,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp),
    grow: Boolean = true,
): Modifier {
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val active = focused || hovered
    return this
        .then(if (grow && focused) Modifier.scale(1.05f) else Modifier)
        .border(
            width = if (active) 3.dp else 0.dp,
            color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
            shape = shape,
        )
}

@Composable
fun PosterImage(url: String?, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        loading = { PosterPlaceholder() },
        error = { PosterPlaceholder() },
    )
}

@Composable
private fun PosterPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
    }
}

/** Карточка в сетке: постер + подпись снизу, как в эталонном приложении. */
@Composable
fun PosterCard(item: CatalogItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier
            .padding(4.dp)
            .focusHighlight(interaction, shape)
            .clip(shape)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        PosterImage(item.poster, Modifier.fillMaxWidth().aspectRatio(2f / 2.9f))
        Text(
            item.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}

/** Фирменная надпись: [FHD] videobox */
@Composable
fun Wordmark(modifier: Modifier = Modifier, big: Boolean = false) {
    val fs = if (big) 34.sp else 20.sp
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.clip(RoundedCornerShape(if (big) 10.dp else 6.dp)).background(FhdBlue)
                .padding(horizontal = if (big) 12.dp else 7.dp, vertical = if (big) 4.dp else 2.dp)
        ) {
            Text("FHD", color = Color(0xFF041A45), fontWeight = FontWeight.Black, fontSize = fs * 0.8f)
        }
        Spacer(Modifier.width(if (big) 12.dp else 8.dp))
        Text("videobox", fontWeight = FontWeight.Bold, fontSize = fs, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun CenterMessage(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

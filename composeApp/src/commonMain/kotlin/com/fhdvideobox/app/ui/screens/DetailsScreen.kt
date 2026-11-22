package com.fhdvideobox.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.fhdvideobox.app.data.*
import com.fhdvideobox.app.ui.AppController
import com.fhdvideobox.app.ui.AppTopBar
import com.fhdvideobox.app.ui.toUserMessage
import com.fhdvideobox.app.ui.components.CenterMessage
import com.fhdvideobox.app.ui.components.Loading
import com.fhdvideobox.app.ui.components.PosterImage

@Composable
fun DetailsScreen(app: AppController, item: CatalogItem) {
    var tab by remember(item.id) { mutableStateOf(0) }
    var details by remember(item.id) { mutableStateOf<ItemDetails?>(null) }
    var detailsError by remember(item.id) { mutableStateOf<String?>(null) }
    var videos by remember(item.id) { mutableStateOf<List<VideoSource>?>(null) }
    var videosError by remember(item.id) { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    val bookmarks by app.settings.bookmarks.collectAsState()
    val bookmarked = bookmarks.any { it.id == item.id }

    LaunchedEffect(item.id, reloadKey) {
        val api = app.api ?: return@LaunchedEffect
        detailsError = null
        runCatching { api.details(item.id) }.onSuccess { details = it }.onFailure { detailsError = it.toUserMessage() }
    }
    LaunchedEffect(item.id, reloadKey) {
        val api = app.api ?: return@LaunchedEffect
        videos = null; videosError = null
        runCatching { api.videos(item.id) }.onSuccess { videos = it }.onFailure { videosError = it.toUserMessage() }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = item.title,
            subtitle = item.originalTitle ?: details?.originalTitle,
            onBack = { app.back() },
        ) {
            if (tab == 1) IconButton(onClick = { reloadKey++ }) { Icon(Icons.Default.Refresh, "Обновить") }
            IconButton(onClick = { app.settings.toggleBookmark(details?.toItem() ?: item) }) {
                Icon(if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Закладка")
            }
        }
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(tab == 0, { tab = 0 }, text = { Text("ОПИСАНИЕ") })
            Tab(tab == 1, { tab = 1 }, text = { Text("ВИДЕО") })
        }
        when (tab) {
            0 -> when {
                details != null -> DescriptionTab(details!!, onTrailer = { url -> app.platform.play("${item.title} — трейлер", null, url) }, onWatch = { tab = 1 })
                detailsError != null -> CenterMessage(detailsError!!, "Повторить") { reloadKey++ }
                else -> Loading()
            }
            else -> when {
                videos != null -> VideosTab(app, details?.toItem() ?: item, videos!!)
                videosError != null -> CenterMessage(videosError!!, "Повторить") { reloadKey++ }
                else -> Loading()
            }
        }
    }
}

@Composable
private fun DescriptionTab(d: ItemDetails, onTrailer: (String) -> Unit, onWatch: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        val poster = @Composable { mod: Modifier ->
            Column(mod, horizontalAlignment = Alignment.CenterHorizontally) {
                PosterImage(d.poster, Modifier.fillMaxWidth().aspectRatio(2f / 2.9f).clip(RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(16.dp))
                Button(onClick = onWatch) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("СМОТРЕТЬ") }
                if (d.trailer != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onTrailer(d.trailer) }) {
                        Icon(Icons.Default.PlayCircleOutline, null); Spacer(Modifier.width(6.dp)); Text("СМОТРЕТЬ ТРЕЙЛЕР")
                    }
                }
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize().padding(24.dp)) {
                poster(Modifier.width(320.dp))
                Spacer(Modifier.width(32.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { InfoBlock(d) }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                poster(Modifier.fillMaxWidth(0.65f).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(20.dp))
                InfoBlock(d)
            }
        }
    }
}

@Composable
private fun InfoBlock(d: ItemDetails) {
    Text(d.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    d.originalTitle?.let {
        Text(it, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
    }
    fun votes(v: Int?) = if (v != null) " ($v)" else ""
    InfoLine("Год", d.year?.toString())
    InfoLine("IMDB", d.imdb?.let { "$it${votes(d.imdbVotes)}" })
    InfoLine("Кинопоиск", d.kp?.let { "$it${votes(d.kpVotes)}" })
    InfoLine("Продолжительность", d.duration?.let { "$it мин." })
    InfoLine("Возраст", d.age)
    InfoLine("Жанр", d.genres.joinToString(", ").ifBlank { null })
    InfoLine("Страна", d.countries.joinToString(", ").ifBlank { null })
    InfoLine("Режиссёр", d.directors.joinToString(", ").ifBlank { null })
    InfoLine("В ролях", d.actors.joinToString(", ").ifBlank { null })
    if (!d.description.isNullOrBlank()) {
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline)
        Text(d.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun InfoLine(label: String, value: String?) {
    if (value == null) return
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$label: ") }
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(value) }
        },
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun VideosTab(app: AppController, item: CatalogItem, videos: List<VideoSource>) {
    if (videos.isEmpty()) { CenterMessage("Видео пока недоступно"); return }
    val preferred by app.settings.quality.collectAsState()
    var pick by remember { mutableStateOf<VideoSource?>(null) }

    fun start(v: VideoSource) {
        val auto = when {
            v.streams.size == 1 -> v.streams.first()
            preferred != "ASK" -> v.streams.firstOrNull { it.quality.equals(preferred, true) }
            else -> null
        }
        if (auto != null) app.play(item, v, auto) else pick = v
    }

    // Сериал: группируем по сезонам
    val grouped = videos.groupBy { it.season }
    LazyColumn(Modifier.fillMaxSize()) {
        grouped.forEach { (season, list) ->
            if (season != null) item(key = "s$season") {
                Text(
                    "$season сезон", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
            items(list, key = { it.id }) { v -> VideoRow(v) { start(v) } }
        }
    }

    pick?.let { v ->
        AlertDialog(
            onDismissRequest = { pick = null },
            title = { Text("Качество") },
            text = {
                Column {
                    v.streams.forEach { s ->
                        TextButton(onClick = { pick = null; app.play(item, v, s) }, modifier = Modifier.fillMaxWidth()) {
                            Text(s.quality, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pick = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun VideoRow(v: VideoSource, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val title = buildString {
        if (v.episode != null) {
            append("${v.episode} серия")
            if (v.episodeTitle != null) append(" — ${v.episodeTitle}")
            append("  •  ")
        }
        append(v.translation)
    }
    Row(
        Modifier.fillMaxWidth()
            .background(if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Videocam, null, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(v.qualitiesLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            v.provider?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

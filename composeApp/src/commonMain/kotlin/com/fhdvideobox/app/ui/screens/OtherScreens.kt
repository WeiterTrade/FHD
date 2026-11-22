package com.fhdvideobox.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fhdvideobox.app.AppConfig
import com.fhdvideobox.app.data.*
import com.fhdvideobox.app.ui.AppController
import com.fhdvideobox.app.ui.AppTopBar
import com.fhdvideobox.app.ui.Screen
import com.fhdvideobox.app.ui.components.CenterMessage
import com.fhdvideobox.app.ui.components.Loading
import com.fhdvideobox.app.ui.components.Wordmark
import com.fhdvideobox.app.ui.toUserMessage
import kotlinx.coroutines.delay

// ======================= Поиск =======================

@Composable
fun SearchScreen(app: AppController, onMenu: (() -> Unit)?) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CatalogItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val grid by app.settings.grid.collectAsState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    // Поиск с задержкой 500 мс после ввода
    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = null; error = null; return@LaunchedEffect }
        delay(500)
        val api = app.api ?: return@LaunchedEffect
        loading = true
        runCatching { api.search(query.trim()) }
            .onSuccess { results = it.items; error = null }
            .onFailure { error = it.toUserMessage() }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Поиск", onMenu = onMenu, onBack = if (app.backStack.size > 1) ({ app.back() }) else null)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Название фильма или сериала") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, "Очистить") } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).focusRequester(focus),
        )
        when {
            loading && results == null -> Loading()
            error != null -> CenterMessage(error!!)
            results == null -> CenterMessage("Введите хотя бы 2 символа")
            results!!.isEmpty() -> CenterMessage("Ничего не найдено")
            else -> PosterGrid(results!!, grid.minCellDp, onClick = { app.open(Screen.Details(it)) })
        }
    }
}

// ======================= Закладки / История =======================

@Composable
fun LibraryScreen(app: AppController, bookmarks: Boolean, onMenu: (() -> Unit)?) {
    val grid by app.settings.grid.collectAsState()
    val bm by app.settings.bookmarks.collectAsState()
    val hist by app.settings.history.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    val items = if (bookmarks) bm else hist.map { it.item }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(if (bookmarks) "Закладки" else "История", subtitle = "${items.size}", onMenu = onMenu) {
            if (items.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Default.DeleteSweep, "Очистить") }
        }
        if (items.isEmpty()) CenterMessage(if (bookmarks) "Добавляйте фильмы в закладки кнопкой 🔖 на странице фильма" else "Здесь появятся просмотренные фильмы")
        else PosterGrid(items, grid.minCellDp, onClick = { app.open(Screen.Details(it)) })
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text(if (bookmarks) "Очистить закладки?" else "Очистить историю?") },
        confirmButton = {
            TextButton(onClick = {
                confirmClear = false
                if (bookmarks) app.settings.clearBookmarks() else app.settings.clearHistory()
            }) { Text("Очистить") }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Отмена") } },
    )
}

// ======================= Настройки =======================

private enum class SettingsPage(val title: String) { ROOT("Настройки"), VIDEO("Видео"), SERVER("Сервер и регион"), LOOK("Вид и поведение"), DATA("Сохранённые данные") }

@Composable
fun SettingsScreen(app: AppController, onMenu: (() -> Unit)?) {
    var page by remember { mutableStateOf(SettingsPage.ROOT) }
    com.fhdvideobox.app.PlatformBackHandler(enabled = page != SettingsPage.ROOT) { page = SettingsPage.ROOT }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            page.title,
            onMenu = if (page == SettingsPage.ROOT) onMenu else null,
            onBack = if (page != SettingsPage.ROOT) ({ page = SettingsPage.ROOT }) else null,
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (page) {
                SettingsPage.ROOT -> {
                    SettingRow(Icons.Default.VideoSettings, "Видео", "Качество по умолчанию, плеер") { page = SettingsPage.VIDEO }
                    SettingRow(Icons.Default.Dns, "Сервер и регион", "Страна, адрес сервера") { page = SettingsPage.SERVER }
                    SettingRow(Icons.Default.Palette, "Вид и поведение", "Тема, размер постеров, сортировка") { page = SettingsPage.LOOK }
                    SettingRow(Icons.Default.Save, "Сохранённые данные", "История, закладки") { page = SettingsPage.DATA }
                    SettingRow(Icons.Default.Info, "О приложении", "Версия, помощь") { app.openRoot(Screen.About) }
                }
                SettingsPage.VIDEO -> VideoSettings(app)
                SettingsPage.SERVER -> ServerSettings(app)
                SettingsPage.LOOK -> LookSettings(app)
                SettingsPage.DATA -> DataSettings(app)
            }
        }
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector?, title: String, subtitle: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) ({ Text(subtitle) }) else null,
        leadingContent = if (icon != null) ({ Icon(icon, null) }) else null,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun <T> ChoiceGroup(title: String, options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
    options.forEach { o ->
        ListItem(
            headlineContent = { Text(label(o)) },
            leadingContent = { RadioButton(selected = o == selected, onClick = null) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            modifier = Modifier.clickable { onPick(o) },
        )
    }
}

@Composable
private fun VideoSettings(app: AppController) {
    val q by app.settings.quality.collectAsState()
    ChoiceGroup(
        "Качество по умолчанию",
        listOf("ASK", "AUTO", "2160p", "1080p", "720p", "480p", "360p"),
        q, { if (it == "ASK") "Всегда спрашивать" else it }, app.settings::setQuality,
    )
}

@Composable
private fun ServerSettings(app: AppController) {
    val region by app.settings.region.collectAsState()
    val custom by app.settings.customServer.collectAsState()
    var url by remember { mutableStateOf(custom) }
    var detected by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    LaunchedEffect(checking) {
        if (!checking) return@LaunchedEffect
        detected = app.geo.detectCountry() ?: "не удалось определить"
        checking = false
    }

    ChoiceGroup("Регион", Region.entries, region ?: Region.KZ, { it.title }) { app.connect(it) }
    SettingRow(Icons.Default.MyLocation, "Определить по IP", detected?.let { "Ваша страна: $it" } ?: "Проверить, где вы сейчас") { checking = true }
    SettingRow(Icons.Default.RestartAlt, "Пройти выбор региона заново", "Как при первом запуске") { app.resetRegion() }
    Text("Адрес сервера (для зеркала или тестов)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp))
    Text("Текущий: ${region?.let { app.settings.serverUrlFor(it) } ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(url, { url = it }, Modifier.weight(1f), placeholder = { Text("пусто — стандартный") }, singleLine = true)
        Spacer(Modifier.width(8.dp))
        Button(onClick = { app.settings.setCustomServer(url); region?.let { app.connect(it) } }) { Text("Применить") }
    }
}

@Composable
private fun LookSettings(app: AppController) {
    val theme by app.settings.theme.collectAsState()
    val grid by app.settings.grid.collectAsState()
    val sort by app.settings.sort.collectAsState()
    ChoiceGroup("Тема оформления", AppTheme.entries, theme, { it.title }, app.settings::setTheme)
    ChoiceGroup("Размер постеров", GridSize.entries, grid, { it.title }, app.settings::setGrid)
    ChoiceGroup("Сортировка каталога", SortOrder.entries, sort, { it.title }, app::changeSort)
}

@Composable
private fun DataSettings(app: AppController) {
    val bm by app.settings.bookmarks.collectAsState()
    val hist by app.settings.history.collectAsState()
    SettingRow(Icons.Default.History, "Очистить историю", "Записей: ${hist.size}") { app.settings.clearHistory() }
    SettingRow(Icons.Default.BookmarkRemove, "Очистить закладки", "Закладок: ${bm.size}") { app.settings.clearBookmarks() }
}

// ======================= О приложении =======================

@Composable
fun AboutScreen(app: AppController, onMenu: (() -> Unit)?) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar("О приложении", onMenu = onMenu)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(app.platform.logo(), null, Modifier.height(140.dp))
            Spacer(Modifier.height(12.dp))
            Wordmark(big = true)
            Spacer(Modifier.height(12.dp))
            Text("Версия ${AppConfig.VERSION} • ${app.platform.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            val region by app.settings.region.collectAsState()
            Text("Сервер: ${region?.title ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

package com.fhdvideobox.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fhdvideobox.app.PlatformBackHandler
import com.fhdvideobox.app.data.Section
import com.fhdvideobox.app.ui.components.Wordmark
import com.fhdvideobox.app.ui.screens.*
import com.fhdvideobox.app.ui.theme.FhdTheme
import kotlinx.coroutines.launch

/** Корень приложения: тема → выбор региона → основной интерфейс. */
@Composable
fun AppRoot(app: AppController) {
    val theme by app.settings.theme.collectAsState()
    val region by app.settings.region.collectAsState()
    FhdTheme(theme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (region == null || app.api == null) RegionGate(app) else MainShell(app)
        }
    }
}

private data class MenuEntry(val title: String, val icon: ImageVector, val screen: Screen)

private val sectionIcons = mapOf(
    Section.NOW_WATCHING to Icons.Default.PlayCircle,
    Section.NEW to Icons.Default.FiberNew,
    Section.FILMS to Icons.Default.Movie,
    Section.SERIES to Icons.Default.VideoLibrary,
    Section.CARTOONS to Icons.Default.ChildCare,
    Section.SHOWS to Icons.Default.Tv,
)

private val menuGroups: List<List<MenuEntry>> = listOf(
    Section.entries.map { MenuEntry(it.title, sectionIcons.getValue(it), Screen.Catalog(it)) },
    listOf(
        MenuEntry("Закладки", Icons.Default.Bookmark, Screen.Bookmarks),
        MenuEntry("История", Icons.Default.History, Screen.History),
    ),
    listOf(
        MenuEntry("Поиск", Icons.Default.Search, Screen.Search),
        MenuEntry("Настройки", Icons.Default.Settings, Screen.Settings),
        MenuEntry("О приложении", Icons.Outlined.Info, Screen.About),
    ),
)

/** Основной интерфейс: на ТВ/ПК/планшете меню слева всегда, на телефоне — выдвижное. */
@Composable
private fun MainShell(app: AppController) {
    val screen = app.current
    val rootScreen = app.backStack.first()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = app.platform.isTv || maxWidth >= 840.dp
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        PlatformBackHandler(enabled = true) {
            when {
                drawerState.isOpen -> scope.launch { drawerState.close() }
                !app.back() -> app.platform.exit()
                else -> {}
            }
        }

        val openMenu: (() -> Unit)? = if (wide) null else ({ scope.launch { drawerState.open() } })
        val content: @Composable () -> Unit = {
            when (screen) {
                is Screen.Catalog -> CatalogScreen(app, screen.section, openMenu)
                is Screen.Details -> DetailsScreen(app, screen.item)
                Screen.Search -> SearchScreen(app, openMenu)
                Screen.Bookmarks -> LibraryScreen(app, bookmarks = true, onMenu = openMenu)
                Screen.History -> LibraryScreen(app, bookmarks = false, onMenu = openMenu)
                Screen.Settings -> SettingsScreen(app, openMenu)
                Screen.About -> AboutScreen(app, openMenu)
            }
        }

        val menu: @Composable () -> Unit = {
            SideMenu(selected = rootScreen) { target ->
                app.openRoot(target)
                scope.launch { drawerState.close() }
            }
        }

        if (wide) {
            Row(Modifier.fillMaxSize()) {
                // Как в эталоне: на экране деталей меню скрывается, остаётся полноэкранная карточка
                if (screen !is Screen.Details) {
                    Box(Modifier.width(240.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) { menu() }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = screen !is Screen.Details,
                drawerContent = {
                    ModalDrawerSheet(Modifier.width(280.dp), drawerContainerColor = MaterialTheme.colorScheme.surface) { menu() }
                },
            ) { content() }
        }
    }
}

@Composable
private fun SideMenu(selected: Screen, onSelect: (Screen) -> Unit) {
    Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        Wordmark(Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp))
        menuGroups.forEachIndexed { gi, group ->
            if (gi > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline)
            group.forEach { e -> MenuRow(e, e.screen == selected) { onSelect(e.screen) } }
        }
    }
}

@Composable
private fun MenuRow(e: MenuEntry, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary
        focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        else -> Color.Transparent
    }
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().height(50.dp).background(bg)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(e.icon, null, tint = fg)
        Spacer(Modifier.width(18.dp))
        Text(e.title, color = fg, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
    }
}

/** Общая верхняя панель. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onMenu: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        },
        navigationIcon = {
            when {
                onBack != null -> IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                onMenu != null -> IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, "Меню") }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

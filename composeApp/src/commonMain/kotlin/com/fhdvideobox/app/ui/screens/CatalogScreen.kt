package com.fhdvideobox.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fhdvideobox.app.data.CatalogItem
import com.fhdvideobox.app.data.GridSize
import com.fhdvideobox.app.data.Section
import com.fhdvideobox.app.data.SortOrder
import com.fhdvideobox.app.ui.AppController
import com.fhdvideobox.app.ui.AppTopBar
import com.fhdvideobox.app.ui.Screen
import com.fhdvideobox.app.ui.components.CenterMessage
import com.fhdvideobox.app.ui.components.Loading
import com.fhdvideobox.app.ui.components.PosterCard

@Composable
fun CatalogScreen(app: AppController, section: Section, onMenu: (() -> Unit)?) {
    val feed = app.feed(section)
    val region by app.settings.region.collectAsState()
    val grid by app.settings.grid.collectAsState()
    val sort by app.settings.sort.collectAsState()
    var sortMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    LaunchedEffect(feed, app.api) { if (feed.items.isEmpty()) feed.loadMore() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = section.title,
            subtitle = region?.let { "Сервер: ${it.title}" },
            onMenu = onMenu,
        ) {
            IconButton(onClick = { app.open(Screen.Search) }) { Icon(Icons.Default.Search, "Поиск") }
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Сортировка") }
                DropdownMenu(sortMenu, { sortMenu = false }) {
                    SortOrder.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.title) },
                            leadingIcon = { RadioButton(selected = s == sort, onClick = null) },
                            onClick = { sortMenu = false; app.changeSort(s) },
                        )
                    }
                }
            }
            IconButton(onClick = {
                val next = GridSize.entries[(grid.ordinal + 1) % GridSize.entries.size]
                app.settings.setGrid(next)
            }) { Icon(Icons.Default.GridView, "Размер постеров") }
            Box {
                IconButton(onClick = { moreMenu = true }) { Icon(Icons.Default.MoreVert, "Ещё") }
                DropdownMenu(moreMenu, { moreMenu = false }) {
                    DropdownMenuItem(text = { Text("Обновить") }, onClick = { moreMenu = false; feed.reload() })
                    DropdownMenuItem(text = { Text("Сменить сервер") }, onClick = { moreMenu = false; app.resetRegion() })
                }
            }
        }

        when {
            feed.items.isEmpty() && feed.loading -> Loading()
            feed.items.isEmpty() && feed.error != null -> CenterMessage(feed.error!!, "Повторить") { feed.reload() }
            feed.items.isEmpty() && feed.endReached -> CenterMessage("Здесь пока ничего нет")
            else -> PosterGrid(
                items = feed.items,
                minCell = grid.minCellDp,
                loadingMore = feed.loading,
                onNearEnd = { feed.loadMore() },
                onClick = { app.open(Screen.Details(it)) },
            )
        }
    }
}

/** Сетка постеров с подгрузкой следующей страницы. */
@Composable
fun PosterGrid(
    items: List<CatalogItem>,
    minCell: Int,
    loadingMore: Boolean = false,
    onNearEnd: () -> Unit = {},
    onClick: (CatalogItem) -> Unit,
) {
    val state = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.layoutInfo.totalItemsCount - 12
        }
    }
    LaunchedEffect(nearEnd, items.size) { if (nearEnd) onNearEnd() }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minCell.dp),
        state = state,
        contentPadding = PaddingValues(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { item -> PosterCard(item, onClick = { onClick(item) }) }
        if (loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

package com.fhdvideobox.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** Простое платформенное хранилище строк (SharedPreferences на Android, файл на ПК). */
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

enum class AppTheme(val title: String) {
    DARK_GREEN("Тёмная (зелёная)"),
    DARK_BLUE("Тёмная (синяя FHD)"),
    BLACK("Чёрная (AMOLED)"),
    LIGHT("Светлая"),
}

enum class GridSize(val title: String, val minCellDp: Int) {
    SMALL("Мелкие постеры", 120),
    NORMAL("Обычные постеры", 160),
    LARGE("Крупные постеры", 210),
}

@Serializable
data class HistoryEntry(
    val item: CatalogItem,
    val videoTitle: String? = null,
    val timestamp: Long = 0,
)

class SettingsStore(private val kv: KeyValueStore) {

    private inline fun <reified T : Enum<T>> enumFlow(key: String, def: T): MutableStateFlow<T> =
        MutableStateFlow(kv.get(key)?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: def)

    // ---- Регион ----
    private val _region = MutableStateFlow(Region.byName(kv.get("region")))
    val region: StateFlow<Region?> = _region.asStateFlow()
    fun setRegion(r: Region?) { kv.put("region", r?.name); _region.value = r }

    /** Ручной адрес сервера (для разработчика / зеркала). Пусто — стандартный адрес региона. */
    private val _customServer = MutableStateFlow(kv.get("custom_server").orEmpty())
    val customServer: StateFlow<String> = _customServer.asStateFlow()
    fun setCustomServer(url: String) { kv.put("custom_server", url.trim()); _customServer.value = url.trim() }

    fun serverUrlFor(r: Region): String = _customServer.value.ifBlank { r.defaultUrl }

    // ---- Вид ----
    private val _theme = enumFlow("theme", AppTheme.DARK_GREEN)
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()
    fun setTheme(t: AppTheme) { kv.put("theme", t.name); _theme.value = t }

    private val _grid = enumFlow("grid", GridSize.NORMAL)
    val grid: StateFlow<GridSize> = _grid.asStateFlow()
    fun setGrid(g: GridSize) { kv.put("grid", g.name); _grid.value = g }

    private val _sort = enumFlow("sort", SortOrder.POPULAR)
    val sort: StateFlow<SortOrder> = _sort.asStateFlow()
    fun setSort(s: SortOrder) { kv.put("sort", s.name); _sort.value = s }

    // ---- Видео ----
    /** "ASK" — всегда спрашивать, иначе предпочитаемое качество ("1080p", "AUTO"...) */
    private val _quality = MutableStateFlow(kv.get("quality") ?: "ASK")
    val quality: StateFlow<String> = _quality.asStateFlow()
    fun setQuality(q: String) { kv.put("quality", q); _quality.value = q }

    // ---- Закладки ----
    private val itemsSer = ListSerializer(CatalogItem.serializer())
    private val _bookmarks = MutableStateFlow(load(itemsSer, "bookmarks"))
    val bookmarks: StateFlow<List<CatalogItem>> = _bookmarks.asStateFlow()

    fun isBookmarked(id: String) = _bookmarks.value.any { it.id == id }
    fun toggleBookmark(item: CatalogItem) {
        val list = _bookmarks.value.toMutableList()
        if (!list.removeAll { it.id == item.id }) list.add(0, item)
        _bookmarks.value = list
        kv.put("bookmarks", AppJson.encodeToString(itemsSer, list))
    }
    fun clearBookmarks() { _bookmarks.value = emptyList(); kv.put("bookmarks", null) }

    // ---- История ----
    private val histSer = ListSerializer(HistoryEntry.serializer())
    private val _history = MutableStateFlow(load(histSer, "history"))
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    fun addHistory(entry: HistoryEntry) {
        val list = (listOf(entry) + _history.value.filterNot { it.item.id == entry.item.id }).take(300)
        _history.value = list
        kv.put("history", AppJson.encodeToString(histSer, list))
    }
    fun clearHistory() { _history.value = emptyList(); kv.put("history", null) }

    private fun <T> load(ser: kotlinx.serialization.KSerializer<List<T>>, key: String): List<T> =
        kv.get(key)?.let { runCatching { AppJson.decodeFromString(ser, it) }.getOrNull() } ?: emptyList()
}

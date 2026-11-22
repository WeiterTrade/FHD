package com.fhdvideobox.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fhdvideobox.app.Platform
import com.fhdvideobox.app.data.*
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Экраны приложения. */
sealed interface Screen {
    data class Catalog(val section: Section) : Screen
    data class Details(val item: CatalogItem) : Screen
    data object Search : Screen
    data object Bookmarks : Screen
    data object History : Screen
    data object Settings : Screen
    data object About : Screen
}

/** Состояние одной ленты каталога (кешируется, чтобы при «Назад» не терялась прокрутка). */
class CatalogFeed(
    private val scope: CoroutineScope,
    private val apiProvider: () -> VideoboxApi?,
    val section: Section,
    private val sortProvider: () -> SortOrder,
) {
    val items = mutableStateListOf<CatalogItem>()
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var endReached by mutableStateOf(false)
        private set
    private var page = 0
    private var job: Job? = null

    fun reload() {
        job?.cancel(); items.clear(); page = 0; endReached = false; error = null; loading = false
        loadMore()
    }

    fun loadMore() {
        if (loading || endReached) return
        val api = apiProvider() ?: return
        loading = true
        job = scope.launch {
            runCatching { api.catalog(section, page + 1, sortProvider()) }
                .onSuccess { p ->
                    page = p.page
                    val known = items.map { it.id }.toHashSet()
                    items.addAll(p.items.filter { it.id !in known })
                    endReached = p.items.isEmpty() || p.page >= p.pages
                    error = null
                }
                .onFailure { error = it.toUserMessage() }
            loading = false
        }
    }
}

fun Throwable.toUserMessage(): String = when {
    this::class.simpleName?.contains("Timeout") == true -> "Сервер не отвечает. Проверьте интернет."
    this::class.simpleName?.contains("UnknownHost") == true -> "Нет подключения к интернету."
    this::class.simpleName?.contains("ResponseException") == true -> "Ошибка сервера. Попробуйте позже."
    else -> "Не удалось загрузить данные (${this::class.simpleName})."
}

/** Центральный объект приложения: навигация, текущий сервер, кеш лент. */
class AppController(
    val platform: Platform,
    val scope: CoroutineScope,
) {
    val http: HttpClient = createHttpClient()
    val settings = SettingsStore(platform.storage)
    val geo = GeoIpDetector(http)

    // ---- Сервер ----
    var api: VideoboxApi? by mutableStateOf(null)
        private set

    fun connect(region: Region) {
        settings.setRegion(region)
        api = VideoboxApi(http, settings.serverUrlFor(region))
        feeds.clear()
    }

    /** Сбросить регион — при следующем показе снова сработает определение по IP. */
    fun resetRegion() {
        settings.setRegion(null)
        api = null
        feeds.clear()
        backStack.clear(); backStack.add(Screen.Catalog(Section.FILMS))
    }

    // ---- Навигация ----
    val backStack = mutableStateListOf<Screen>(Screen.Catalog(Section.FILMS))
    val current: Screen get() = backStack.last()

    fun open(screen: Screen) { backStack.add(screen) }

    /** Пункт бокового меню — «корневой» переход без накопления стека. */
    fun openRoot(screen: Screen) {
        backStack.clear(); backStack.add(screen)
    }

    /** true — обработали, false — стек пуст (можно выходить). */
    fun back(): Boolean {
        if (backStack.size > 1) { backStack.removeAt(backStack.lastIndex); return true }
        val c = current
        if (!(c is Screen.Catalog && c.section == Section.FILMS)) { openRoot(Screen.Catalog(Section.FILMS)); return true }
        return false
    }

    // ---- Ленты ----
    private val feeds = HashMap<Section, CatalogFeed>()

    fun feed(section: Section): CatalogFeed = feeds.getOrPut(section) {
        CatalogFeed(scope, { api }, section, { settings.sort.value })
    }

    fun changeSort(s: SortOrder) {
        settings.setSort(s)
        feeds.values.forEach { it.reload() }
    }

    // ---- Воспроизведение ----
    fun play(item: CatalogItem, source: VideoSource, stream: Stream) {
        val sub = buildString {
            append(source.translation)
            if (source.season != null && source.episode != null) append(" • ${source.season} сезон, ${source.episode} серия")
            append(" • ${stream.quality}")
        }
        settings.addHistory(HistoryEntry(item, sub, platform.nowMillis()))
        platform.play(item.caption, sub, stream.url)
    }

    // Должно идти последним: к этому моменту все поля уже инициализированы.
    init {
        settings.region.value?.let { connect(it) }
    }
}

package com.fhdvideobox.app.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.json.*

/**
 * Бесплатный легальный каталог: Internet Archive (archive.org).
 * Фильмы, мультфильмы и ТВ в общественном достоянии / под свободными лицензиями.
 * Документация API: https://archive.org/developers/
 */
class ArchiveSource(private val http: HttpClient) : ContentSource {

    override val label = "Бесплатный каталог • Internet Archive"

    private val base = "https://archive.org"
    private val rows = 48

    /** Только элементы с видео в MP4, без закрытых и «взрослых». */
    private val baseFilter =
        "mediatype:(movies) AND format:(\"h.264\" OR MPEG4 OR \"512Kb MPEG4\") " +
            "AND -access-restricted-item:true " +
            "AND -subject:(sex OR sexploitation OR erotic OR adult OR nudity OR stag) AND -title:(sex)"

    private fun collection(section: Section) = when (section) {
        Section.CARTOONS -> "animationandcartoons"
        Section.SERIES -> "classic_tv"
        Section.SHOWS -> "prelinger"        // хроника, документальное и образовательное кино
        else -> "feature_films"
    }

    private fun typeOf(section: Section) = when (section) {
        Section.CARTOONS -> "cartoon"
        Section.SERIES -> "series"
        Section.SHOWS -> "show"
        else -> "film"
    }

    private fun sortKey(section: Section, sort: SortOrder) = when {
        section == Section.NOW_WATCHING -> "week desc"
        section == Section.NEW -> "addeddate desc"
        sort == SortOrder.NEW -> "addeddate desc"
        sort == SortOrder.RATING -> "avg_rating desc"
        sort == SortOrder.YEAR -> "year desc"
        else -> "downloads desc"
    }

    override suspend fun catalog(section: Section, page: Int, sort: SortOrder): CatalogPage =
        runQuery("collection:(${collection(section)}) AND $baseFilter", sortKey(section, sort), page, typeOf(section))

    override suspend fun search(query: String): CatalogPage {
        val clean = query.replace(Regex("[^\\p{L}\\p{N} ]"), " ").trim()
        if (clean.isEmpty()) return CatalogPage()
        return runQuery(
            "title:($clean) AND collection:(feature_films OR animationandcartoons OR classic_tv) AND $baseFilter",
            "downloads desc", 1, null,
        )
    }

    private suspend fun runQuery(q: String, sort: String, page: Int, type: String?): CatalogPage {
        val text = http.get("$base/advancedsearch.php") {
            parameter("q", q)
            listOf("identifier", "title", "year", "date").forEach { parameter("fl[]", it) }
            parameter("sort[]", sort)
            parameter("rows", rows)
            parameter("page", page)
            parameter("output", "json")
        }.bodyAsText()
        val resp = AppJson.parseToJsonElement(text).jsonObject["response"]?.jsonObject ?: return CatalogPage()
        val found = resp["numFound"]?.jsonPrimitive?.intOrNull ?: 0
        val items = resp["docs"]?.jsonArray.orEmpty().mapNotNull { el ->
            val d = el.jsonObject
            val id = d.str("identifier") ?: return@mapNotNull null
            CatalogItem(
                id = id,
                title = d.str("title") ?: id.replace('_', ' '),
                year = yearOf(d),
                poster = poster(id),
                type = type,
            )
        }
        return CatalogPage(items, page, maxOf(1, (found + rows - 1) / rows))
    }

    override suspend fun details(id: String): ItemDetails {
        val root = metadata(id)
        val md = root["metadata"]?.jsonObject ?: JsonObject(emptyMap())
        val videoLen = root.files().mapNotNull { it.str("length")?.toDoubleOrNull() }.maxOrNull()
        return ItemDetails(
            id = id,
            title = md.str("title") ?: id,
            year = yearOf(md),
            poster = poster(id),
            description = md.str("description")?.let(::stripHtml),
            imdb = null,
            duration = parseRuntime(md.str("runtime")) ?: videoLen?.let { (it / 60).toInt() },
            genres = md.list("subject").flatMap { it.split(';', ',') }.map { it.trim() }.filter { it.isNotEmpty() }.take(6),
            countries = md.list("country"),
            directors = md.list("director").ifEmpty { md.list("creator") },
            actors = md.list("contributor"),
            age = null,
        )
    }

    override suspend fun videos(id: String): List<VideoSource> {
        val files = metadata(id).files()
        val mp4 = files.filter { f ->
            val name = f.str("name").orEmpty()
            name.endsWith(".mp4", ignoreCase = true) && f.str("format")?.contains("Thumb", true) != true
        }
        if (mp4.isEmpty()) return emptyList()

        // Несколько вариантов качества одного исходника группируем вместе
        val groups = mp4.groupBy { f ->
            if (f.str("source") == "derivative") f.str("original") ?: f.str("name")!! else f.str("name")!!
        }.toList()

        return groups.mapIndexed { i, (key, variants) ->
            val streams = variants
                .sortedByDescending { it.str("height")?.toIntOrNull() ?: it.str("size")?.toLongOrNull()?.let { s -> (s / 1_000_000).toInt() } ?: 0 }
                .map { f -> Stream(qualityOf(f), fileUrl(id, f.str("name")!!)) }
                .distinctBy { it.quality }
            VideoSource(
                id = "$id#$i",
                translation = "Оригинал (без перевода)",
                provider = "archive.org • общественное достояние",
                episode = if (groups.size > 1) i + 1 else null,
                episodeTitle = if (groups.size > 1) key.substringAfterLast('/').substringBeforeLast('.').replace('_', ' ') else null,
                streams = streams,
            )
        }
    }

    // ---------------- helpers ----------------

    private suspend fun metadata(id: String): JsonObject =
        AppJson.parseToJsonElement(http.get("$base/metadata/${id.encodeURLPathPart()}").bodyAsText()).jsonObject

    private fun JsonObject.files(): List<JsonObject> = this["files"]?.jsonArray.orEmpty().map { it.jsonObject }

    private fun poster(id: String) = "$base/services/img/${id.encodeURLPathPart()}"

    private fun fileUrl(id: String, name: String) =
        "$base/download/${id.encodeURLPathPart()}/" + name.split('/').joinToString("/") { it.encodeURLPathPart() }

    private fun qualityOf(f: JsonObject): String {
        f.str("height")?.toIntOrNull()?.let { if (it > 0) return "${it}p" }
        return when (f.str("format")) {
            "512Kb MPEG4" -> "240p"
            "h.264 HD" -> "720p"
            else -> "SD"
        }
    }

    private fun yearOf(o: JsonObject): Int? =
        (o.str("year") ?: o.str("date"))?.let { Regex("(18|19|20)\\d{2}").find(it)?.value?.toIntOrNull() }

    /** "1:31:44" → 91, "85 min" → 85 */
    private fun parseRuntime(r: String?): Int? {
        if (r.isNullOrBlank()) return null
        val parts = r.trim().split(':').mapNotNull { it.trim().toIntOrNull() }
        if (r.contains(':') && parts.size >= 2) {
            return if (parts.size == 3) parts[0] * 60 + parts[1] else parts[0]
        }
        return Regex("\\d+").find(r)?.value?.toIntOrNull()
    }

    private fun stripHtml(s: String) = s
        .replace(Regex("(?i)<br\\s*/?>|</p>"), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

/** Поле может быть строкой или массивом строк — берём первую строку. */
internal fun JsonObject.str(key: String): String? = when (val v = this[key]) {
    is JsonPrimitive -> v.contentOrNull
    is JsonArray -> (v.firstOrNull() as? JsonPrimitive)?.contentOrNull
    else -> null
}

internal fun JsonObject.list(key: String): List<String> = when (val v = this[key]) {
    is JsonPrimitive -> listOfNotNull(v.contentOrNull)
    is JsonArray -> v.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    else -> emptyList()
}

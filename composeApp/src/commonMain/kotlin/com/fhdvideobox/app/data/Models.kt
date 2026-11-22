package com.fhdvideobox.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Разделы бокового меню. `apiKey` уходит на сервер в параметре section. */
enum class Section(val title: String, val apiKey: String) {
    NOW_WATCHING("Сейчас смотрят", "now"),
    NEW("Новинки", "new"),
    FILMS("Фильмы", "films"),
    SERIES("Сериалы", "series"),
    CARTOONS("Мультфильмы", "cartoons"),
    SHOWS("Телепередачи", "shows"),
}

enum class SortOrder(val title: String, val apiKey: String) {
    POPULAR("По популярности", "popular"),
    NEW("По дате добавления", "new"),
    RATING("По рейтингу", "rating"),
    YEAR("По году выхода", "year"),
}

@Serializable
data class CatalogItem(
    val id: String,
    val title: String,
    @SerialName("original_title") val originalTitle: String? = null,
    val year: Int? = null,
    val poster: String? = null,
    val type: String? = null,          // film | series | cartoon | show
) {
    val caption: String get() = if (year != null) "$title ($year)" else title
}

@Serializable
data class CatalogPage(
    val items: List<CatalogItem> = emptyList(),
    val page: Int = 1,
    val pages: Int = 1,
)

@Serializable
data class ItemDetails(
    val id: String,
    val title: String,
    @SerialName("original_title") val originalTitle: String? = null,
    val year: Int? = null,
    val poster: String? = null,
    val type: String? = null,
    val description: String? = null,
    val imdb: Double? = null,
    @SerialName("imdb_votes") val imdbVotes: Int? = null,
    val kp: Double? = null,
    @SerialName("kp_votes") val kpVotes: Int? = null,
    val duration: Int? = null,          // минуты
    val age: String? = null,            // "16+"
    val genres: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    val actors: List<String> = emptyList(),
    val trailer: String? = null,        // ссылка на поток трейлера
) {
    fun toItem() = CatalogItem(id, title, originalTitle, year, poster, type)
}

@Serializable
data class Stream(
    val quality: String,                // "1080p", "720p", "AUTO" ...
    val url: String,
)

/** Одна озвучка (а у сериала — одна серия в рамках озвучки). */
@Serializable
data class VideoSource(
    val id: String,
    val translation: String,            // "Дубляж", "Профессиональный (многоголосый)" ...
    val provider: String? = null,       // подпись снизу, например название вашего CDN
    val season: Int? = null,
    val episode: Int? = null,
    @SerialName("episode_title") val episodeTitle: String? = null,
    val streams: List<Stream> = emptyList(),
) {
    val qualitiesLabel: String get() = streams.joinToString(", ") { it.quality }
}

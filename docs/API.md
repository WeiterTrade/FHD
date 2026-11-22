# API сервера FHD Videobox (v1)

Каждый регион — отдельный сервер с одинаковым API. Адрес задаётся в `gradle.properties`
(`serverKz`, `serverRu`) или секретами CI `SERVER_KZ` / `SERVER_RU`. Адрес должен заканчиваться на `/`.

Все ответы — JSON в UTF-8. Неизвестные поля приложение игнорирует, необязательные можно не присылать.

## GET `api/v1/ping`
Проверка доступности. `{"ok": true}`

## GET `api/v1/catalog?section=films&page=1&sort=popular`
| параметр | значения |
|---|---|
| `section` | `now` (сейчас смотрят), `new` (новинки), `films`, `series`, `cartoons`, `shows` (телепередачи) |
| `sort` | `popular`, `new`, `rating`, `year` |
| `page` | с 1 |

```json
{
  "items": [
    {"id": "123", "title": "Название", "original_title": "Original", "year": 2024,
     "poster": "https://cdn.example/posters/123.jpg", "type": "film"}
  ],
  "page": 1,
  "pages": 40
}
```

## GET `api/v1/search?q=текст`
Ответ как у `catalog`.

## GET `api/v1/items/{id}`
```json
{
  "id": "123", "title": "…", "original_title": "…", "year": 1994, "poster": "…", "type": "film",
  "description": "…",
  "imdb": 9.3, "imdb_votes": 2630056, "kp": 9.1, "kp_votes": 850010,
  "duration": 142, "age": "16+",
  "genres": ["Драма"], "countries": ["США"], "directors": ["…"], "actors": ["…"],
  "trailer": "https://…/trailer.m3u8"
}
```

## GET `api/v1/items/{id}/videos`
Список озвучек. Для сериала — по одной записи на серию (`season`, `episode`), приложение само сгруппирует по сезонам.
```json
[
  {
    "id": "123-dub",
    "translation": "Дубляж",
    "provider": "FHD CDN",
    "streams": [
      {"quality": "AUTO",  "url": "https://cdn.example/123/master.m3u8"},
      {"quality": "1080p", "url": "https://cdn.example/123/1080.m3u8"},
      {"quality": "720p",  "url": "https://cdn.example/123/720.m3u8"}
    ]
  },
  {"id": "s1e1-dub", "translation": "Дубляж", "season": 1, "episode": 1, "episode_title": "Пилот",
   "streams": [{"quality": "AUTO", "url": "https://…/s1e1.m3u8"}]}
]
```
Форматы потоков: HLS (`.m3u8`) или MP4. Android играет через ExoPlayer, Windows/Linux — через libVLC.

## Определение страны
Приложение само определяет страну по IP через публичные сервисы (ipwho.is, ipapi.co, geojs.io, country.is).
Если хотите свой эндпоинт — добавьте его первым в `GeoIpDetector.kt`, формат ответа: `{"country_code": "KZ"}`.

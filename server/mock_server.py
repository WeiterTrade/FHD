#!/usr/bin/env python3
"""
Тестовый сервер FHD Videobox (только стандартная библиотека Python 3.8+).

    python3 server/mock_server.py            # http://0.0.0.0:8080

Сервер изображает ДВА региональных сервера:
    http://<IP>:8080/kz/   — «Казахстан»
    http://<IP>:8080/ru/   — «Россия»
Контракт API — docs/API.md. Демо-контент: открытые фильмы Blender Foundation (CC-BY).
Чтобы подключить приложение: Настройки → Сервер и регион → Адрес сервера → http://<IP вашего ПК>:8080/kz/
"""
import json, os, sys, urllib.parse
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler

HERE = os.path.dirname(os.path.abspath(__file__))
G = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/"
MUX = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

FILMS = [
    dict(id="bbb", title="Большой кролик Бак", original_title="Big Buck Bunny", year=2008, type="film", duration=10,
         genres=["Комедия", "Анимация"], countries=["Нидерланды"], directors=["Саша Гёделюк"], imdb=6.4, imdb_votes=45000, kp=6.9, kp_votes=3100, age="0+",
         description="Огромный добродушный кролик терпит издевательства трёх грызунов, пока его терпение не лопается. Открытый фильм Blender Foundation.",
         streams=[("AUTO", MUX), ("1080p", G + "BigBuckBunny.mp4")]),
    dict(id="sintel", title="Синтел", original_title="Sintel", year=2010, type="film", duration=15,
         genres=["Фэнтези", "Анимация"], countries=["Нидерланды"], directors=["Колин Левандовски"], imdb=7.4, imdb_votes=21000, age="12+",
         description="Девушка Синтел ищет маленького дракона, которого когда-то выходила и потеряла.",
         streams=[("1080p", G + "Sintel.mp4")]),
    dict(id="tos", title="Слёзы стали", original_title="Tears of Steel", year=2012, type="film", duration=12,
         genres=["Фантастика"], countries=["Нидерланды"], directors=["Ян Хьюберт"], imdb=6.2, age="12+",
         description="Группа учёных пытается спасти мир от роботов, вернувшись к событиям прошлого.",
         streams=[("1080p", G + "TearsOfSteel.mp4")]),
    dict(id="ed", title="Слоновья мечта", original_title="Elephants Dream", year=2006, type="film", duration=11,
         genres=["Фантастика", "Анимация"], countries=["Нидерланды"], directors=["Бассам Курдали"], imdb=6.3, age="6+",
         description="Двое героев блуждают по бесконечной странной машине — первый открытый фильм Blender.",
         streams=[("720p", G + "ElephantsDream.mp4")]),
    dict(id="cosmos", title="Космическая прачечная", original_title="Cosmos Laundromat", year=2015, type="film", duration=12,
         genres=["Драма", "Анимация"], countries=["Нидерланды"], imdb=7.0, age="12+",
         description="Овца по имени Франк встречает загадочного продавца, предлагающего ей прожить множество жизней.",
         streams=[("AUTO", MUX)]),
    dict(id="spring", title="Весна", original_title="Spring", year=2019, type="cartoon", duration=8,
         genres=["Фэнтези", "Анимация"], countries=["Нидерланды"], imdb=7.5, age="6+",
         description="Пастушка и её пёс встречаются с древними духами, чтобы продолжить круговорот жизни.",
         streams=[("AUTO", MUX)]),
    dict(id="agent327", title="Агент 327", original_title="Agent 327: Operation Barbershop", year=2017, type="cartoon", duration=4,
         genres=["Боевик", "Комедия"], countries=["Нидерланды"], imdb=7.8, age="12+",
         description="Агент 327 отправляется под прикрытием в парикмахерскую — тизер полнометражного фильма.",
         streams=[("AUTO", MUX)]),
    dict(id="charge", title="Заряд", original_title="Charge", year=2022, type="film", duration=4,
         genres=["Боевик"], countries=["Нидерланды"], age="16+",
         description="В мире, где энергия стала дефицитом, старик защищает последний источник от наёмницы.",
         streams=[("AUTO", MUX)]),
    dict(id="sprite", title="Ужас эльфов", original_title="Sprite Fright", year=2021, type="cartoon", duration=10,
         genres=["Ужасы", "Комедия"], countries=["Нидерланды"], imdb=7.3, age="12+",
         description="Подростки устраивают пикник в лесу и сталкиваются с его крошечными обитателями.",
         streams=[("AUTO", MUX)]),
    dict(id="hero", title="Герой", original_title="Hero", year=2018, type="cartoon", duration=4,
         genres=["Приключения"], countries=["Испания"], age="6+",
         description="Демонстрация инструмента Grease Pencil — короткая история о маленьком герое.",
         streams=[("AUTO", MUX)]),
    dict(id="coffee", title="Кофе-ран", original_title="Coffee Run", year=2020, type="cartoon", duration=3,
         genres=["Драма"], countries=["Нидерланды"], age="6+",
         description="Девушка с чашкой кофе переживает воспоминания о трудных отношениях.",
         streams=[("AUTO", MUX)]),
]
SERIES = [
    dict(id="caminandes", title="Каминандес", original_title="Caminandes", year=2013, type="series", duration=3,
         genres=["Комедия", "Анимация"], countries=["Аргентина"], imdb=7.0, age="0+",
         description="Лама Кога из Патагонии пытается пересечь дорогу, перебраться через забор и добыть ягоды.",
         episodes=[(1, "Ламигос"), (2, "Грейн Ламбо"), (3, "Ламаповеренный")]),
]
ALL = FILMS + SERIES
TRANSLATIONS = ["Дубляж", "Профессиональный (многоголосый)", "Оригинал (английский)"]


def card(x, base):
    return dict(id=x["id"], title=x["title"], original_title=x["original_title"], year=x["year"],
                type=x["type"], poster=f"{base}posters/{x['id']}.jpg")


def section_items(section):
    if section == "films": return [x for x in ALL if x["type"] == "film"]
    if section == "series": return [x for x in ALL if x["type"] == "series"]
    if section == "cartoons": return [x for x in ALL if x["type"] == "cartoon"]
    if section == "shows": return []
    if section == "new": return sorted(ALL, key=lambda x: -x["year"])
    return ALL  # now / popular


class H(BaseHTTPRequestHandler):
    def log_message(self, fmt, *a):
        sys.stderr.write("[%s] %s\n" % (self.address_string(), fmt % a))

    def send_json(self, obj, code=200):
        body = json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers(); self.wfile.write(body)

    def do_GET(self):
        u = urllib.parse.urlparse(self.path)
        q = {k: v[0] for k, v in urllib.parse.parse_qs(u.query).items()}
        parts = [p for p in u.path.split("/") if p]
        if not parts or parts[0] not in ("kz", "ru"):
            return self.send_json({"error": "use /kz/ or /ru/"}, 404)
        region, rest = parts[0], parts[1:]
        base = f"http://{self.headers.get('Host')}/{region}/"

        if rest[:1] == ["posters"] and len(rest) == 2:
            path = os.path.join(HERE, "posters", os.path.basename(rest[1]))
            if not os.path.exists(path): return self.send_json({"error": "no poster"}, 404)
            data = open(path, "rb").read()
            self.send_response(200); self.send_header("Content-Type", "image/jpeg")
            self.send_header("Content-Length", str(len(data))); self.send_header("Cache-Control", "max-age=86400")
            self.end_headers(); return self.wfile.write(data)

        if rest[:2] != ["api", "v1"]:
            return self.send_json({"error": "not found"}, 404)
        api = rest[2:]

        if api == ["ping"]:
            return self.send_json({"ok": True, "region": region.upper()})

        if api == ["catalog"]:
            items = list(section_items(q.get("section", "films")))
            sort = q.get("sort", "popular")
            if sort in ("new", "year"): items.sort(key=lambda x: -x["year"])
            elif sort == "rating": items.sort(key=lambda x: -(x.get("imdb") or 0))
            page, per = int(q.get("page", 1)), 24
            pages = max(1, (len(items) + per - 1) // per)
            chunk = items[(page - 1) * per: page * per]
            return self.send_json({"items": [card(x, base) for x in chunk], "page": page, "pages": pages})

        if api == ["search"]:
            s = q.get("q", "").lower()
            found = [card(x, base) for x in ALL if s in x["title"].lower() or s in x["original_title"].lower()]
            return self.send_json({"items": found, "page": 1, "pages": 1})

        if len(api) >= 2 and api[0] == "items":
            x = next((i for i in ALL if i["id"] == api[1]), None)
            if not x: return self.send_json({"error": "not found"}, 404)
            if len(api) == 2:
                d = card(x, base)
                d.update({k: x.get(k) for k in ("description", "imdb", "imdb_votes", "kp", "kp_votes", "duration",
                                                 "age", "genres", "countries", "directors")})
                d["actors"] = x.get("actors", [])
                d["trailer"] = MUX
                return self.send_json(d)
            if api[2:] == ["videos"]:
                provider = f"FHD CDN • {region.upper()}"
                out = []
                if x["type"] == "series":
                    for t_i, t in enumerate(TRANSLATIONS[:2]):
                        for ep, name in x["episodes"]:
                            out.append(dict(id=f"{x['id']}-{t_i}-1-{ep}", translation=t, provider=provider, season=1,
                                            episode=ep, episode_title=name, streams=[{"quality": "AUTO", "url": MUX}]))
                else:
                    for t_i, t in enumerate(TRANSLATIONS):
                        out.append(dict(id=f"{x['id']}-{t_i}", translation=t, provider=provider,
                                        streams=[{"quality": qq, "url": uu} for qq, uu in x["streams"]]))
                return self.send_json(out)

        self.send_json({"error": "not found"}, 404)


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    print(f"FHD Videobox mock server: http://0.0.0.0:{port}/kz/  и  /ru/")
    ThreadingHTTPServer(("0.0.0.0", port), H).serve_forever()

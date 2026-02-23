package eu.kanade.tachiyomi.animeextension.ru.animelib eu.kanade.tachiyomi.animeextension.ru.animelib

импорт eu.kanade.tachiyomi.animesource.model.SAnime
импорт eu.kanade.tachiyomi.animesource.model.SEpisode
импорт eu.kanade.tachiyomi.animesource.model.Video
импорт eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
импорт eu.kanade.tachiyomi.network.GET
импорт kotlinx.serialization.json.Json
импорт kotlinx.serialization.json.jsonArray
импорт kotlinx.serialization.json.jsonObject
импорт kotlinx.serialization.json.jsonPrimitive
импорт okhttp3. Заголовки
импорт okhttp3. Запрос
импорт okhttp3. Реакция
импорт org.jsoup.nodes.Document
импорт org.jsoup.nodes.Element
импорт uy.kohesive.injekt.injectLazy

класс AnimeLib : ParsedAnimeHttpSource() {

    переобойдение вал название = "AnimeLib"
    переобойдение вал baseUrl = "https://v5.animelib.org"
    переобойдение вал долго = "ru"
    переобойдение вал поддержкиПоследние = верно

    частный вал json: Json by injectLazy()

    // Данные авторизации из твоего Python-скрипта
    частный вал AUTH_TOKEN = "Bearer 1yy1aPsqCEYbe2s0iJJ83JUppulmzA"
    частный вал SITE_ID = "5"
    частный вал API_URL = "https://api.lib.social/api"
    
    // Стандартный CDN, который сейчас использует сайт
    частный вал CDN_URL = "https://cache.lib.social"

    переобойдение весело headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Authorization", AUTH_TOKEN)
        .add("site-id", SITE_ID)
        .add("Referer", "$baseUrl/")
        .add("Origin", baseURL)
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")

    переобойдение весело videoListRequest(episode: SEpisode): Request {
        // Извлекаем ID серии для запроса к API (как ты делал в Python)
        вал id = episode.url.split("/").last()
        возвращение GET("$API_URL/episodes/$id", headers)
    }

    переобойдение весело videoListParse(response: Response): List<Video> {
        вал responseData = response.body.string()
        вал jsonRoot = json.parseToJsonElement(responseData).jsonObject
        вал data = jsonRoot["data"]?.jsonObject ?: возвращение emptyList()
        вал players = data["players"]?.jsonArray ?: возвращение emptyList()

        вал videoList = mutableListOf<Video>()

        players.forEach { playerElement ->
            вал player = playerElement.jsonObject
            вал имя игрока = player["player"]?.jsonPrimitive?.content ?: ""
            вал название команды = player["team"]?.jsonObject?.получить("name")?.jsonPrimitive?.content ?: "Озвучка"

            если (playerName == "Animelib") {
                вал video = player["video"]?.jsonObject
                вал qualityArray = video?.получить("quality")?.jsonArray
                qualityArray?.forEach { qElement ->
                    вал q = qElement.jsonObject
                    вал href = q["href"]?.jsonPrimitive?.content ?: ""
                    вал качество = q["quality"]?.jsonPrimitive?.content ?: ""
                    
                    // Склеиваем CDN и путь к видео
                    вал videoUrl = если (href.startsWith("http")) href другое "$CDN_URL$href"
                    videoList.add(Video(videoUrl, "Animelib: $teamName ($quality p)", videoUrl))
                }
            } другое если (playerName == "Kodik") {
                вал sRC = player["src"]?.jsonPrimitive?.content ?: ""
                если (src.isNotEmpty()) {
                    вал url = если (src.startsWith("//")) "https:$src" другое sRC
                    videoList.add(Video(url, "Kodik: $teamName", url))
                }
            }
        }
        возвращение videoList
    }

    // Заглушки, чтобы расширение работало (Aniyomi требует их наличия)
    переобойдение весело animeDetailsParse(document: Document): SAnime = SAnime.create()
    переобойдение весело episodeFromElement(element: Element): SEpisode = SEpisode.create()
    переобойдение весело episodeListSelector(): String = ""
    переобойдение весело latestUpdatesFromElement(element: Element): SAnime = SAnime.create()
    переобойдение весело latestUpdatesNextPageSelector(): String? = null
    переобойдение весело latestUpdatesSelector(): String = ""
    переобойдение весело searchAnimeFromElement(element: Element): SAnime = SAnime.create()
    переобойдение весело searchAnimeNextPageSelector(): String? = null
    переобойдение весело searchAnimeSelector(): String = ""
    переобойдение весело popularAnimeFromElement(element: Element): SAnime = SAnime.create()
    переобойдение весело popularAnimeNextPageSelector(): String? = null
    переобойдение весело popularAnimeSelector(): String = ""
    переобойдение весело videoFromElement(element: Element): Video? = null
    переобойдение весело videoListSelector(): String = ""
    переобойдение весело videoUrlParse(document: Document): String = ""
}

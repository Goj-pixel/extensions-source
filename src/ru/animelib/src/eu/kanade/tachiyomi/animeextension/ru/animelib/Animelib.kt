package eu.kanade.tachiyomi.animeextension.ru.animelib

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy

class AnimeLib : ParsedAnimeHttpSource() {

    override val name = "AnimeLib"
    override val baseUrl = "https://v5.animelib.org"
    override val lang = "ru"
    override val supportsLatest = true

    private val json: Json by injectLazy()

    // ВСТАВЬ СВОЙ ТОКЕН ИЗ БРАУЗЕРА ЗДЕСЬ
    private val AUTH_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIxIiwianRpIjoiNDIxZDEyNTdiYzJmMTk2NjdmYzIzMWY5ZDJjMTkwOWQxMjYyZDU5MTE3YTVhNzk1ODEwMGZmY2Q5YmVkYWI3MmEyNWJiMjVhMWUxZjE0OGMiLCJpYXQiOjE3NzEzMjE5NDkuNjMyMTc3LCJuYmYiOjE3NzEzMjE5NDkuNjMyMTc5LCJleHAiOjE3NzM3NDExNDkuNjI3MDEzLCJzdWIiOiI4Mjk1NjkwIiwic2NvcGVzIjpbXX0.ef2eJAP52pVjpts70DY6HO5eS1BCyh7ypQXQV1de73lx5CyAsHuoozY7o6MKi1iSBiq82WcViUyUgFTtUpvI0GPkeJQ8AkoIwW5puM1Yx2IC9YBHEt4Nc1lwyvmGnMOnpWt0it53D_KIK1erDdRZVwOmEds67CoYwohSRTqmeqmKR-q6bE7pVvkU5tswJL1fu0DRMaZvN2arQVFakMETgMOKexqPt0ZGuUMRBwgKCXH6kTPMLQBhLObRoO7ju0gYyfOMp3k8HZkNeG1Wdy7lO9DW23RBDFMgkRqOIOQnIA7j9zHvcC40rBFi1-Eekbg4Zv3dEMOx6ngnF3L38c-pVh4EItb3MfMcu83l9TL2hW1kgLDM4kIInDBFui3IiZmekiw-T00sX-G9COw3jc9AkiwLGA1ztq2hAndC4rQQpI0GvFiCgtokyrD6KHc9KCjVcV-olwO5BepUDZgRy5mGdcHWkgs4eXbl0DRDEAjYFEBDa0n6cqbDv0y5I8CsgaLBtoGDZOOxkzXlrLs8mDVlne2UKOUGCAdoTU1TPYwFPDFUw9c-tXSOTUcMI2kTROvLB4lzOBZ5mRFLhiuvLBzgMBBnvrL3KbLDynU_Q8n3wfULy-HkH3dX7JZSjoWWjC7CSHBn9CZ482rMfYQbO6LqV6mRkn1bE0pl-ZVAHuLpE3E"
    private val SITE_ID = "5"
    private val API_URL = "https://api.lib.social/api"
    private val CDN_URL = "https://cache.lib.social"

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Authorization", AUTH_TOKEN)
        .add("site-id", SITE_ID)
        .add("Referer", "$baseUrl/")
        .add("Origin", baseUrl)
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")

    override fun videoListRequest(episode: SEpisode): Request {
        val id = episode.url.split("/").last()
        return GET("$API_URL/episodes/$id", headers)
    }

    override fun videoListParse(response: Response): List<Video> {
        val responseData = response.body.string()
        val jsonRoot = json.parseToJsonElement(responseData).jsonObject
        val data = jsonRoot["data"]?.jsonObject ?: return emptyList()
        val players = data["players"]?.jsonArray ?: return emptyList()

        val videoList = mutableListOf<Video>()

        players.forEach { playerElement ->
            val player = playerElement.jsonObject
            val playerName = player["player"]?.jsonPrimitive?.content ?: ""
            val teamName = player["team"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "Translate"

            if (playerName == "Animelib") {
                val video = player["video"]?.jsonObject
                val qualityArray = video?.get("quality")?.jsonArray
                qualityArray?.forEach { qElement ->
                    val q = qElement.jsonObject
                    val href = q["href"]?.jsonPrimitive?.content ?: ""
                    val quality = q["quality"]?.jsonPrimitive?.content ?: ""
                    
                    val videoUrl = if (href.startsWith("http")) href else "$CDN_URL$href"
                    videoList.add(Video(videoUrl, "Animelib: $teamName ($quality p)", videoUrl))
                }
            } else if (playerName == "Kodik") {
                val src = player["src"]?.jsonPrimitive?.content ?: ""
                if (src.isNotEmpty()) {
                    val url = if (src.startsWith("//")) "https:$src" else src
                    videoList.add(Video(url, "Kodik: $teamName", url))
                }
            }
        }
        return videoList
    }

    override fun animeDetailsParse(document: Document): SAnime = SAnime.create()
    override fun episodeFromElement(element: Element): SEpisode = SEpisode.create()
    override fun episodeListSelector(): String = ""
    override fun latestUpdatesFromElement(element: Element): SAnime = SAnime.create()
    override fun latestUpdatesNextPageSelector(): String? = null
    override fun latestUpdatesSelector(): String = ""
    override fun searchAnimeFromElement(element: Element): SAnime = SAnime.create()
    override fun searchAnimeNextPageSelector(): String? = null
    override fun searchAnimeSelector(): String = ""
    override fun popularAnimeFromElement(element: Element): SAnime = SAnime.create()
    override fun popularAnimeNextPageSelector(): String? = null
    override fun popularAnimeSelector(): String = ""
    override fun videoFromElement(element: Element): Video? = null
    override fun videoListSelector(): String = ""
    override fun videoUrlParse(document: Document): String = ""
}

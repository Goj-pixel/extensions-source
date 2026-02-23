package eu.kanade.tachiyomi.animeextension.ru.animelib

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
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

    // Твой токен и настройки (из Python)
    private val BACKUP_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIxIiwianRpIjoiNDIxZDEyNTdiYzJmMTk2NjdmYzIzMWY5ZDJjMTkwOWQxMjYyZDU5MTE3YTVhNzk1ODEwMGZmY2Q5YmVkYWI3MmEyNWJiMjVhMWUxZjE0OGMiLCJpYXQiOjE3NzEzMjE5NDkuNjMyMTc3LCJuYmYiOjE3NzEzMjE5NDkuNjMyMTc5LCJleHAiOjE3NzM3NDExNDkuNjI3MDEzLCJzdWIiOiI4Mjk1NjkwIiwic2NvcGVzIjpbXX0.ef2eJAP52pVjpts70DY6HO5eS1BCyh7ypQXQV1de73lx5CyAsHuoozY7o6MKi1iSBiq82WcViUyUgFTtUpvI0GPkeJQ8AkoIwW5puM1Yx2IC9YBHEt4Nc1lwyvmGnMOnpWt0it53D_KIK1erDdRZVwOmEds67CoYwohSRTqmeqmKR-q6bE7pVvkU5tswJL1fu0DRMaZvN2arQVFakMETgMOKexqPt0ZGuUMRBwgKCXH6kTPMLQBhLObRoO7ju0gYyfOMp3k8HZkNeG1Wdy7lO9DW23RBDFMgkRqOIOQnIA7j9zHvcC40rBFi1-Eekbg4Zv3dEMOx6ngnF3L38c-pVh4EItb3MfMcu83l9TL2hW1kgLDM4kIInDBFui3IiZmekiw-T00sX-G9COw3jc9AkiwLGA1ztq2hAndC4rQQpI0GvFiCgtokyrD6KHc9KCjVcV-olwO5BepUDZgRy5mGdcHWkgs4eXbl0DRDEAjYFEBDa0n6cqbDv0y5I8CsgaLBtoGDZOOxkzXlrLs8mDVlne2UKOUGCAdoTU1TPYwFPDFUw9c-tXSOTUcMI2kTROvLB4lzOBZ5mRFLhiuvLBzgMBBnvrL3KbLDynU_Q8n3wfULy-HkH3dX7JZSjoWWjC7CSHBn9CZ482rMfYQbO6LqV6mRkn1bE0pl-ZVAHuLpE3E"
    private val SITE_ID = "5"
    private val API_URL = "https://api.lib.social/api"
    private val CDN_URL = "https://cache.lib.social"

    // Динамический токен через WebView
    private fun getAuthToken(): String {
        return try {
            val cookies = client.cookieJar.loadForRequest(baseUrl.toHttpUrl())
            val token = cookies.find { it.name == "token" }?.value
            if (token != null) "Bearer $token" else BACKUP_TOKEN
        } catch (e: Exception) {
            BACKUP_TOKEN
        }
    }

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Authorization", getAuthToken())
        .add("site-id", SITE_ID)
        .add("Referer", "$baseUrl/")
        .add("Origin", baseUrl)
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")

    // --- ПОИСК И ПОПУЛЯРНОЕ ---
    override fun popularAnimeRequest(page: Int): Request {
        return GET("$API_URL/anime?page=$page&site_id[]=$SITE_ID&sort_by=rate_avg&type=anime", headers)
    }

    override fun popularAnimeParse(response: Response) = parseAnimeList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: eu.kanade.tachiyomi.animesource.model.AnimeFilterList): Request {
        return GET("$API_URL/anime?page=$page&site_id[]=$SITE_ID&q=$query&type=anime", headers)
    }

    override fun searchAnimeParse(response: Response) = parseAnimeList(response)

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$API_URL/anime?page=$page&site_id[]=$SITE_ID&sort_by=last_episode_at&type=anime", headers)
    }

    override fun latestUpdatesParse(response: Response) = parseAnimeList(response)

    private fun parseAnimeList(response: Response): eu.kanade.tachiyomi.animesource.model.AnimesPage {
        val data = json.decodeFromString<AnimeListResponse>(response.body.string())
        val animes = data.data.map { anime ->
            SAnime.create().apply {
                title = anime.rusName ?: anime.engName ?: "Без названия"
                setUrlWithoutDomain("/anime/${anime.slug}")
                thumbnail_url = anime.cover?.thumbnail
                description = anime.summary
            }
        }
        return eu.kanade.tachiyomi.animesource.model.AnimesPage(animes, data.links?.next != null)
    }

    // --- ДЕТАЛИ АНИМЕ ---
    override fun animeDetailsRequest(anime: SAnime): Request {
        // Извлекаем slug из url
        val slug = anime.url.split("/").last()
        return GET("$API_URL/anime/$slug?site_id[]=$SITE_ID", headers)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val wrapper = json.decodeFromString<AnimeDetailsResponse>(response.body.string())
        val anime = wrapper.data
        return SAnime.create().apply {
            title = anime.rusName ?: anime.engName ?: "Без названия"
            setUrlWithoutDomain("/anime/${anime.slug}")
            thumbnail_url = anime.cover?.thumbnail
            description = anime.summary
            status = when (anime.status?.id) {
                1 -> SAnime.ONGOING
                2 -> SAnime.COMPLETED
                else -> SAnime.UNKNOWN
            }
        }
    }

    // --- СПИСОК ЭПИЗОДОВ ---
    override fun episodeListRequest(anime: SAnime): Request {
        val slug = anime.url.split("/").last()
        // Сначала нам нужно получить ID аниме (числовой), но API принимает slug в episodes?anime_id=slug
        return GET("$API_URL/episodes?anime_id=$slug&site_id[]=$SITE_ID", headers)
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val wrapper = json.decodeFromString<EpisodeListResponse>(response.body.string())
        return wrapper.data.map { ep ->
            SEpisode.create().apply {
                name = "Эпизод ${ep.number} - ${ep.name ?: ""}"
                episode_number = ep.number.toFloat()
                url = "/episodes/${ep.id}" // Сохраняем ID эпизода для следующего шага
                date_upload = System.currentTimeMillis() // Можно парсить дату, но это не критично
            }
        }.reversed()
    }

    // --- ВИДЕО ---
    override fun videoListRequest(episode: SEpisode): Request {
        val id = episode.url.split("/").last()
        return GET("$API_URL/episodes/$id", headers)
    }

    override fun videoListParse(response: Response): List<Video> {
        val wrapper = json.decodeFromString<VideoDataResponse>(response.body.string())
        val players = wrapper.data.players ?: return emptyList()
        val videoList = mutableListOf<Video>()

        players.forEach { player ->
            val team = player.team?.name ?: "Unknown"
            
            if (player.player == "Animelib") {
                player.video?.quality?.forEach { q ->
                    val url = if (q.href.startsWith("http")) q.href else "$CDN_URL${q.href}"
                    videoList.add(Video(url, "Animelib: $team (${q.quality}p)", url))
                }
            } else if (player.player == "Kodik") {
                player.src?.let { src ->
                    val url = if (src.startsWith("//")) "https:$src" else src
                    videoList.add(Video(url, "Kodik: $team", url))
                }
            }
        }
        return videoList
    }

    // Заглушки (не нужны при работе через API)
    override fun animeDetailsParse(document: Document) = SAnime.create()
    override fun episodeFromElement(element: Element) = SEpisode.create()
    override fun episodeListSelector() = ""
    override fun latestUpdatesFromElement(element: Element) = SAnime.create()
    override fun latestUpdatesNextPageSelector() = null
    override fun latestUpdatesSelector() = ""
    override fun searchAnimeFromElement(element: Element) = SAnime.create()
    override fun searchAnimeNextPageSelector() = null
    override fun searchAnimeSelector() = ""
    override fun popularAnimeFromElement(element: Element) = SAnime.create()
    override fun popularAnimeNextPageSelector() = null
    override fun popularAnimeSelector() = ""
    override fun videoFromElement(element: Element) = null
    override fun videoListSelector() = ""
    override fun videoUrlParse(document: Document) = ""

    // --- DATA CLASSES (ВНУТРИ ФАЙЛА) ---
    @Serializable data class AnimeListResponse(val data: List<AnimeDto>, val links: LinksDto? = null)
    @Serializable data class AnimeDetailsResponse(val data: AnimeDto)
    @Serializable data class AnimeDto(
        val slug: String,
        @SerialName("rus_name") val rusName: String? = null,
        @SerialName("eng_name") val engName: String? = null,
        val cover: CoverDto? = null,
        val summary: String? = null,
        val status: StatusDto? = null
    )
    @Serializable data class CoverDto(val thumbnail: String? = null)
    @Serializable data class StatusDto(val id: Int)
    @Serializable data class LinksDto(val next: String? = null)

    @Serializable data class EpisodeListResponse(val data: List<EpisodeDto>)
    @Serializable data class EpisodeDto(val id: Int, val number: String, val name: String? = null)

    @Serializable data class VideoDataResponse(val data: VideoWrapper)
    @Serializable data class VideoWrapper(val players: List<PlayerDto>? = null)
    @Serializable data class PlayerDto(
        val player: String,
        val src: String? = null,
        val team: TeamDto? = null,
        val video: VideoContentDto? = null
    )
    @Serializable data class TeamDto(val name: String)
    @Serializable data class VideoContentDto(val quality: List<QualityDto>)
    @Serializable data class QualityDto(val href: String, val quality: Int)
}

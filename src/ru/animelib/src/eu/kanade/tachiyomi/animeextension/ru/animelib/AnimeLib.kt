package eu.kanade.tachiyomi.animeextension.ru.animelib

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.lib.playlistutils.PlaylistUtils
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
import java.text.SimpleDateFormat
import java.util.Locale

class AnimeLib : ParsedAnimeHttpSource() {

    override val name = "AnimeLib"

    override val baseUrl = "https://animelib.org"

    override val lang = "ru"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    private val playlistUtils by lazy { PlaylistUtils(client, headers) }

    private val backupToken = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIxIiwianRpIjoiYmZkZTk3OTFkNjY3MjI1MmFkMTk0ZDVkZDAwOTQyZDEyNWZhNmUzY2JhMWQ0OTkzNDc0NDIyOGMxNDAzMGM4YmMxZjg1MDc2MGU4ZDlkNzEiLCJpYXQiOjE3NzEyMzkxNTkuMTkyODQ0LCJuYmYiOjE3NzEyMzkxNTkuMTkyODQ1LCJleHAiOjE3NzM2NTgzNTkuMTg0MzUzLCJzdWIiOiI4Mjk1NjkwIiwic2NvcGVzIjpbXX0.h87fX5H03YudEiEOykIrl6EpL8QSxgCtqe7aHU23dnDO42KUZIqNxkkyIyUjjaW8x8ZlMxa9CPpEgEwFRLVELadoUDVOJrt5WjGFvVvzqBNgeh9fVIWC6Unb_5bR4Y9nAcbcpSJ8jCoGUaMC_RJpy_vNZydtKGqFQR2De66892J3EbemWckiBrv6IQUAJyiS0cSaRTmpFnyWhRjVIsef5we16LmCo_xnfdj7SatEamkLQwFLRGIB76pBcgKqYwMmU7QieocKz6WmrMnBYgoMK3fZcwl5nGpfULwp0ZlcOW5S-YdgOzzb1DI3clu9QQlNvmtOhXE8tXF3Kuv0I6eIjtW5QiY6PnhCu7cg5vxAWh9xvGR6XiLZQb1U7jeCwLHR1luUhdinvNAKu1siHLZWG2XH1n6KFXA95cZm0VkyGBHIofQUL8OdTXU3NM7JPiUfT9yDA67XOabjfOw6T_pHgKlHcVORCs5PtV1JnWvE9fPlQcjaAZ7Puen7l5DrgIDwnGLnY32IHCBZ-KuPXb4pGEPQRcFzaIbNBfEBh7TAuJocuvHh5Fwm34tk8DLhrYi6fyUvuhqFUopGkukG6-8ZrRYzvldT6hIRZIkRCeDROpSGX1Dshi5tQaX5Yx3LUthdLatx07MMX4Ljhs8_LSVg3cXJZamLes0cU8CXiWGlM8Y"

    private val siteId = "5"

    private val apiUrl = "https://hapi.hentaicdn.org/api"

    private val cdnUrl = "https://cache.lib.social"

    private val dateFormatter by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }

    private fun getAuthToken(): String {
        return try {
            val cookies = client.cookieJar.loadForRequest(baseUrl.toHttpUrl())
            val token = cookies.find { it.name == "token" }?.value
            if (token != null) "Bearer $token" else backupToken
        } catch (e: Exception) {
            backupToken
        }
    }

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Authorization", getAuthToken())
        .add("site-id", siteId)
        .add("client-time-zone", "Asia/Krasnoyarsk")
        .add("Referer", "$baseUrl/")
        .add("Origin", baseUrl)
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
        .add("sec-ch-ua", "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\", \"Google Chrome\";v=\"144\"")
        .add("sec-ch-ua-mobile", "?0")
        .add("sec-ch-ua-platform", "\"Windows\"")

    override fun getAnimeUrl(anime: SAnime): String = "$baseUrl/ru/anime/${anime.url}"

    override fun popularAnimeRequest(page: Int): Request = GET("$apiUrl/anime?page=$page&site_id[]=$siteId&sort_by=rate_avg&type=anime", headers)

    override fun popularAnimeParse(response: Response) = parseAnimeList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request = GET("$apiUrl/anime?page=$page&site_id[]=$siteId&q=$query&type=anime", headers)

    override fun searchAnimeParse(response: Response) = parseAnimeList(response)

    override fun latestUpdatesRequest(page: Int): Request = GET("$apiUrl/anime?page=$page&site_id[]=$siteId&sort_by=last_episode_at&type=anime", headers)

    override fun latestUpdatesParse(response: Response) = parseAnimeList(response)

    private fun parseAnimeList(response: Response): AnimesPage {
        val data = json.decodeFromString<AnimeListResponse>(response.body.string())
        val animes = data.data.map { anime ->
            SAnime.create().apply {
                title = anime.rusName ?: anime.engName ?: "No Name"
                setUrlWithoutDomain("/anime/${anime.slug}")
                thumbnail_url = anime.cover?.thumbnail
                description = anime.summary
            }
        }
        return AnimesPage(animes, data.links?.next != null)
    }

    override fun animeDetailsRequest(anime: SAnime): Request {
        val slug = anime.url.split("/").last()
        return GET("$apiUrl/anime/$slug?site_id[]=$siteId", headers)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val wrapper = json.decodeFromString<AnimeDetailsResponse>(response.body.string())
        val anime = wrapper.data
        return SAnime.create().apply {
            title = anime.rusName ?: anime.engName ?: "No Name"
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

    override fun episodeListRequest(anime: SAnime): Request {
        val slug = anime.url.split("/").last()
        return GET("$apiUrl/episodes?anime_id=$slug&site_id[]=$siteId", headers)
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val wrapper = json.decodeFromString<EpisodeListResponse>(response.body.string())
        return wrapper.data.map { ep ->
            SEpisode.create().apply {
                name = "Эпизод ${ep.number} - ${ep.name ?: ""}"
                episode_number = ep.number.toFloat()
                url = "/episodes/${ep.id}"
                date_upload = try {
                    dateFormatter.parse(ep.date ?: "")?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
        }.reversed()
    }

    override fun videoListRequest(episode: SEpisode): Request {
        val id = episode.url.split("/").last()
        return GET("$apiUrl/episodes/$id", headers)
    }

    override fun videoListParse(response: Response): List<Video> {
        val wrapper = json.decodeFromString<VideoDataResponse>(response.body.string())
        val players = wrapper.data.players ?: return emptyList()
        val videoList = mutableListOf<Video>()
        players.forEach { player ->
            val team = player.team?.name ?: "Unknown"
            if (player.player == "Animelib") {
                player.video?.quality?.forEach { q ->
                    val rawUrl = if (q.href.startsWith("http")) q.href else "$cdnUrl${q.href}"
                    if (rawUrl.contains(".m3u8")) {
                        try {
                            videoList.addAll(
                                playlistUtils.extractFromHls(
                                    rawUrl,
                                    videoNameGen = { quality: String -> "AnimeLib: $team ($quality)" },
                                ),
                            )
                        } catch (e: Exception) {
                            videoList.add(Video(rawUrl, "AnimeLib: $team (${q.quality}p)", rawUrl, headers = headers))
                        }
                    } else {
                        videoList.add(Video(rawUrl, "AnimeLib: $team (${q.quality}p)", rawUrl, headers = headers))
                    }
                }
            } else if (player.player == "Kodik") {
                player.src?.let { src ->
                    val url = if (src.startsWith("//")) "https:$src" else src
                    videoList.add(Video(url, "Kodik: $team", url, headers = headers))
                }
            }
        }
        return videoList
    }

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

    override fun videoFromElement(element: Element): Video = throw Exception("Not used")

    override fun videoListSelector() = ""

    override fun videoUrlParse(document: Document) = ""

    @Serializable
    data class AnimeListResponse(val data: List<AnimeDto>, val links: LinksDto? = null)

    @Serializable
    data class AnimeDetailsResponse(val data: AnimeDto)

    @Serializable
    data class AnimeDto(
        val slug: String,
        @SerialName("rus_name") val rusName: String? = null,
        @SerialName("eng_name") val engName: String? = null,
        val cover: CoverDto? = null,
        val summary: String? = null,
        val status: StatusDto? = null,
    )

    @Serializable
    data class CoverDto(val thumbnail: String? = null)

    @Serializable
    data class StatusDto(val id: Int)

    @Serializable
    class LinksDto(val next: String? = null)

    @Serializable
    data class EpisodeListResponse(val data: List<EpisodeDto>)

    @Serializable
    data class EpisodeDto(
        val id: Int,
        val number: String,
        val name: String? = null,
        @SerialName("created_at") val date: String? = null,
    )

    @Serializable
    data class VideoDataResponse(val data: VideoWrapper)

    @Serializable
    data class VideoWrapper(val players: List<PlayerDto>? = null)

    @Serializable
    data class PlayerDto(
        val player: String,
        val src: String? = null,
        val team: TeamDto? = null,
        val video: VideoContentDto? = null,
    )

    @Serializable
    data class TeamDto(val name: String)

    @Serializable
    data class VideoContentDto(val quality: List<QualityDto>)

    @Serializable
    data class QualityDto(val href: String, val quality: Int)
}

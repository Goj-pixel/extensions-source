package eu.kanade.tachiyomi.animeextension.ru.animelib

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.lib.playlistutils.PlaylistUtils
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.parseAs
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale

class Animelib : AnimeHttpSource() {

    override val name = "Animelib"
    override val baseUrl = "https://v5.animelib.org"
    override val lang = "ru"
    override val supportsLatest = true

    private val apiUrl = "https://hapi.hentaicdn.org/api"
    private val cdnUrl = "https://cache.lib.social"

    private val playlistUtils by lazy { PlaylistUtils(client, headers) }
    private val dateFormatter by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }

    private val backupToken = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIxIiwianRpIjoiNDIxZDEyNTdiYzJmMTk2NjdmYzIzMWY5ZDJjMTkwOWQxMjYyZDU5MTE3YTVhNzk1ODEwMGZmY2Q5YmVkYWI3MmEyNWJiMjVhMWUxZjE0OGMiLCJpYXQiOjE3NzEzMjE5NDkuNjMyMTc3LCJuYmYiOjE3NzEzMjE5NDkuNjMyMTc5LCJleHAiOjE3NzM3NDExNDkuNjI3MDEzLCJzdWIiOiI4Mjk1NjkwIiwic2NvcGVzIjpbXX0.ef2eJAP52pVjpts70DY6HO5eS1BCyh7ypQXQV1de73lx5CyAsHuoozY7o6MKi1iSBiq82WcViUyUgFTtUpvI0GPkeJQ8AkoIwW5puM1Yx2IC9YBHEt4Nc1lwyvmGnMOnpWt0it53D_KIK1erDdRZVwOmEds67CoYwohSRTqmeqmKR-q6bE7pVvkU5tswJL1fu0DRMaZvN2arQVFakMETgMOKexqPt0ZGuUMRBwgKCXH6kTPMLQBhLObRoO7ju0gYyfOMp3k8HZkNeG1Wdy7lO9DW23RBDFMgkRqOIOQnIA7j9zHvcC40rBFi1-Eekbg4Zv3dEMOx6ngnF3L38c-pVh4EItb3MfMcu83l9TL2hW1kgLDM4kIInDBFui3IiZmekiw-T00sX-G9COw3jc9AkiwLGA1ztq2hAndC4rQQpI0GvFiCgtokyrD6KHc9KCjVcV-olwO5BepUDZgRy5mGdcHWkgs4eXbl0DRDEAjYFEBDa0n6cqbDv0y5I8CsgaLBtoGDZOOxkzXlrLs8mDVlne2UKOUGCAdoTU1TPYwFPDFUw9c-tXSOTUcMI2kTROvLB4lzOBZ5mRFLhiuvLBzgMBBnvrL3KbLDynU_Q8n3wfULy-HkH3dX7JZSjoWWjC7CSHBn9CZ482rMfYQbO6LqV6mRkn1bE0pl-ZVAHuLpE3E"

    private fun getAuthToken(): String {
        val token = client.cookieJar.loadForRequest(baseUrl.toHttpUrl()).find { it.name == "token" }?.value
        return if (token != null) "Bearer $token" else backupToken
    }

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Authorization", getAuthToken())
        .add("site-id", "5")
        .add("Referer", "$baseUrl/")
        .add("Origin", baseUrl)
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")

    override fun getAnimeUrl(anime: SAnime) = "$baseUrl/ru/anime/${anime.url}"

    override fun popularAnimeRequest(page: Int) = GET("$apiUrl/anime?page=$page&site_id[]=5&sort_by=rate_avg&type=anime", headers)
    override fun popularAnimeParse(response: Response): AnimesPage {
        val res = response.parseAs<AnimeList>()
        return AnimesPage(res.data.map { it.toSAnime() }, res.links?.next != null)
    }

    override fun animeDetailsRequest(anime: SAnime) = GET("$apiUrl/anime/${anime.url}?fields[]=summary&fields[]=status", headers)
    override fun animeDetailsParse(response: Response) = response.parseAs<AnimeInfo>().data.toSAnime()

    override fun episodeListRequest(anime: SAnime) = GET("$apiUrl/episodes?anime_id=${anime.url}", headers)
    override fun episodeListParse(response: Response) = response.parseAs<EpisodeList>().data.map { it.toSEpisode() }.reversed()

    override fun videoListRequest(episode: SEpisode) = GET("$apiUrl/episodes/${episode.url}", headers)
    override fun videoListParse(response: Response): List<Video> {
        val players = response.parseAs<EpisodeVideoData>().data.players ?: return emptyList()
        val videoHeaders = headersBuilder().build() // Берем все заголовки для плеера

        return players.flatMap { player ->
            val team = player.team.name
            if (player.player == "Animelib" && player.video != null) {
                player.video.quality.flatMap { q ->
                    val url = if (q.href.startsWith("http")) q.href else "$cdnUrl${q.href}"
                    if (url.contains(".m3u8")) {
                        playlistUtils.extractFromHls(url, urlHeaders = videoHeaders, videoNameGen = { "$team - $it (Animelib)" })
                    } else {
                        listOf(Video(url, "$team - ${q.quality}p", url, headers = videoHeaders))
                    }
                }
            } else if (player.player == "Kodik" && player.src != null) {
                val url = if (player.src.startsWith("//")) "https:${player.src}" else player.src
                listOf(Video(url, "Kodik: $team", url, headers = videoHeaders))
            } else {
                emptyList()
            }
        }
    }

    override fun latestUpdatesRequest(page: Int) = GET("$apiUrl/anime?page=$page&site_id[]=5&sort_by=last_episode_at&type=anime", headers)
    override fun latestUpdatesParse(response: Response) = popularAnimeParse(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return GET("$apiUrl/anime?q=$query&page=$page&site_id[]=5&type=anime", headers)
    }
    override fun searchAnimeParse(response: Response) = popularAnimeParse(response)

    private fun AnimeData.toSAnime() = SAnime.create().apply {
        // ИСПРАВЛЕНИЕ 422: Сохраняем полный slug_url (например 25788--tensui-no...)
        url = href 
        title = rusName
        thumbnail_url = cover.thumbnail
        description = summary
    }

    private fun EpisodeInfo.toSEpisode() = SEpisode.create().apply {
        url = id.toString()
        name = "Серия $number $episodeName"
        episode_number = number.toFloat()
        date_upload = try { dateFormatter.parse(date)?.time ?: 0L } catch (e: Exception) { 0L }
    }
}

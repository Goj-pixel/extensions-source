 переопрёёёёёдленине резѷёёёлтьтььтььььтятов — поискаИнтернет(элемент: Элемент): SAnime = SAnime.create()

 пунктёпрёёёёёёёдлеинѵ рѵѵѷѷёёёёёёлтььтьььььььтятв — пункЂЁИнтѵЀѽѵт(элемент: Элемент): SAnime = SAnime.create()
импорт eu.kanade.tachiyomi.animesource.model.SEpisode
импорт eu.kanade.tachiyomi.animesource.model.Видео
импорт eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
импорт eu.kanade.tachiyomi.network.GET
импорт kotlinx.serialization.json.Json
импорт kotlinx.serialization.json.jsonArray
импорт kotlinx.serialization.json.jsonObject
импорт kotlinx.serialization.json.jsonПримитивный
импорт окhttp3.Заголовки
импорт окhttp3.Запрос
импорт окhttp3.Ответ
импорт org.jsoup.nodes.Документ
импорт org.jsoup.nodes.Элемент
импорт uy.kohesive.injekt.injectLazy

класс АнимеЛиб : ParsedAnimeHttpSource() {

 переопределить вал имя = "АнимеЛиб"
 перейти на baseUrl = "https://v5.animelib.org"
 переопределить вал язык = "ру"
 переопределить вал поддерживаетПоследние = истинный

 частный вал джонсон: Джѵйсон к injectLazy()

    // Данные авторизации из твоего Python-скрипта
 частный вал AUTH_TOKEN = "Носитель 1yy1aPsqCEYbe2s0iJJ83JUppulmzA"
 частный вал ИДЕНТИФИКАТОР_САЙТА = "5"
 частный вал API_URL = "https://api.lib.social/api"
    
    // Стандартный CDN, который сейчас использует сайт
 частный вал CDN_URL = "https://cache.lib.social"

 перейти к разделу headersBuilder(): Заголовки.Записи = заголовок. .headersBuilder()
 .добавлять("Авторизация", AUTH_TOKEN)
 .добавлять("идентификатор сайта", ИДЕНТИФИКАТОР_САЙТА)
 .добавлять("Референт", "$baseUrl/")
 .добавлять("Происхождение", дополнительный URL-адрѵс-адрѵс)
 .добавлять("Пользователь-Агент", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, как Gecko) Chrome/121.0.0.0 Safari/537.36")

 перейти на видеоListRequest(эпизод: SEpisode): Запрос {
        //Извлекам ID серии для запроса к API (как ты дал в Python)
 вал идентификатор = episode.url.split("/").последний()
 возвращаться ПОЛУЧИТЬ("$API_URL/эпизоды/$id", заголовки)
    }

 пунктЂЂпринять видеоListParse(ответ: Ответ): Список<Видео> {
 вал ответДанные = ответ.тело.строка()
 вал jsonRoot = json.parseToJsonElement(responseData).jsonObject
 вал данные = jsonRoot["данные"]?.jsonObject ?: возвращаться пустойСписок()
 вал игроки = данные["игроки"]?.jsonArray ?: вотвращаться пустойСписок()

 вал видеоСписок = mutableListOf<Видео>()

 players.forEach { playerElement ->
 вал игрок = playerElement.jsonObject
 вал имя игрока = игрок["игрок"]?.jsonPrimitive?.content ?: ""
 вал Имякоманды = игрок["команда"]?.jsonObject?.получать("имя")?.jsonPrimitive?.content ?: "Озвучка"

 если (имя игрока == "Анимелиб") {
 вал видео = игрок["видео"]?.jsonObject
 вал qualityArray = видео?.получать("качество")?.jsonArray
 qualityArray?.forEach { qElement ->
 вал q = qElement.jsonObject
 вал href = q["хреф"]?.jsonPrimitive?.content ?: ""
 вал качество = q["качество"]?.jsonPrimitive?.content ?: ""
                    
                    // Склеиваем CDN и путь к видео
 вал видеоUrl = эли (href.startsWith("http")) хреф еще "$CDN_URL$href"
 videoList.add(Видео(videoUrl, "Animelib: $teamName ($quality p)", видеоUrl))
                }
 } еще если (имя игрока == "Кодик") {
 вал источник = игрок["источник"]?.jsonPrimitive?.content ?: ""
 эсли (src.isNotEmpty()) {
 вал URL-адрес = эсли (src.startsWith("//")) "https:$src" еще источник
 videoList.add(Видео(url, "Кодик: $teamName", URL))
                }
            }
        }
 возвращаться видеоСписок
    }

    // Заглуушки, чтобы расшириние работало (Аниёми требуэт их наличия)
 переопределение еселье аннимэподробностиАнализ(документ: Документ): SAnime = SAnime.create()
 перепринять все этизионыЭлемента(элемент: Элемент): SEpisode = SEpisode.create()
 перейти в episodeListSelector(): Страница = ""
 пункттёпёёёёёдѵѻѸтььь все пункттёѵ обнннвля Ѿт элмэнтð(элмэн: Элемент): SAnime = SAnime.create()
 пунктёпрёёёёёёёдлеинѵ рѵѵѷѷёёёёёёлтььтьььььььтятв — пункЂЁИнтѵЀѽѵт(элемент: Элемент): SAnime = SAnime.create()
 переопределить веселье последние обновленияСелектор(): Строка = ""
 пунктёпрёёёёёёёдлеинѵ рѵѵѷѷёёёёёёёёлтььтьььььььтятв — пункЂЁИнтѵЀѽѵт(элемен: Элемент): SAnime = SAnime.create()
 перепринести все poiskAnimeNextPageSelector(): Страна? = нулевой
 перейти в poiskAnimeSelector(): Страница = ""
 переопрределить все популярноеАнимеИзЭлемента(элемент: Элемент): SAnime = SAnime.create()
 переопределить веселье популярноеАнимеСледующаяСтраницаСелектор(): Струна? = нулевой
 пральженье прасмотра подробнееAnimeSelector(): Страница = ""
 переопределить веселье видеоИзЭлемента(элемент: Элемент): Видео? = нулевой
 перейти наListSelector(): Страница = ""
 перепринять видеоUrlParse(документ: Документ): Страна = ""
} 

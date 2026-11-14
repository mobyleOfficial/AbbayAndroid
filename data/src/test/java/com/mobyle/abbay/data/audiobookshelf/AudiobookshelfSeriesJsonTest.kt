package com.mobyle.abbay.data.audiobookshelf

import com.model.AudiobookshelfSeries
import com.model.CatalogSeries
import org.junit.Assert.assertEquals
import org.junit.Test

class AudiobookshelfSeriesJsonTest {
    private val coverOf = { id: String -> "cover/$id" }

    @Test
    fun aSeriesShowsTheCoverOfItsFirstBook() {
        val body = """{"results":[{"id":"s1","name":"Dune","books":[
            {"id":"b2","media":{"metadata":{"title":"Dune Messiah","seriesName":"Dune #2"}}},
            {"id":"b1","media":{"metadata":{"title":"Dune","seriesName":"Dune #1"}}}
        ]}]}"""

        assertEquals(listOf(AudiobookshelfSeries("s1", "Dune", 2, "cover/b1")), AudiobookshelfSeriesJson.series(body, coverOf))
    }

    @Test
    fun seriesBooksAreInTheirOrderWithUnnumberedOnesLast() {
        val body = """{"results":[
            {"id":"extra","libraryId":"lib","mediaType":"book","media":{"numAudioFiles":1,"metadata":{"title":"Extra","seriesName":"Dune"}}},
            {"id":"b10","libraryId":"lib","mediaType":"book","media":{"numAudioFiles":1,"metadata":{"title":"Ten","seriesName":"Dune #10"}}},
            {"id":"b2","libraryId":"lib","mediaType":"book","media":{"numAudioFiles":1,
              "metadata":{"title":"Two","series":{"id":"s1","name":"Dune","sequence":"2"}}}},
            {"id":"b1","libraryId":"lib","mediaType":"book","media":{"numAudioFiles":1,"metadata":{"title":"One","seriesName":"Other #4, Dune #1"}}}
        ]}"""

        val books = AudiobookshelfSeriesJson.seriesBooks(body, "s1", coverOf)

        assertEquals(listOf("b1", "b2", "b10", "extra"), books.map { it.id })
        assertEquals(CatalogSeries("s1", "Dune", "1", "lib"), books.first().series)
    }

    @Test
    fun aFullItemLinksToItsSeries() {
        val body = """{"id":"b1","libraryId":"lib","mediaType":"book","media":{
            "metadata":{"title":"Dune","series":[{"id":"s1","name":"Dune","sequence":"1"}]},
            "audioFiles":[{"index":1,"ino":"1","duration":1.0,"metadata":{"filename":"01.mp3","size":1}}]}}"""

        val book = AudiobookshelfJson.book(body, "https://abs", coverOf)!!

        assertEquals(CatalogSeries("s1", "Dune", "1", "lib"), book.series)
    }

    @Test
    fun aPageHasMoreUntilItsTotalIsReached() {
        assertEquals(true, hasMorePages("""{"results":[{},{}],"total":5}""", page = 1, limit = 2))
        assertEquals(false, hasMorePages("""{"results":[{}],"total":5}""", page = 2, limit = 2))
        assertEquals(true, hasMorePages("""{"results":[{},{}]}""", page = 0, limit = 2))
        assertEquals(false, hasMorePages("""{"results":[{}]}""", page = 0, limit = 2))
    }

    @Test
    fun theSeriesFilterCarriesTheIdInBase64() {
        assertEquals("series.czE=", AudiobookshelfUrls.seriesFilter("s1"))
    }
}

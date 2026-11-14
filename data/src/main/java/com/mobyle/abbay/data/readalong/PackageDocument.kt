package com.mobyle.abbay.data.readalong

import android.util.Xml
import org.xmlpull.v1.XmlPullParser

/** What read-along needs from an EPUB's package document: its language and its chapters' files in reading order. */
internal class PackageDocument(val language: String?, val spine: List<String>) {
    companion object {
        fun parse(opf: String): PackageDocument {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(opf.reader())
            }
            val manifest = HashMap<String, String>()
            val spineIds = ArrayList<String>()
            var language: String? = null
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                when (parser.name.substringAfter(':')) {
                    "language" -> if (language == null) language = parser.nextText().trim().ifEmpty { null }
                    "item" -> {
                        val id = parser.getAttributeValue(null, "id")
                        val href = parser.getAttributeValue(null, "href")
                        if (id != null && href != null) manifest[id] = href
                    }
                    "itemref" -> if (parser.getAttributeValue(null, "linear") != "no") {
                        parser.getAttributeValue(null, "idref")?.let(spineIds::add)
                    }
                }
            }
            return PackageDocument(language, spineIds.mapNotNull(manifest::get))
        }
    }
}

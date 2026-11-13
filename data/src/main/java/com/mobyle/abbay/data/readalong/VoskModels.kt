package com.mobyle.abbay.data.readalong

/**
 * The speech models read-along can download: Vosk's small models (Apache 2.0, 30–50 MB each, made for phones), one
 * per language, pinned to an exact archive, size and SHA-256 so a changed or tampered file is never unpacked. Adding a
 * language means downloading its archive once and recording these three values.
 */
object VoskModels {
    const val HOST = "alphacephei.com"

    data class Model(val locale: String, val archive: String, val sizeBytes: Long, val sha256: String) {
        val url: String get() = "https://$HOST/vosk/models/$archive"

        /** The folder the archive holds the model in. */
        val folder: String get() = archive.removeSuffix(".zip")
    }

    val all: List<Model> = listOf(
        Model("en-US", "vosk-model-small-en-us-0.15.zip", 41_205_931, "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498"),
        Model("pt-BR", "vosk-model-small-pt-0.3.zip", 32_453_112, "6e1ce909032e1afa7a88e68a3d628ecafff302bdf195befab308826c395e93b7"),
        Model("es-ES", "vosk-model-small-es-0.42.zip", 39_817_833, "09b239888f633ef2f0b4e09736e3d9936acfd810bc65d53fad45261762c6511f"),
        Model("fr-FR", "vosk-model-small-fr-0.22.zip", 42_233_323, "cabf6180e177eb9b3a9a9d43a437bd5e549f3a7d09525e5d69a3fed787be12ad"),
        Model("de-DE", "vosk-model-small-de-0.15.zip", 46_499_967, "b7e53c90b1f0a38456f4cd62b366ecd58803cd97cd42b06438e2c131713d5e43"),
        Model("it-IT", "vosk-model-small-it-0.22.zip", 49_665_141, "9ec65e75861d1c6c2e457cccd932705340dcdf233f5b239f00733b4de0bf3267"),
        Model("nl-NL", "vosk-model-small-nl-0.22.zip", 40_441_176, "039811c3b829de64e4f123a9f684a53784005b212a346ac0b899dc7efce2ed0a"),
        Model("ru-RU", "vosk-model-small-ru-0.22.zip", 46_236_750, "961d5ff98a17f4aa6de69864d0aa71fa5bac682301d2b5d17a3f24c5c99a46d4"),
        Model("pl-PL", "vosk-model-small-pl-0.22.zip", 52_979_372, "c4cd16498ea544f446f9e9a55cbd602b71cfe5a2b6f2b0834d81e1b6fce15f0d"),
        Model("ca-ES", "vosk-model-small-ca-0.4.zip", 43_405_881, "99f90bfff5c2b187c705ddbc20aaa2600eddc359466f404404f1db6029fba5d4"),
        Model("tr-TR", "vosk-model-small-tr-0.3.zip", 36_855_784, "8c8d07cec1bce31add14967c16891c84152cdf76d391e33c08278119b9ea96e5"),
        Model(
            "cs-CZ",
            "vosk-model-small-cs-0.4-rhasspy.zip",
            46_088_666,
            "287c3bbefc8ad67b4ab9636eecef3d62acc3719990777d03e226db5a7f19fbda"
        ),
    )

    /** The model for a locale id, or for its language when the region differs ("pt-PT" reads with the pt-BR model). */
    fun forLocale(locale: String): Model? {
        val normalised = locale.replace('_', '-')
        val language = normalised.substringBefore('-').lowercase()
        return all.firstOrNull { it.locale.equals(normalised, ignoreCase = true) }
            ?: all.firstOrNull { it.locale.substringBefore('-').equals(language, ignoreCase = true) }
    }
}

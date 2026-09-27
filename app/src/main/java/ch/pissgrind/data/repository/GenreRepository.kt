package ch.pissgrind.data.repository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenreRepository @Inject constructor() {

    /**
     * All valid genres. This is the single source of truth.
     * Only genres defined here are used in recommendations, UI, and training.
     */
    private val validGenres: Set<String> = setOf(

        // ===== Grind / Gore ===== Add new Subgenres here if needed
        "Grindcore",
        "Goregrind",
        "Deathgrind",
        "Cybergrind",
        "Pornogrind",
        "Noisegrind",
        "Grindgore",
        "Mincecore",
        "Mincegore",


        // Metal
        "Black Metal",
        "Death Metal",
        "Doom Metal",
        "Drone Metal",
        "Folk Metal",
        "Gothic Metal",
        "Heavy Metal",
        "Industrial Metal",
        "Melodic Death Metal",
        "Metalcore",
        "Mathcore",
        "Noise Metal",
        "Nu Metal",
        "Post Metal",
        "Power Metal",
        "Progressive Metal",
        "Sludge Metal",
        "Speed Metal",
        "Stoner Metal",
        "Symphonic Metal",
        "Technical Death Metal",
        "Thrash Metal",
        "War Metal",

        // Punk
        "Crust Punk",
        "D-Beat",
        "Hardcore Punk",
        "Post Punk",
        "Powerviolence",
        "Punk Rock",
        "Skate Punk",
        "Street Punk",
        "Deutschpunk",
        "Synth Punk",

        // Rock
        "Alternative Rock",
        "Blues Rock",
        "Classic Rock",
        "Garage Rock",
        "Grunge",
        "Hard Rock",
        "Indie Rock",
        "Math Rock",
        "Noise Rock",
        "Post Rock",
        "Progressive Rock",
        "Psychedelic Rock",
        "Shoegaze",
        "Stoner Rock",
        "Surf Rock",

        // Electronic
        "Ambient",
        "Breakcore",
        "Darkwave",
        "Drum and Bass",
        "EBM",
        "Electro",
        "IDM",
        "Industrial",
        "Synthwave",
        "Techno",
        "Vaporwave",
        "Acid Core",

        // Hip-Hop / Rap
        "Boom Bap",
        "Conscious Hip-Hop",
        "Gangsta Rap",
        "Hip-Hop",
        "Trap",
        "Deutschrap",

        // Other
        "Blues",
        "Country",
        "Dark Folk",
        "Experimental",
        "Folk",
        "Jazz",
        "Neofolk",
        "Reggae",
        "Singer-Songwriter",
        "Ska",
        "Soul",
        "Soundtrack",
        "Spoken Word",
        "Noise",
        "World Music"
    )

    /**
     * Maps alternative spellings, abbreviations, and variations to the canonical genre name.
     * Keys are lowercase for case-insensitive matching.
     */
    private val aliasMap: Map<String, String> = buildMap {
        // ADD ANY ALTERNATIVE WAYS OF TYPING GENRES HERE

        // Black Metal
        put("black metal", "Black Metal")
        put("black", "Black Metal")
        put("bm", "Black Metal")
        put("atmospheric black metal", "Black Metal")
        put("raw black metal", "Black Metal")
        put("dsbm", "Black Metal")
        put("depressive black metal", "Black Metal")
        put("depressive suicidal black metal", "Black Metal")
        put("blackgaze", "Black Metal")

        // Death Metal
        put("death metal", "Death Metal")
        put("death", "Death Metal")
        put("dm", "Death Metal")
        put("brutal death metal", "Death Metal")
        put("old school death metal", "Death Metal")
        put("osdm", "Death Metal")

        // Technical Death Metal
        put("technical death metal", "Technical Death Metal")
        put("tech death", "Technical Death Metal")
        put("tech-death", "Technical Death Metal")
        put("progressive death metal", "Technical Death Metal")

        // Melodic Death Metal
        put("melodic death metal", "Melodic Death Metal")
        put("melodeath", "Melodic Death Metal")
        put("melo-death", "Melodic Death Metal")
        put("mdm", "Melodic Death Metal")

        // Doom Metal
        put("doom metal", "Doom Metal")
        put("doom", "Doom Metal")
        put("funeral doom", "Doom Metal")
        put("funeral doom metal", "Doom Metal")
        put("death doom", "Doom Metal")
        put("death/doom", "Doom Metal")
        put("death-doom", "Doom Metal")

        // Drone Metal
        put("drone metal", "Drone Metal")
        put("drone", "Drone Metal")
        put("drone doom", "Drone Metal")

        // Sludge Metal
        put("sludge metal", "Sludge Metal")
        put("sludge", "Sludge Metal")
        put("sludgecore", "Sludge Metal")

        // Stoner Metal
        put("stoner metal", "Stoner Metal")
        put("stoner doom", "Stoner Metal")

        // Stoner Rock
        put("stoner rock", "Stoner Rock")
        put("stoner", "Stoner Rock")
        put("desert rock", "Stoner Rock")

        // Post Metal
        put("post-metal", "Post Metal")
        put("post metal", "Post Metal")
        put("atmospheric metal", "Post Metal")

        // ===== Grindcore & Subgenres =====
        put("grindcore", "Grindcore")
        put("grind", "Grindcore")
        put("grind core", "Grindcore")

        put("goregrind", "Goregrind")
        put("gore grind", "Goregrind")
        put("gore", "Goregrind")

        put("deathgrind", "Deathgrind")
        put("death grind", "Deathgrind")
        put("death/grind", "Deathgrind")

        put("cybergrind", "Cybergrind")
        put("cyber grind", "Cybergrind")

        put("pornogrind", "Pornogrind")
        put("porno grind", "Pornogrind")

        put("noisegrind", "Noisegrind")
        put("noise grind", "Noisegrind")

        put("grindgore", "Grindgore")
        put("mincecore", "Mincecore")
        put("mince core", "Mincecore")
        put("mincegore", "Mincegore")
        put("mince gore", "Mincegore")
        put("mince", "Mincecore")


        // Thrash Metal
        put("thrash metal", "Thrash Metal")
        put("thrash", "Thrash Metal")
        put("crossover thrash", "Thrash Metal")
        put("crossover", "Thrash Metal")

        // Speed Metal
        put("speed metal", "Speed Metal")
        put("speed", "Speed Metal")

        // Heavy Metal
        put("heavy metal", "Heavy Metal")
        put("metal", "Heavy Metal")
        put("nwobhm", "Heavy Metal")
        put("traditional heavy metal", "Heavy Metal")
        put("trad metal", "Heavy Metal")

        // Power Metal
        put("power metal", "Power Metal")
        put("power", "Power Metal")

        // Progressive Metal
        put("progressive metal", "Progressive Metal")
        put("prog metal", "Progressive Metal")
        put("prog-metal", "Progressive Metal")

        // Metalcore
        put("metalcore", "Metalcore")
        put("deathcore", "Metalcore")
        put("melodic metalcore", "Metalcore")
        put("melodic hardcore", "Metalcore")

        // Mathcore
        put("mathcore", "Mathcore")
        put("chaotic hardcore", "Mathcore")

        // Industrial Metal
        put("industrial metal", "Industrial Metal")

        // Noise Metal
        put("noise metal", "Noise Metal")
        put("noisecore", "Noise Metal")

        // Nu Metal
        put("nu metal", "Nu Metal")
        put("nu-metal", "Nu Metal")
        put("nü metal", "Nu Metal")
        put("rap metal", "Nu Metal")

        // Folk Metal
        put("folk metal", "Folk Metal")
        put("viking metal", "Folk Metal")
        put("pagan metal", "Folk Metal")

        // Gothic Metal
        put("gothic metal", "Gothic Metal")
        put("goth metal", "Gothic Metal")

        // Symphonic Metal
        put("symphonic metal", "Symphonic Metal")

        // War Metal
        put("war metal", "War Metal")
        put("bestial black metal", "War Metal")
        put("black/death", "War Metal")
        put("black/death metal", "War Metal")

        // Crust Punk
        put("crust punk", "Crust Punk")
        put("crust", "Crust Punk")
        put("crustcore", "Crust Punk")
        put("stenchcore", "Crust Punk")
        put("neocrust", "Crust Punk")
        put("d beat crust punk", "Crust Punk")
        // D-Beat
        put("d-beat", "D-Beat")
        put("d beat", "D-Beat")
        put("dbeat", "D-Beat")
        put("raw punk", "D-Beat")
        put("käng", "D-Beat")

        // Hardcore Punk
        put("hardcore punk", "Hardcore Punk")
        put("hardcore", "Hardcore")
        put("hc", "Hardcore Punk")
        put("beatdown", "Hardcore Punk")
        put("beatdown hardcore", "Hardcore Punk")
        put("youth crew", "Hardcore Punk")
        put("uk punk", "Hardcore Punk")

        // Powerviolence
        put("powerviolence", "Powerviolence")
        put("pv", "Powerviolence")
        put("power violence", "Powerviolence")
        put("fastcore", "Powerviolence")

        // Punk Rock
        put("punk rock", "Punk Rock")
        put("punk", "Punk Rock")
        put("pop punk", "Punk Rock")
        put("oi", "Punk Rock")
        put("oi!", "Punk Rock")

        // Post Punk
        put("post-punk", "Post Punk")
        put("post punk", "Post Punk")
        put("darkwave", "Darkwave")
        put("coldwave", "Post Punk")
        put("goth rock", "Post Punk")
        put("gothic rock", "Post Punk")
        put("goth", "Post Punk")
        put("deathrock", "Post Punk")

        // Skate Punk
        put("skate punk", "Skate Punk")
        put("skatepunk", "Skate Punk")
        put("melodic punk", "Skate Punk")

        // Street Punk
        put("street punk", "Street Punk")
        put("streetpunk", "Street Punk")

        // Alternative Rock
        put("alternative rock", "Alternative Rock")
        put("alternative", "Alternative Rock")
        put("alt-rock", "Alternative Rock")
        put("alt rock", "Alternative Rock")
        put("indie", "Alternative Rock")

        // Blues Rock
        put("blues rock", "Blues Rock")

        // Classic Rock
        put("classic rock", "Classic Rock")
        put("rock", "Classic Rock")
        put("rock & roll", "Classic Rock")
        put("rock and roll", "Classic Rock")

        // Garage Rock
        put("garage rock", "Garage Rock")
        put("garage", "Garage Rock")
        put("garage punk", "Garage Rock")
        put("lo-fi garage", "Garage Rock")

        // Grunge
        put("grunge", "Grunge")

        // Hard Rock
        put("hard rock", "Hard Rock")

        // Indie Rock
        put("indie rock", "Indie Rock")
        put("indie pop", "Indie Rock")
        put("lo-fi", "Indie Rock")
        put("lofi", "Indie Rock")

        // Math Rock
        put("math rock", "Math Rock")
        put("mathrock", "Math Rock")

        // Noise Rock
        put("noise rock", "Noise Rock")
        put("no wave", "Noise Rock")

        // Post Rock
        put("post-rock", "Post Rock")
        put("post rock", "Post Rock")

        // Progressive Rock
        put("progressive rock", "Progressive Rock")
        put("prog rock", "Progressive Rock")
        put("prog", "Progressive Rock")
        put("art rock", "Progressive Rock")

        // Psychedelic Rock
        put("psychedelic rock", "Psychedelic Rock")
        put("psychedelic", "Psychedelic Rock")
        put("psych rock", "Psychedelic Rock")
        put("acid rock", "Psychedelic Rock")
        put("space rock", "Psychedelic Rock")

        // Shoegaze
        put("shoegaze", "Shoegaze")
        put("shoe gaze", "Shoegaze")
        put("dream pop", "Shoegaze")

        // Surf Rock
        put("surf rock", "Surf Rock")
        put("surf", "Surf Rock")
        put("surf punk", "Surf Rock")

        // Ambient
        put("ambient", "Ambient")
        put("dark ambient", "Ambient")
        put("drone ambient", "Ambient")

        // Breakcore
        put("breakcore", "Breakcore")

        // Darkwave
        put("dark wave", "Darkwave")
        put("synth goth", "Darkwave")

        // Drum and Bass
        put("drum and bass", "Drum and Bass")
        put("dnb", "Drum and Bass")
        put("drum & bass", "Drum and Bass")
        put("jungle", "Drum and Bass")

        // EBM
        put("ebm", "EBM")
        put("electronic body music", "EBM")
        put("aggrotech", "EBM")
        put("dark electro", "EBM")

        // Electro
        put("electro", "Electro")
        put("electronic", "Electro")
        put("electronica", "Electro")
        put("edm", "Electro")

        // IDM
        put("idm", "IDM")
        put("intelligent dance music", "IDM")
        put("glitch", "IDM")

        // Industrial
        put("industrial", "Industrial")
        put("power electronics", "Industrial")
        put("power noise", "Industrial")
        put("harsh noise", "Noise")

        // Noise
        put("noise", "Noise")
        put("harsh noise wall", "Noise")
        put("hnw", "Noise")
        put("japanoise", "Noise")

        // Synthwave
        put("synthwave", "Synthwave")
        put("retrowave", "Synthwave")
        put("outrun", "Synthwave")
        put("synth", "Synthwave")

        // Techno
        put("techno", "Techno")
        put("industrial techno", "Techno")
        put("hard techno", "Techno")
        put("gabber", "Techno")

        // Vaporwave
        put("vaporwave", "Vaporwave")

        //Acid Core
        put("acid core", "Acid Core")
        put("acidcore", "Acid Core")
        put("acid-core", "Acid Core")

        // Hip-Hop
        put("hip-hop", "Hip-Hop")
        put("hip hop", "Hip-Hop")
        put("rap", "Hip-Hop")
        put("hiphop", "Hip-Hop")

        // Boom Bap
        put("boom bap", "Boom Bap")
        put("boombap", "Boom Bap")

        // Conscious Hip-Hop
        put("conscious hip-hop", "Conscious Hip-Hop")
        put("conscious rap", "Conscious Hip-Hop")
        put("political hip-hop", "Conscious Hip-Hop")

        // Gangsta Rap
        put("gangsta rap", "Gangsta Rap")
        put("gangster rap", "Gangsta Rap")
        put("g-funk", "Gangsta Rap")

        // Trap
        put("trap", "Trap")
        put("trap metal", "Trap")

        // Blues
        put("blues", "Blues")
        put("delta blues", "Blues")
        put("electric blues", "Blues")

        // Country
        put("country", "Country")
        put("americana", "Country")
        put("alt-country", "Country")
        put("outlaw country", "Country")

        // Dark Folk
        put("dark folk", "Dark Folk")
        put("apocalyptic folk", "Dark Folk")

        // Experimental
        put("experimental", "Experimental")
        put("avant-garde", "Experimental")
        put("avant garde", "Experimental")

        // Folk
        put("folk", "Folk")
        put("acoustic", "Folk")
        put("folk rock", "Folk")

        // Jazz
        put("jazz", "Jazz")
        put("free jazz", "Jazz")
        put("jazz fusion", "Jazz")
        put("fusion", "Jazz")

        // Neofolk
        put("neofolk", "Neofolk")
        put("neo-folk", "Neofolk")
        put("martial industrial", "Neofolk")

        // Reggae
        put("reggae", "Reggae")
        put("dub", "Reggae")
        put("roots reggae", "Reggae")

        // Singer-Songwriter
        put("singer-songwriter", "Singer-Songwriter")
        put("singer songwriter", "Singer-Songwriter")
        put("songwriter", "Singer-Songwriter")

        // Ska
        put("ska", "Ska")
        put("ska punk", "Ska")
        put("ska-punk", "Ska")
        put("2 tone", "Ska")
        put("two tone", "Ska")

        // Soul
        put("soul", "Soul")
        put("r&b", "Soul")
        put("rnb", "Soul")
        put("funk", "Soul")
        put("neo-soul", "Soul")

        // Soundtrack
        put("soundtrack", "Soundtrack")
        put("ost", "Soundtrack")
        put("score", "Soundtrack")
        put("film score", "Soundtrack")

        // Spoken Word
        put("spoken word", "Spoken Word")
        put("poetry", "Spoken Word")
        put("spoken", "Spoken Word")

        // World Music
        put("world music", "World Music")
        put("world", "World Music")
        put("ethnic", "World Music")
        put("tribal", "World Music")
    }

    /**
     * Single lookup table: every valid genre maps to itself (lowercase),
     * plus all aliases. Built once. This guarantees no valid genre is ever
     * "unreachable" and that aliases can only point to real genres.
     */
    private val canonicalLookup: Map<String, String> by lazy {
        buildMap {
            //  Every valid genre maps to itself
            validGenres.forEach { put(it.lowercase(), it) }
            // Aliases
            aliasMap.forEach { (alias, canonical) -> put(alias, canonical) }
        }
    }

    /**
     * Sanity check: warns at startup if any alias points to a genre that
     * isn't in validGenres
     */
    init {
        val invalid = aliasMap.values.toSet() - validGenres
        if (invalid.isNotEmpty()) {
            android.util.Log.w("GenreRepository", "Aliases point to undefined genres: $invalid")
        }
    }

    fun normalize(genre: String): String? {
        val trimmed = genre.trim().lowercase()
        if (trimmed.isBlank()) return null
        return canonicalLookup[trimmed]
    }


    /**
     * Normalize a list of genres. Removes duplicates and unrecognized genres.
     * E.g. ["grind", "Grindcore", "death metal"] -> ["Grindcore", "Death Metal"]
     */
    fun normalizeAll(genres: List<String>): List<String> {
        return genres
            .mapNotNull { normalize(it) }
            .distinct()
    }

    /**
     * Get all valid genre names (for UI display).
     */
    fun getAllGenres(): List<String> {
        return validGenres.sorted()
    }

    /**
     * Get genres grouped by category (for UI sections).
     */
    fun getGenresByCategory(): Map<String, List<String>> {
        return mapOf(
            "Metal" to listOf(
                "Black Metal", "Death Metal", "Doom Metal", "Drone Metal",
                "Folk Metal", "Gothic Metal", "Heavy Metal", "Industrial Metal",
                "Mathcore", "Melodic Death Metal", "Metalcore",
                "Noise Metal", "Nu Metal", "Post Metal", "Power Metal",
                "Progressive Metal", "Sludge Metal", "Speed Metal", "Stoner Metal",
                "Symphonic Metal", "Technical Death Metal", "Thrash Metal", "War Metal"
            ),
            "Punk" to listOf(
                "Crust Punk", "D-Beat", "Hardcore Punk", "Post Punk",
                "Powerviolence", "Punk Rock", "Skate Punk", "Street Punk",
                "Deutschpunk", "Synth Punk",
                // Grind/Gore:
                "Grindcore", "Goregrind", "Deathgrind", "Cybergrind",
                "Pornogrind", "Noisegrind", "Grindgore", "Mincecore", "Mincegore"
            ),

            "Rock" to listOf(
                "Alternative Rock", "Blues Rock", "Classic Rock", "Garage Rock",
                "Grunge", "Hard Rock", "Indie Rock", "Math Rock", "Noise Rock",
                "Post Rock", "Progressive Rock", "Psychedelic Rock", "Shoegaze",
                "Stoner Rock", "Surf Rock"
            ),
            "Electronic" to listOf(
                "Ambient", "Breakcore", "Darkwave", "Drum and Bass", "EBM",
                "Electro", "IDM", "Industrial", "Synthwave", "Techno", "Vaporwave",
                "Acid Core"
            ),
            "Hip-Hop" to listOf(
                "Boom Bap", "Conscious Hip-Hop", "Gangsta Rap", "Hip-Hop", "Trap"
            ),
            "Other" to listOf(
                "Blues", "Country", "Dark Folk", "Experimental", "Folk", "Jazz",
                "Neofolk", "Noise", "Reggae", "Singer-Songwriter", "Ska", "Soul",
                "Soundtrack", "Spoken Word", "World Music"
            )
        )
    }

    /**
     * Check if a genre string maps to any valid genre.
     */
    fun isValid(genre: String): Boolean {
        return normalize(genre) != null
    }
}

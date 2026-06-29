package ch.skit.pissgrind.data.model

enum class SortOrder(val key: String) {
    ALPHABETICAL("alphabeticalByName"),
    NEWEST("newest"),
    RECENT("recent"),
    FREQUENT("frequent")
}
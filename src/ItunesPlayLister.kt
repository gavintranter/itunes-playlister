import java.io.File

private sealed class Element(val value: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Element

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString() = value

    class Id(value: String) : Element(value)

    class Artist(value: String) : Element(value)

    class Name(value: String) : Element(value)

    class Other(value: String) : Element(value)

    companion object {
        operator fun invoke(value: String): Element {
            val element = value.trim()
            return when {
                element.startsWith(KeyType.ID.key) -> Element.Id(extractStringValue(value))
                element.startsWith(KeyType.ARTIST.key) -> Element.Artist(extractStringValue(value))
                element.startsWith(KeyType.NAME.key) -> Element.Name(extractStringValue(value))
                else -> Element.Other("")
            }
        }

        private val elementValueRegex = ".*<(integer|string)>(.+?)</(integer|string)>".toRegex()

        private fun extractStringValue(it: String) = it.replace(elementValueRegex, "$2").replace("&#38;", "&")
    }
}

private data class Track(val id: Element.Id, val artist: Element.Artist, val name: Element.Name) {
    override fun toString(): String = "$artist - $name"
}

private data class Playlist(val name: String, val tracks: List<Track>) {
    override fun toString() : String = "\n\n==========\n$name:\n" + tracks.joinToString("\n")
}

private enum class KeyType(val key: String) {
    ID("<key>Track ID</key>"),
    ARTIST("<key>Artist</key>"),
    NAME("<key>Name</key>"),
    OTHER("");

    companion object {
        fun blah(value: Element) : KeyType = when (value) {
            is Element.Id -> ID
            is Element.Artist -> ARTIST
            is Element.Name -> NAME
            is Element.Other -> OTHER
        }
    }
}

fun main(args: Array<String>) {
    val files = File("/users/Gavin/Documents/playlists").listFiles().filter { it.extension.equals("xml", true) }

    files.forEach { println(createPlaylist(it.readLines())) }
}

private fun createPlaylist(lines: List<String>): Playlist = Playlist(getPlaylistName(lines), getTracks(lines))

private fun getPlaylistName(lines: List<String>): String = lines.last { it.contains(KeyType.NAME.key) }.let { Element(it) }.toString()

private fun getTracks(lines: List<String>): List<Track> {
    val data = lines.map { Element(it) }
            .filter { it !is Element.Other }
            .groupBy { KeyType.blah(it) }

    val ids = data.getOrElse(KeyType.ID, { throw IllegalStateException("No Id list") })
    val entries = data.getOrElse(KeyType.ARTIST, { throw IllegalStateException("No Artist list") })
            .zip(data.getOrElse(KeyType.NAME, { throw IllegalStateException("No Name list") })) { it, other -> Pair(it as Element.Artist, other  as Element.Name) }
            .zip(ids) { it, other -> Track(other as Element.Id, it.first, it.second) }
            .associateBy { it.id }

    return mapIdsToTracks(ids, entries)
}

private fun mapIdsToTracks(ids: List<Element>, trackEntries: Map<Element.Id, Track>) =
        ids.drop(trackEntries.count()).map { trackEntries.getOrElse(it as Element.Id, { Track(it, Element.Artist("Unknown"), Element.Name("Unknown")) }) }
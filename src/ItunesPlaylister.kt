import Element.*
import java.io.File
import kotlin.reflect.KClass

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

    companion object {
        private val elementValueRegex = ".*<(integer|string)>(.+?)</(integer|string)>".toRegex()

        private fun extractStringValue(it: String) = it.replace(elementValueRegex, "$2").replace("&#38;", "&")

        private enum class KeyType(val key: String) {
            ID("<key>Track ID</key>"),
            ARTIST("<key>Artist</key>"),
            NAME("<key>Name</key>")
        }

        operator fun invoke(value: String): Element? {
            return when {
                KeyType.ID.key in value -> Id(extractStringValue(value))
                KeyType.ARTIST.key in value -> Artist(extractStringValue(value))
                KeyType.NAME.key in value -> Name(extractStringValue(value))
                else -> null
            }
        }
    }

    class Id(value: String = "Unknown") : Element(value)
    class Artist(value: String = "Unknown") : Element(value)
    class Name(value: String = "Unknown") : Element(value)
}

private data class Track(val id: Id = Id(), val artist: Artist = Artist(), val name: Name = Name()) {
    override fun toString(): String = "$artist - $name"
}

private data class Playlist(val name: Name, val tracks: List<Track>) {
    override fun toString() : String = "\n\n==========\n$name:\n${tracks.joinToString("\n")}"
}

fun main(args: Array<String>) {
    val files = File("/users/Gavin/Documents/playlists").listFiles().filter { it.extension.equals("xml", true) }

    files.forEach { println(createPlaylist(it.readLines())) }
}

private fun createPlaylist(lines: List<String>): Playlist {
    val data: Map<KClass<out Element>, List<Element>> = lines.mapNotNull { Element(it) }
            .groupBy { it.javaClass.kotlin }

    return with(data) {
        val ids = getValue(Id::class)
        val names = getValue(Name::class)
        val entries = getValue(Artist::class)
                .zip(names) { artist, name -> Pair(artist as Artist, name  as Name) }
                .zip(ids) { (artist, name), id -> Track(id as Id, artist, name) }
                .sortedWith(compareBy {ids.indexOf(it.id)})

        Playlist(names.last() as Name, entries)
    }
}
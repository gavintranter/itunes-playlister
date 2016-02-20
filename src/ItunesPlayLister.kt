import java.io.File
import kotlin.reflect.KClass

private interface Element

private data class Id(val value: String) : Element {
    override fun toString() : String {
        return value;
    }
}

private data class Artist(val value: String) : Element {
    override fun toString() : String {
        return value;
    }
}

private data class Name(val value: String) : Element {
    override fun toString() : String {
        return value;
    }
}

private data class Track(val id: Id, val artist: Artist, val name: Name) {
    override fun toString(): String {
        return "$artist - $name"
    }
}

private data class Playlist(val name: String, val tracks: List<Track>) {
    override fun toString() : String {
        val string = "\n\n==========\n$name:\n"
        return string + tracks.joinToString("\n")
    }
}

private enum class KeyType(val key: String) {
    ID("<key>Track ID</key>"),
    ARTIST("<key>Artist</key>"),
    TRACK("<key>Name</key>")
}

private val elementValueRegex = ".*<(integer|string)>(.+?)</(integer|string)>".toRegex()

fun main(args: Array<String>) {
    val files = File("/users/Gavin/Documents/playlists").listFiles().filter { it.extension.equals("xml", true) }

    files.forEach { println(createPlaylist(it.readLines()))}
}

private fun createPlaylist(lines: List<String>): Playlist {
    val name = lines.last { it.contains(KeyType.TRACK.key) }.let { extractStringValue(it) }

    return Playlist(name, getTracks(lines))
}

private fun getTracks(lines: List<String>): List<Track> {
    val data: Map<in KClass<Element>, List<Element>> = lines.map { it.trim() }
            .filter { it.startsWith(KeyType.ID.key) || it.startsWith(KeyType.ARTIST.key) || it.startsWith(KeyType.TRACK.key) }
            .map { it.replace("&#38;", "&") }
            .map { extractElementValue(it) }
            .groupBy { it.javaClass.kotlin }

    val entries: Map<Id, Track> = data[Artist::class]!!.zip(data[Name::class]!!, { it, other -> Pair(it, other)})
            .zip(data[Id::class]!!, { it, other -> Track(other as Id, it.first as Artist, it.second as Name)})
            .associateBy { it.id }

    @Suppress("UNCHECKED_CAST") // we know its a List<Id> and given the above didn't NPE it will be safe here
    return mapIdsToTracks(data[Id::class]!!.drop(entries.count()) as List<Id>, entries)
}

private fun extractElementValue(value: String): Element {
    return if (value.startsWith(KeyType.ID.key)) {
        Id(extractStringValue(value))
    }
    else if (value.startsWith(KeyType.ARTIST.key)) {
        Artist(extractStringValue(value))
    }
    else {  // if its not and ID or ARTIST it most be a name.
        Name(extractStringValue(value))
    }
}

private fun extractStringValue(it: String) = it.replace(elementValueRegex, "$2")

private fun mapIdsToTracks(ids: List<Id>, trackEntries: Map<Id, Track>) =
        ids.map { trackEntries.getOrElse(it, { Track(it, Artist("Unknown"), Name("Unknown")) }) }
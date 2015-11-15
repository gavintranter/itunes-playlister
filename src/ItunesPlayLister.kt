import java.io.File

private data class Track(val id: Id, val artist: Artist, val name: Name) {
    override fun toString(): String {
        return "$artist - $name"
    }
}

private data class Playlist(val name: String, val tracks: List<Track>) {
    override fun toString() : String {
        val string = "$name:\n"
        return string + tracks.joinToString("\n")
    }
}

private interface ElementPart

private data class Id(val value: String) : ElementPart {
    override fun toString() : String {
        return value;
    }
}

private data class Artist(val value: String) : ElementPart {
    override fun toString() : String {
        return value;
    }
}

private data class Name(val value: String) : ElementPart {
    override fun toString() : String {
        return value;
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

    files.forEach { println("\n\n==========\n" + createPlaylist(it.readLines()))}
}

private fun createPlaylist(lines: List<String>): Playlist {
    val name = lines.last { it.contains(KeyType.TRACK.key) }.let { extractStringValue(it) }

    return Playlist(name, getTracks(lines))
}

private fun getTracks(lines: List<String>): List<Track> {
    val data = lines.map { it.trim() }
            .filter { it.startsWith(KeyType.ID.key) || it.startsWith(KeyType.ARTIST.key) || it.startsWith(KeyType.TRACK.key) }
            .map { it.replace("&#38;", "&") }
            .map { extractElementValue(it) }
            .groupBy { it.javaClass.kotlin }

    val entries = data.getRaw(Artist::class)!!.zip(data.getRaw(Name::class)!!, { it, other -> Pair(it, other)})
            .zip(data.getRaw(Id::class)!!, {it, other -> Track(other as Id, it.first as Artist, it.second as Name)})
            .toMapBy { it.id }

    return mapIdsToTracks(data.getRaw(Id::class) as List<Id>?, entries)
}

private fun extractElementValue(value: String): ElementPart {
    return if (value.startsWith(KeyType.ID.key)) {
        Id(extractStringValue(value))
    }
    else if (value.startsWith(KeyType.ARTIST.key)) {
        Artist(extractStringValue(value))
    }
    else if (value.startsWith(KeyType.TRACK.key)) {
        Name(extractStringValue(value))
    }
    else {
        throw IllegalArgumentException("$value not an expected entry")
    }
}

private fun extractStringValue(it: String) = it.replace(elementValueRegex, "$2")

private fun mapIdsToTracks(ids: List<Id>?, trackEntries: Map<Id, Track>) =
        ids?.drop(trackEntries.count())
                ?.map { trackEntries.getOrElse(it, { Track(it, Artist("Unknown"), Name("Unknown")) }) }
                ?: throw IllegalStateException("Unable to map ids to Tracks")
import java.io.File

private data class Track(val id: String, val artist: String, val name: String) {
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

private enum class KeyType(val key: String) {
    ARTIST("<key>Artist</key>"),
    TRACK("<key>Name</key>"),
    ID("<key>Track ID</key>");
}

fun main(args: Array<String>) {

    val files = File("/users/Gavin/Documents/playlists").listFiles().filter { it.extension.equals("xml", true) }
    files.forEach { println("\n\n==========\n" + createPlaylist(it.readLines()))}
}

private fun createPlaylist(lines: List<String>): Playlist {
//    todo determine if it is null or not beofre passing to replaceXmlWithStringValue
    val name = lines.lastOrNull { it.contains(KeyType.TRACK.key) }.let { replaceXmlWithStringValue(it, "Not a playlist") }

    return Playlist(name, getTracks(lines))
}

private fun getTracks(lines: List<String>): List<Track> {
    val data = lines.map { it.trim() }
            .filter { it.startsWith(KeyType.ID.key) || it.startsWith(KeyType.ARTIST.key) || it.startsWith(KeyType.TRACK.key) }
            .map { it.replace("&#38;", "&") }
            .groupBy { isOfType(it) }

    val ids = data.get(KeyType.ID)?.map { replaceXmlWithStringValue(it) } ?: throw IllegalStateException("List of Ids is required")
    val tracks = data.get(KeyType.TRACK)?.map { replaceXmlWithStringValue(it) } ?: throw IllegalStateException("List of Tracks is required")
    val artists = data.get(KeyType.ARTIST)?.map { replaceXmlWithStringValue(it) } ?: throw IllegalStateException("List of Artists is required")

    val trackEntries = artists.merge(tracks, { it, other -> Pair(it, other) })
            .merge(ids, { it, other -> Track(other, it.first, it.second) })
            .toMap { it.id }    // the tracks arent in order, we need to "sort" them, this makes it easier, there might be a more functional way

    // Drop ids used to define tracks til we get to the order defining ids then map the tracks to that order
    return ids.drop(trackEntries.count()).map { trackEntries.getOrElse(it, { Track(it, "", "") }) }
}

private fun isOfType(value: String): KeyType {
    return if (value.startsWith(KeyType.ARTIST.key)) {
        KeyType.ARTIST;
    }
    else if (value.startsWith(KeyType.TRACK.key)) {
        KeyType.TRACK;
    }
    else if (value.startsWith(KeyType.ID.key)) {
        KeyType.ID;
    }
    else {
        throw IllegalArgumentException("$value not an expected entry")
    }
}

//todo think of better name
val dataRowValueRegex = ".*<(integer|string)>(.+?)</(integer|string)>".toRegex()

// todo make it non-optional
private fun replaceXmlWithStringValue(it: String?, default: String = "??") = it?.replace(dataRowValueRegex, "$2") ?: default

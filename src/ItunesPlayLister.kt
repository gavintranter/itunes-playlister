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

//todo think of better name
private val dataRowValueRegex = ".*<(integer|string)>(.+?)</(integer|string)>".toRegex()

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
            .groupBy { isOfType(it) }

    val ids = data[KeyType.ID]?.map { extractStringValue(it) } ?: throw IllegalStateException("List of Ids is required")
    val tracks = data[KeyType.TRACK]?.map { extractStringValue(it) } ?: throw IllegalStateException("List of Tracks is required")
    val artists = data[KeyType.ARTIST]?.map { extractStringValue(it) } ?: throw IllegalStateException("List of Artists is required")

    val trackEntries = artists.zip(tracks, { it, other -> Pair(it, other) })
            .zip(ids, { it, other -> Track(other, it.first, it.second) })
            .toMapBy { it.id }

    return mapIdsToTracks(ids, trackEntries)
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

private fun extractStringValue(it: String) = it.replace(dataRowValueRegex, "$2")

private fun mapIdsToTracks(ids: List<String>, trackEntries: Map<String, Track>) =
        ids.drop(trackEntries.count()).map { trackEntries.getOrElse(it, { Track(it, "", "") }) }
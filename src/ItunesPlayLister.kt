import java.io.File
import kotlin.text.Regex

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
    val name = lines.lastOrNull { it.contains(KeyType.TRACK.key) }.let { replaceXmlWithStringValue(it, "Not a playlist") }
    val list = getTracks(lines)

    return Playlist(name, list)
}

private fun getTracks(lines: List<String>): List<Track> {
    val data = lines.map { it.trim() }
            .filter { it.startsWith(KeyType.ID.key) || it.startsWith(KeyType.ARTIST.key) || it.startsWith(KeyType.TRACK.key) }
            .map { it.replace("&#38;", "&") }
            .groupBy { isOfType(it) }

    val ids = data.get(KeyType.ID)?.map { replaceXmlWithIntegerValue(it) }.orEmpty()
    val tracks = data.get(KeyType.TRACK)?.map { replaceXmlWithStringValue(it) }.orEmpty()
    val artists = data.get(KeyType.ARTIST)?.map { replaceXmlWithStringValue(it) }.orEmpty()

    val trackEntries = artists.merge(tracks, { it, other -> Pair(it, other) })
            .merge(ids, { it, other -> Track(other, it.first, it.second) })
            .toMap { it.id }

    return ids.drop(trackEntries.count()).map { trackEntries.getOrElse(it, { Track(it, "", "") }) }
}

private fun isOfType(value: String): KeyType {
    if (value.startsWith(KeyType.ARTIST.key)) {
        return KeyType.ARTIST;
    }
    if (value.startsWith(KeyType.TRACK.key)) {
        return KeyType.TRACK;
    }
    if (value.startsWith(KeyType.ID.key)) {
        return KeyType.ID;
    }

    throw IllegalArgumentException("$value not an expected entry")
}

private fun replaceXmlWithStringValue(it: String?, default: String = "??") = it?.replace(xmlValueCapture("string"), "$1") ?: default

private fun replaceXmlWithIntegerValue(it: String?, default: String = "??") = it?.replace(xmlValueCapture("integer"), "$1") ?: default

private fun xmlValueCapture(valueType: String) = ".*<$valueType>(.+?)</$valueType>".toRegex()
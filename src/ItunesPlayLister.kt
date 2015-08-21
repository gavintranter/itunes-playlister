import java.io.File
import kotlin.text.Regex

private data class Track(val id: String, val artist: String, val name: String) {
    override fun toString(): String {
        return "$artist - $name"
    }
}

private data class Playlist(val name: String, val tracks: List<Track?>) {
    override fun toString() : String {
        val string = "$name:\n"
        return string + tracks.joinToString("\n")
    }
}

private val artistKey = "<key>Artist</key>"
private val nameKey = "<key>Name</key>"
private val trackIdKey = "<key>Track ID</key>"

fun main(args: Array<String>) {

    val files = File("/users/Gavin/Documents/playlists").listFiles().filter { it -> !it.isHidden() }
    files.forEach { println("\n==========\n\n" + createPlaylist(it.readLines()))}
}

private fun createPlaylist(lines: List<String>): Playlist {
    val name = lines.lastOrNull { it.contains(nameKey) }.let { replaceXmlWithStringValue(it, "Not a playlist") }
    val list = getTracks(lines)

    return Playlist(name, list)
}

private fun getTracks(lines: List<String>): List<Track?> {
    val data = lines.map { it.trim() }
            .filter { it.startsWith(trackIdKey) || it.startsWith(artistKey) || it.startsWith(nameKey) }
            .map { it -> it.replace("&#38;", "&") }

    // if there is a better way of doing this I cant see it, this is more readable than putting it in the merges below
    val ids = data.partition { it.startsWith(trackIdKey) }.component1().map { replaceXmlWithIntegerValue(it) }.orEmpty()
    val track = data.partition { it.startsWith(nameKey) }.component1().map { replaceXmlWithStringValue(it) }.orEmpty()
    val artist = data.partition { it.startsWith(artistKey) }.component1().map { replaceXmlWithStringValue(it) }.orEmpty()

    val tracks = artist.merge(track, { it, other -> Pair(it, other) })
            .merge(ids, { it, other -> Track(other, it.first, it.second) })
            .toMap { it.id }

    return ids.drop(tracks.count()).map { tracks.get(it) }
}

private fun replaceXmlWithStringValue(it: String?, default: String = "??") = it?.replace(xmlValueCapture("string"), "$1") ?: default

private fun replaceXmlWithIntegerValue(it: String) = it.replace(xmlValueCapture("integer"), "$1")

private fun xmlValueCapture(valueType: String) = ".*<$valueType>(.+?)</$valueType>".toRegex()
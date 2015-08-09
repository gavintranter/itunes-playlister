import java.io.File
import kotlin.dom.parseXml
import kotlin.text.Regex

private data class Track(val id: String, val artist: String, val name: String) {
    override fun toString(): String {
        return "$artist - $name"
    }
}

private val artistKey = "<key>Artist</key>"
private val nameKey = "<key>Name</key>"
private val trackIdKey = "<key>Track ID</key>"

fun main(args: Array<String>) {
    val lines = File("/users/Gavin/Documents/OxfordGroovyTime.xml").readLines()

    val data = lines.map { it.trim() }
            .filter { it.startsWith(trackIdKey) || it.startsWith(artistKey) || it.startsWith(nameKey) }
            .map { it -> it.replace("&#38;", "&") }

    // if there is a better way of doing this I cant see it, this is more readable than putting it in the merges below
    val ids = data.partition { it.startsWith(trackIdKey) }.component1().map { replaceXmlWithIntegerValue(it) }
    val track = data.partition { it.startsWith(nameKey) }.component1().map { replaceXmlWithStringValue(it) }
    val artist = data.partition { it.startsWith(artistKey) }.component1().map { replaceXmlWithStringValue(it) }

    val tracks = artist.merge(track, { it, other -> Pair(it, other)})
            .merge(ids, { it, other -> Track(other, it.first, it.second) })
            .toMap { it.id }

    ids.drop(tracks.count()).forEach { println(tracks.get(it)) }
}

private fun replaceXmlWithStringValue(it: String) = it.replace(xmlValueCapture("string"), "$1")

private fun replaceXmlWithIntegerValue(it: String) = it.replace(xmlValueCapture("integer"), "$1")

private fun xmlValueCapture(valueType: String) = ".*<$valueType>(.+?)</$valueType>".toRegex()
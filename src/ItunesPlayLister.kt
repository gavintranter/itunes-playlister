import java.io.File
import kotlin.dom.parseXml
import kotlin.text.Regex

data class Track(val id: String, val artist: String, val name: String) {
    override fun toString(): String {
        return "$artist - $name"
    }
}

fun main(args: Array<String>) {
    val lines = File("/users/Gavin/Documents/OxfordGroovyTime.xml").readLines()

    val data = lines.map { it.trim() }
            .filter { it.contains("<key>Artist</key>") || it.contains("<key>Name</key>") || it.contains("<key>Track ID</key>") }
            .map { it -> it.replace("&#38;", "&")}

    // there is probably a better way of doing this but I cant see it, and this is more readable, that putting it in the merges below
    val ids = data.partition { it.startsWith("<key>Track ID</key>") }.component1().map {it.replace(captureInteger(), "$1")}
    val track = data.partition { it.startsWith("<key>Name</key>") }.component1().map {it.replace(captureString(), "$1")}
    val artist = data.partition { it.startsWith("<key>Artist</key>") }.component1().map {it.replace(captureString(), "$1")}

    val tracks = artist.merge(track, { it, other -> Pair(it, other)})
            .merge(ids, { it, other -> Track(other, it.first, it.second) })
            .toMap { it.id }

    ids.drop(tracks.count()).forEach { println(tracks.get(it)) }
}

private fun captureString(): Regex {
    return xmlValueCapture("string");
}

private fun captureInteger(): Regex {
    return xmlValueCapture("integer")
}

private fun xmlValueCapture(valueType: String): Regex {
    return ".*<$valueType>(.+?)</$valueType>".toRegex()
}
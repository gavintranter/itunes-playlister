import java.io.File

fun main(args: Array<String>) {
    val xmlValueCapture = ".*<string>(.+?)</string>"

    val lines = File("/users/Gavin/Documents/OxfordGroovyTime.xml").readLines()

    val track = lines.map { it.trim() }
            .filter { it -> it.contains("<key>Artist</key>") || it.contains("<key>Name</key>") }
            .partition { it.startsWith("<key>Artist</key>") }

    track.first.merge(track.second, { it, other -> it.replace(xmlValueCapture.toRegex(), "$1") +
            " - " + other.replace(xmlValueCapture.toRegex(), "$1") })
            .map { it -> it.replace("&#38;", "&")}
            .forEach { it -> println(it) }
}
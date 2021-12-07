package io.github.kolod

import net.openhft.hashing.LongHashFunction
import java.awt.Color
import java.io.File
import java.io.StringReader
import javax.swing.JEditorPane

/**
 * Get all duplicated files in this directory with a name that matches the regex pattern.
 */
@JvmOverloads
fun File.getDuplicates(regex :Regex? = null) :List<File> =
	if (isDirectory) {
		walk().filter { file ->
			file.isFile && (file.length() > 0) && (regex?.let { it matches file.name } ?: true)
		}.mapNotNull { file ->
			try { file to LongHashFunction.xx3().hashBytes(file.readBytes()) } catch (ex :Exception) { null }
		}.groupBy{ (_, hash) ->
			hash
		}.filter{ (_, values) ->
			values.size > 1
		}.map { (_, values) ->
			values.drop(1)
		}.flatten().map { (file, _) ->
			file
		}.toList()
	} else listOf()

/**
 * Removes all duplicated files in this directory with a name that matches the regex pattern.
 */
@JvmOverloads
fun File.deleteDuplicates(regex :Regex? = null) :List<File> =
	getDuplicates(regex).onEach { file ->
		file.delete()
	}

/**
 * Removes all duplicated files in this directory with a name that matches the regex pattern and all their companions.
 */
fun File.deleteDuplicatesWithCompanions(regex :Regex? = null) :List<File> =
	getDuplicates(regex).map { file ->
		file.getCompanions()
	}.flatten().onEach { file ->
		file.delete()
	}

/**
 * Get all files with the same name and a different extension.
 */
fun File.getCompanions() :List<File> =
	if (isFile) {
		val n = name.split(".").first()
		parentFile.walk().filter { file ->
			file.name.split(".").first() == n
		}.toList()
	} else listOf()

/**
 * Renumber files in the directory.
 */
fun File.renumberWithCompanions(regex :Regex, width :Int = 4, progress :(Int, Int) -> Boolean) {
	if (isDirectory) with (Progress(progress)) {
		val files = walk().filter { file ->
			file.isFile && (file.length() > 0) && (regex matches file.name)
		}.toList()

		if (start(files.size)) {
			files.map { file ->
				if (!next()) return
				file.getCompanions()
			}.onEachIndexed { index, companions ->
				val name = index.toString().padStart(width, '0')
				companions.forEach { oldFile ->
					val extension = oldFile.name.split('.', limit=2).last()
					oldFile.renameTo(File(oldFile.parentFile, "$name.$extension"))
				}
			}
		}
		finish()
	}
}

fun File.renumberWithCompanions(regex :String, width :Int = 4, progress :(Int, Int) -> Boolean) =
	this.renumberWithCompanions(regex.toRegex(), width, progress)


/**
 * Split string into groups by char ranges
 */
fun String.splitByLang() :List<Pair<String, Color>> =
	map { char ->
		char to when (char) {
			in 'a'..'z' -> Color.BLUE.brighter()
			in 'A'..'Z' -> Color.BLUE.brighter()
			in '0'..'9' -> Color.RED
			else -> Color.BLACK
		}
	}.run {
		drop(1).fold(mutableListOf(mutableListOf(first().first) to first().second)) { group, t ->
			group.last().apply{
				if (second == t.second) first.add(t.first)
				else group.add(mutableListOf(t.first) to t.second)
			}
			group
		}.map{
			it.first.joinToString(separator="") to it.second
		}
	}

fun Color.toCSS() = "rgb($red,$green,$blue)"

fun JEditorPane.setTextColoredByLang(str :String) {
	contentType = "text/html; charset=UTF-8"
	val html = "<html><body>" + str.splitByLang().fold(String()) { html, (text, color) ->
		"$html<span style='color:${color.toCSS()}'>$text</span>"
	} + "</body></html>"
	document = editorKit.createDefaultDocument()
	editorKit.read(StringReader(html), document, 0)
}

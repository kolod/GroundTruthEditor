package io.github.kolod

import net.openhft.hashing.LongHashFunction
import java.io.File


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
 * Renumerate files in the directory.
 */
fun File.renumberWithCompanions(regex :Regex, width :Int = 4) {
	if (isDirectory) {
		walk().filter { file ->
			file.isFile && (file.length() > 0) && (regex matches file.name)
		}.map { file ->
			file.getCompanions()
		}.toList().onEachIndexed { index, companions ->
			val name = index.toString().padStart(width, '0')
			companions.forEach { oldFile ->
				val extension = oldFile.name.split('.', limit=2).last()
				oldFile.renameTo(File(oldFile.parentFile, "$name.$extension"))
			}
		}
	}
}


/**
 * Split line to groups by char ranges
 */
fun String.splitByLang() :List<Pair<String, Color>> =
	str.map { char ->
		char to when (char) {
			in 'a'..'z' -> Color.BLUE
			in 'A'..'Z' -> Color.BLUE
			in '0'..'9' -> Color.RED
			else -> Color.BLACK
		}
	}.run{
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

fun JEditorPane.setTextColloredByLang(str :String) {
	document.remove(0, document.length)
	str.splitByLang().forEach{ (text, color) ->
		document.insertString(document.length, text)
	}
}

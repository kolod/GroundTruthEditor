package io.github.kolod

import dumonts.hunspell.Hunspell
import net.openhft.hashing.LongHashFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.*
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

val logger: Logger = LoggerFactory.getLogger(MainWindow::class.java)

private val dictionary = try {
	Hunspell.forDictionaryInResources("ru_RU", "dictionaries/")
} catch (ex :Exception) {
	logger.error(ex.message, ex)
	null
}

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

val File.fileName :String get() = Paths.get(absolutePath).fileName.toString()

/**
 * Renumber files in the directory.
 */
fun File.renumberWithCompanions(regex :Regex, width :Int = 4, progress :((Int, Int) -> Boolean)? = null) {
	if (isDirectory) with (Progress(progress)) {
		val files = walk().filter { file ->
			file.isFile && (file.length() > 0) && (regex matches file.fileName)
		}.toList()

		if (start(files.size)) {
			files.map { file ->
				if (!next()) return
				file.getCompanions()
			}.onEachIndexed { index, companions ->
				val name = index.inc().toString().padStart(width, '0')
				companions.forEach { oldFile ->
					val extension = oldFile.name.split('.', limit=2).last()
					oldFile.renameTo(File(oldFile.parentFile, "$name.$extension"))
				}
			}
		}
		finish()
	}
}

fun File.renumberWithCompanions(regex :String, width :Int = 4, progress :((Int, Int) -> Boolean)? = null) =
	this.renumberWithCompanions(regex.toRegex(), width, progress)


/**
 * Split string into groups by char ranges
 */
fun String.splitByLang() :List<Pair<String, Color>> =
	map { char ->
		char to when (char) {
			in 'a'..'z' -> Color.GREEN.brighter()
			in 'A'..'Z' -> Color.GREEN.brighter()
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
fun Color.toForeground() = "color:${toCSS()}"

private val spacesPattern = Regex("\\s+")
private val punctuationMarksPattern = Regex("[a-zA-Z??-????-??????][%!?;:,\\\\.]+")
private val punctuationMarksPatternAll = Regex("[()%!?;:,\\\\.\"]+")

private val romanNumbers = linkedMapOf(
	1000 to "M", 900 to "CM",
	500 to "D", 400 to "CD",
	100 to "C", 90 to "XC",
	50 to "L", 40 to "XL",
	10 to "X", 9 to "IX",
	5 to "V", 4 to "IV",
	1 to "I"
)

fun toRoman(number: Int): String {
	for (i in romanNumbers.keys) if (number >= i) return romanNumbers[i] + toRoman(number - i)
	return ""
}

private val myDictionary = (1..50).toList().map{ toRoman(it) } + listOf(
	"???", "??????", "????????????????????????", "??????????", "??????", "??-1", "??-2", "??-5", "??/??????", "???1", "???2", "???3", "???4", "???5", "??", "??",
	"????????", "??????????????????", "??????", "OHSAS", "??", "????", "??????????????????????", "????????????????????????", "????????", "??????", "????????????????????",
	"????????????????????", "??????????????????????????????", "??????????????????", "????????", "????????", "????????????????", "??????????????????", "??????/??????", "????????????",
	"????????????????????????????", "????????????????????????????", "3-??", "2-??", "????????????????????????????", "??????????????????", "??????????????????????", "??????",
	"????????????-", "??????????????????????????????????"
)

fun String.addExtraSpace() = replace(punctuationMarksPattern) { it.groupValues[0] + ' ' }.replace("\".", "\". ")
fun String.removeExtraSpaces() = replace(spacesPattern, " ")
fun String.simplify() = trim().addExtraSpace().removeExtraSpaces()
fun String.removePunctuationMarks() = replace(punctuationMarksPatternAll, "")
fun String.toWordPairs() = (1 until length).map { take(it) to drop(it) }
fun String.trySplit() = toWordPairs().firstOrNull { (first, second) ->
	dictionary?.spell(first.removePunctuationMarks()) == true && dictionary.spell(second.removePunctuationMarks())
}

fun String.applyKnownFixes() =
	listOf(
		"\n" to " ",
		"???" to "\"",
		"???" to "\"",
		"x" to "x",
		"\"????????????????????" to "\" ????????????????????",
		"\"????????????????????" to "\" ????????????????????",
		"\".????????????????????" to "\" ????????????????????",
		"\".????????????????????" to "\" ????????????????????",
		"????????5" to "OHSAS",
		"HFIAOTI" to "??????????",
		"HIIAOTI" to "??????????",
		" pasgena " to  " ?????????????? ",
		"rona" to "????????",
		"1!\"" to "1! \"",
		"2!\"" to "2! \"",
		"3!\"" to "3! \"",
		"4!\"" to "4! \"",
		"5!\"" to "5! \"",
		" ??. " to " II. ",
		" ??, " to " II, ",
		" ??." to " III.",
		" ??," to " III,",
		" ????I" to " I ?? II",
		" cr. " to " ????. ",
		" n. " to " ??. ",
		" ?? " to " III ",
		" ????, " to " VIII, ",
		" ???? " to " IV ",
		"??. ??" to "??. II",
		"????. ??" to "????. 8",
		" ??. " to " II. ",
		" 1\\ " to " IV ",
		" ????, " to " VII, ",
		" r. p. " to " ??.??. ",
		" ?? " to " II ",
		" 1\\/ " to " IV ",
		"roga" to "????????",
		"\\'" to "V",
		"??" to "??",
	).fold(this) { result, (oldValue, newValue) ->
		result.replace(oldValue, newValue)
	}

fun File.getAllSpellCheckErrors() = walk().filter { file ->
	file.name.endsWith(".txt")
}.map { file ->
	file.readText().simplify().applyKnownFixes().split(' ').mapNotNull{ string ->
		val word = string.removePunctuationMarks()
		if (word !in myDictionary && dictionary?.spell(word) == false) word else null
	}
}.flatten().toList()

fun String.spellCheck() :String =
	if (dictionary != null) split(" ").joinToString(" ") { string ->
		val word = string.removePunctuationMarks()
		//logger.info(word)
		if (word in myDictionary || dictionary.spell(word)) {
			string
		} else {
			string.trySplit()?.let { (first, second) ->
				"$first $second"
			} ?: run {
				val suggestions = dictionary.suggest(word)
				if (suggestions.isNotEmpty()) {
					"<a href='#' data-suggestions='${suggestions.joinToString(";")}'>$string</a>"
				} else {
					"<a href='#'>$string</a>"
				}
			}
		}
	} else this

fun JEditorPane.setTextChecked(str :String)  {
	contentType = "text/html; charset=UTF-8"
	val html = "<html><body style='font-size: large'>" + str.simplify().applyKnownFixes().spellCheck() + "</body></html>"
	document = editorKit.createDefaultDocument()
	editorKit.read(StringReader(html), document, 0)
}

fun String.setTextColoredByLang(str :String) = str.splitByLang().fold(String()) { html, (text, color) ->
	"$html<span style='${color.toForeground()}'>$text</span>"
}


val JEditorPane.plainTextOrNull
	get() = try {
		val plainKit = getEditorKitForContentType("text/plain")
		val writer = StringWriter()
		plainKit.write(writer, document, 0, document.length)
		writer.toString()
	} catch (ex :Exception) {
		null
	}

val JEditorPane.plainText
	get() = plainTextOrNull ?: ""

fun JEditorPane.setPlainText(str :String) {
	setTextChecked(str)
	document.addDocumentListener(object : DocumentListener {
		override fun insertUpdate(event : DocumentEvent) = setTextChecked(plainText)
		override fun removeUpdate(event : DocumentEvent) = setTextChecked(plainText)
		override fun changedUpdate(event : DocumentEvent) = setTextChecked(plainText)
	})
}

fun <T> MutableListIterator<T>.nextOrPreviousOrNull() =
	when {
		this.hasNext() -> next()
		this.hasPrevious() -> previous()
		else -> null
	}

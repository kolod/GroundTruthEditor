package io.github.kolod

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.awt.Color
import java.io.File

internal class UtilsTest {

	private fun List<File>.toSortedString() :String = map { it.name }.sorted().joinToString(prefix="[", postfix="]")

	@Test
	fun testDeleteDuplicates() {
		val directory = File("./testRemoveDuplicates").apply { deleteRecursively(); mkdirs() }
		for (i in 0..1) File(directory, "1-$i.txt").writeText("test1")
		for (i in 0..2) File(directory, "2-$i.txt").writeText("test2")
		directory.deleteDuplicates(".*\\.txt".toRegex())

		val expected = listOf(
			File(directory, "1-0.txt"),
			File(directory, "2-0.txt")
		)
		val actual = directory.list()?.map { File(directory, it) }
		assertEquals(expected.toSortedString(), actual?.toSortedString())
		directory.deleteRecursively()
	}

	@Test
	fun testGetDuplicates() {
		val directory = File("./testGetDuplicates").apply { deleteRecursively(); mkdirs() }
		File(directory, "1txt").writeText("test1")
		for (i in 0..1) File(directory, "1-$i.txt").writeText("test1")
		for (i in 0..2) File(directory, "2-$i.txt").writeText("test2")
		for (i in 0..2) File(directory, "3-$i.txt").writeText("")
		val expected = listOf(
			File(directory, "1-1.txt"),
			File(directory, "2-2.txt"),
			File(directory, "2-1.txt")
		)
		val actual = directory.getDuplicates(".*\\.txt".toRegex())
		assertEquals(expected.toSortedString(), actual.toSortedString())
		directory.deleteRecursively()
	}

	@Test
	fun testDeleteDuplicatesWithCompanions() {
		val directory = File("./testDeleteDuplicatesWithCompanions").apply { deleteRecursively(); mkdirs() }
		File(directory, "1png").writeText("test1")
		for (i in 0..1) {
			File(directory, "1-$i.png").writeText("test1")
			File(directory, "1-$i.txt").writeText("test1")
		}
		for (i in 0..2) {
			File(directory, "2-$i.png").writeText("test2")
			File(directory, "2-$i.gt.txt").writeText("test2")
		}
		for (i in 0..2) {
			File(directory, "3-$i.png").writeText("")
			File(directory, "3-$i.txt").writeText("")
		}
		val expected = listOf(
			File(directory, "1-1.png"),
			File(directory, "1-1.txt"),
			File(directory, "2-1.png"),
			File(directory, "2-1.gt.txt"),
			File(directory, "2-2.png"),
			File(directory, "2-2.gt.txt"),
		)
		val actual = directory.deleteDuplicatesWithCompanions(".*\\.png".toRegex())
		assertEquals(expected.toSortedString(), actual.toSortedString())
		directory.deleteRecursively()
	}

	@Test
	fun testGetCompanions() {
		val directory = File("./testGetCompanions").apply { deleteRecursively(); mkdirs() }
		val expected = listOf("png", "txt", "gt.txt").map{ extension ->
			File(directory, "test.$extension").apply{ writeText("") }
		}
		val actual = expected.first().getCompanions()
		assertEquals(expected.toSortedString(), actual.toSortedString())
		directory.deleteRecursively()
	}

	@Test
	fun testRenumberWithCompanions() {
		val directory = File("./testRenumberWithCompanions")
		directory.deleteRecursively()
		directory.mkdirs()
		val expected = (1 .. 5).map{ index ->
			val expectedName = index.toString().padStart(2, '0')
			val name = (index*2).toString().padStart(2, '0')
			listOf("txt", "png").map{ extension ->
				File(directory, "$name.$extension").writeText("-")
				File(directory, "$expectedName.$extension")
			}
		}.flatten()
		directory.renumberWithCompanions(""".*\.png""", 2)
		val actual = directory.list()?.map { File(directory, it) }?.toList()
		assertEquals(expected.toSortedString(), actual?.toSortedString())
		directory.deleteRecursively()
	}

	@Test
	fun testToCSS() {
		assertEquals("rgb(0,0,0)", Color.BLACK.toCSS())
		assertEquals("rgb(255,0,0)", Color.RED.toCSS())
		assertEquals("rgb(0,255,0)", Color.GREEN.toCSS())
		assertEquals("rgb(0,0,255)", Color.BLUE.toCSS())
		assertEquals("rgb(255,255,255)", Color.WHITE.toCSS())
	}

	@Test
	fun testToForeground() {
		assertEquals("color:rgb(0,0,0)", Color.BLACK.toForeground())
		assertEquals("color:rgb(255,0,0)", Color.RED.toForeground())
		assertEquals("color:rgb(0,255,0)", Color.GREEN.toForeground())
		assertEquals("color:rgb(0,0,255)", Color.BLUE.toForeground())
		assertEquals("color:rgb(255,255,255)", Color.WHITE.toForeground())
	}

	@Test
	fun testToRoman() {
		assertEquals(""   , toRoman(-1))
		assertEquals(""   , toRoman(0))
		assertEquals("I"  , toRoman(1))
		assertEquals("II" , toRoman(2))
		assertEquals("III", toRoman(3))
		assertEquals("IV" , toRoman(4))
		assertEquals("V"  , toRoman(5))
		assertEquals("VI" , toRoman(6))
	}
}

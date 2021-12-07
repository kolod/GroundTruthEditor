package io.github.kolod

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

internal class UtilsTest {

	private fun compare(expected :List<File>, actual: List<File>?) :Boolean =
		(expected.size == actual?.size) &&
		expected.containsAll(actual) &&
		actual.containsAll(expected)


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
		assertTrue(compare(expected, actual))
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
		assertTrue(compare(expected, actual))
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
		assertTrue(compare(expected, actual))
		directory.deleteRecursively()
	}

	@Test
	fun testGetCompanions() {
		val directory = File("./testGetCompanions").apply { deleteRecursively(); mkdirs() }
		val expected = listOf("png", "txt", "gt.txt").map{ extension ->
			File(directory, "test.$extension").apply{ writeText("") }
		}
		val actual = expected.first().getCompanions()
		assertTrue(compare(expected, actual))
		directory.deleteRecursively()
	}

	@Test
	fun testRenumberWithCompanions() {
		val directory = File("./testRenumberWithCompanions").apply { deleteRecursively(); mkdirs() }
		val expected = (1 .. 5).map{ index ->
			val expectedName = index.toString().padStart(2, '0')
			val name = (index*2).toString().padStart(2, '0')
			listOf("txt", "png").map{ extension ->
				File("$name.$extension").writeText("")
				File("$expectedName.$extension")
			}
		}.flatten()
		directory.renumberWithCompanions((".*\\.png".toRegex())) { _, _ -> true }
		val actual = directory.list()?.map{ File(directory, it) }
		assertTrue(compare(expected, actual))
		directory.deleteRecursively()
	}
}

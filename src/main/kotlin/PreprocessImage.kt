package io.github.kolod

import nu.pattern.OpenCV
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

operator fun Point.times(value :Int) = Point(x * value, y * value)
operator fun Size.times(value :Int) = Size(width * value, height * value)
operator fun Rect.times(value :Int) = Rect(x * value, y * value, width * value, height * value)


private val Rect.bottom get() = y + height
private val Rect.top    get() = y
private val Rect.left   get() = x
private val Rect.right  get() = x + width
private val Rect.square get() = width * height

private fun <T> List<T>.toPair(): Pair<T, T> {
	require (size == 2) { "List is not of length 2! '$this'" }
	return zipWithNext().single()
}

private val List<Boolean>.groups get() =
	zipWithNext().mapIndexedNotNull { i, (previous, next) ->
		i.takeIf { previous != next }
	}.chunked(2).map {
		it.toPair()
	}

private val List<Rect>.groupsVertical : List<Pair<Int, Int>> get() =
	(0 .. maxOf { it.bottom } + 1).map { y ->
		any { y in it.top .. it.bottom }
	}.groups

private fun List<Rect>.merge() :Rect  {
	val left   = minOf { it.left   }
	val right  = maxOf { it.right  }
	val bottom = maxOf { it.bottom }
	val top    = minOf { it.top    }
	return Rect(left, top, right - left, bottom - top)
}

private fun Mat.toImage(): BufferedImage {
	val matOfByte = MatOfByte()
	Imgcodecs.imencode(".png", this, matOfByte)
	return ImageIO.read(ByteArrayInputStream(matOfByte.toArray()))
}

fun preProcessImage(path : File) = preProcessImage(path.absolutePath)

fun preProcessImage(path : String) : BufferedImage {
	logger.info("Load: $path")

	OpenCV.loadShared()

	// Reading the Image from the file and storing it in to a Matrix object
	val inputImage = Imgcodecs.imread(path)

	// Convert the image to gray scale
	val grayImage = Mat()
	Imgproc.cvtColor(inputImage, grayImage, Imgproc.COLOR_BGR2GRAY)

	// Performing OTSU threshold
	val thresholdImage = Mat()
	Imgproc.threshold(grayImage, thresholdImage, 0.0, 255.0, Imgproc.THRESH_OTSU or Imgproc.THRESH_BINARY_INV)

	// Specify structure shape and kernel size.
	// Kernel size increases or decreases the area
	// of the rectangle to be detected.
	// A smaller value like (10, 10) will detect
	// each word instead of a sentence.
	val rectKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(1.0, 1.0))

	// Applying dilation on the threshold image
	val dilationImage = Mat()
	Imgproc.dilate(thresholdImage, dilationImage, rectKernel)

	// Finding contours
	val contours = mutableListOf<MatOfPoint>()
	Imgproc.findContours(dilationImage, contours, inputImage.clone(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE)

	// Looping through the identified contours
	val boxes = contours.map { Imgproc.boundingRect(it) }
	val h = boxes.groupsVertical

	val groupedBoxes = boxes.groupBy { box ->
		h.indexOfFirst { box.bottom in it.first .. it.second }
	}.toSortedMap().map { (_, boxes) ->
		boxes
	}

	val lineBoxes = groupedBoxes.map { it.merge() }.zip(
		groupedBoxes.mapNotNull { groupBoxes ->
			val bottoms = groupBoxes.filter{ it.square >= 20 }.ifEmpty{ groupBoxes }.map{ it.bottom }.toSortedSet()
			bottoms.firstOrNull{ (bottoms.last() - it) <= 3 } ?: bottoms.first()
		})

	// Convert multiline image to single line
	val newWidth = lineBoxes.sumOf { it.first.width } + 5 * lineBoxes.count() + 2
	val heightUpperBase = lineBoxes.maxOf { (box, base_y) -> base_y - box.top }
	val heightLowerBase = lineBoxes.maxOf { (box, base_y) -> box.bottom - base_y }
	val newHeight = heightUpperBase + heightLowerBase + 2
	val newImage = Mat(newHeight, newWidth, inputImage.type(), Scalar(inputImage.get(0,0)))
	var x = 2
	lineBoxes.forEach { (box, base_y) ->
		val rect = Rect(x, heightUpperBase + 1 - base_y + box.top, box.width, box.height)
		inputImage.submat(box).copyTo(newImage.submat(rect))
		x += box.width + 5
	}

	return newImage.toImage()
}

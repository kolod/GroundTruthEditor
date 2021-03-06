package io.github.kolod

class Progress(private val block: ((Int, Int) -> Boolean)? = null) {
	private var time = 0L
	private var total = 0
	private var current = 0

	fun start(value :Int) :Boolean {
		total = value
		time = System.currentTimeMillis()
		return block?.invoke(0, value) ?: true
	}

	fun next() :Boolean {
		current += 1
		if ((System.currentTimeMillis() - time) < 100L) return true
		time = System.currentTimeMillis()
		return block?.invoke(current, total) ?: true
	}

	fun next(value :Int) :Boolean {
		if ((System.currentTimeMillis() - time) < 100L) return true
		time = System.currentTimeMillis()
		current = value
		return block?.invoke(current, total) ?: true
	}

	fun finish() {
		block?.invoke(total, total)
	}
}

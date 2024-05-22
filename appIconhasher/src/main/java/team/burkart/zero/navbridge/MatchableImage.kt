package team.burkart.zero.navbridge

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.abs

class MatchableImage(private var data: IntArray = IntArray(0), private var luminance: Float = 0f) {

	companion object {
		val dimension: Int = 32
		val blockSize: Int = 32
		val blockCount: Int = dimension / blockSize
	}

	constructor(string: String) : this() {
		val parts = string.split("|")
		if (parts.size == 2) {
			luminance = parts[1].toFloat()
		}
		parts[0].split(",").forEach {
			data += it.toInt()
		}
	}

	constructor(drawable: Drawable) : this() {
		val bitmap = drawable.toBitmap(dimension, dimension)
		for (line in 0..< dimension) {
			for (block in 0..< blockCount) {
				var currentBlock: Int = 0
				for (bit in 0..< blockSize) {
					val pixel = Color.valueOf(bitmap.getPixel(block * blockSize + bit, line))
					if (pixel.alpha() > 0f) {
						currentBlock = currentBlock.or(1)
						luminance += pixel.alpha()
					}
					currentBlock = currentBlock.rotateLeft(1)
				}
				data += currentBlock
			}
		}
		luminance /= (dimension * dimension)
	}

	fun match (other: MatchableImage) : Float {
		if (data.size != other.data.size) {return 0f}
		var matchPixels : Int = (dimension * dimension)
		for (i in data.indices) {
			matchPixels -= data[i].xor(other.data[i]).countOneBits()
		}
		return matchPixels.toFloat() / (dimension * dimension) - abs(luminance - other.luminance)
	}

	override fun toString() : String {
		return data.joinToString(separator = ",") + "|${luminance}f"
	}
}
package team.burkart.zero.navbridge

import android.content.Context
import android.graphics.drawable.Drawable
import team.burkart.zero.packet.NavPacket
import java.io.BufferedReader
import java.io.InputStreamReader

class MapsIconMatcher(private val context: Context) {
	data class Matchable(var image: MatchableImage, var maneuver: NavPacket.Companion.Maneuver)
	private val matchables : Array<Matchable> by lazy { loadData() }

	fun getManeuver(drawable: Drawable) : NavPacket.Companion.Maneuver {
		val matchImage = MatchableImage(drawable)
		var maxMatch = 0f
		var maxMatchManeuver = NavPacket.Companion.Maneuver.Undefined
		for (matchable in matchables) {
			val match = matchImage.match(matchable.image)
			if (match > maxMatch) {
				maxMatch = match
				maxMatchManeuver = matchable.maneuver
			}
		}
		return maxMatchManeuver
	}

	private fun loadData() : Array<Matchable> {
		val inputStream = context.resources.openRawResource(R.raw.mapsimagemapping)
		val reader = BufferedReader(InputStreamReader(inputStream))

		var result: Array<Matchable> = arrayOf()
		while (reader.ready()) {
			val parts = reader.readLine().split(";")
			result += Matchable(MatchableImage(parts[0]), NavPacket.Companion.Maneuver.entries[parts[1].toInt()])
		}

		return result
	}
}
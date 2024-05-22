package team.burkart.zero.navbridge.iconhasher

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import team.burkart.zero.navbridge.MatchableImage


class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
	}

	enum class Maneuver(val value: Byte) { // Values from Here SDK
		Undefined(0),
		Straight(1),
		UturnRight(2),
		UturnLeft(3),
		RightLane(4),
		RightLight(5),
		RightMedium(6),
		RightHard(7),
		Middle(8),
		LeftLane(9),
		LeftLight(10),
		LeftMedium(11),
		LeftHard(12),
		HighwayEnterRight(13),
		HighwayEnterLeft(14),
		HighwayLeaveRight(15),
		HighwayLeaveLeft(16),
		HighwayKeepRight(17),
		HighwayKeepLeft(18),
		RoundaboutExit1(19),
		RoundaboutExit2(20),
		RoundaboutExit3(21),
		RoundaboutExit4(22),
		RoundaboutExit5(23),
		RoundaboutExit6(24),
		RoundaboutExit7(25),
		RoundaboutExit8(26),
		RoundaboutExit9(27),
		RoundaboutExit10(28),
		RoundaboutExit11(29),
		RoundaboutExit12(30),
		RoundaboutLeftExit1(31),
		RoundaboutLeftExit2(32),
		RoundaboutLeftExit3(33),
		RoundaboutLeftExit4(34),
		RoundaboutLeftExit5(35),
		RoundaboutLeftExit6(36),
		RoundaboutLeftExit7(37),
		RoundaboutLeftExit8(38),
		RoundaboutLeftExit9(39),
		RoundaboutLeftExit10(40),
		RoundaboutLeftExit11(41),
		RoundaboutLeftExit12(42),
		Start(43), // Icon missing
		Finish(44),
		Ferry(45), // Icon missing
		PassStation(46), // Icon missing
		HeadTo(47), // Icon missing
		ChangeLine(48) // Icon missing
	}

	fun doTest(view: View) {


		var map : Map<Int, Maneuver> = mapOf(
			0 to Maneuver.RightLight, 1 to Maneuver.RoundaboutExit7, 2 to Maneuver.RightHard, 3 to Maneuver.RoundaboutLeftExit6, 4 to Maneuver.RightMedium,
			5 to Maneuver.LeftHard, 6 to Maneuver.RoundaboutLeftExit1, 7 to Maneuver.RightMedium, 8 to Maneuver.RightLane, 9 to Maneuver.RightLane,
			10 to Maneuver.LeftLight, 11 to Maneuver.LeftLight, 12 to Maneuver.LeftMedium, 13 to Maneuver.LeftLane, 14 to Maneuver.RightHard,
			15 to Maneuver.LeftLane, 16 to Maneuver.RightLane, 17 to Maneuver.RightLight, 18 to Maneuver.RoundaboutLeftExit2, 19 to Maneuver.RoundaboutExit8,
			20 to Maneuver.RightMedium, 21 to Maneuver.Straight, 22 to Maneuver.RightLight, 23 to Maneuver.UturnLeft, 24 to Maneuver.UturnLeft,
			25 to Maneuver.Straight, 26 to Maneuver.Straight, 27 to Maneuver.RoundaboutLeftExit5, 28 to Maneuver.RoundaboutExit1, 29 to Maneuver.RightHard,
			30 to Maneuver.LeftMedium, 31 to Maneuver.UturnRight, 32 to Maneuver.LeftLane, 33 to Maneuver.RoundaboutExit5, 34 to Maneuver.UturnRight,
			35 to Maneuver.LeftLane, 36 to Maneuver.RightLight, 37 to Maneuver.UturnRight, 38 to Maneuver.LeftLight, 39 to Maneuver.UturnLeft,
			40 to Maneuver.Straight, 41 to Maneuver.Straight, 42 to Maneuver.RoundaboutExit4, 43 to Maneuver.RoundaboutExit3, 44 to Maneuver.RightHard,
			45 to Maneuver.RoundaboutExit2, 46 to Maneuver.LeftLane, 47 to Maneuver.RightLight, 48 to Maneuver.UturnRight, 49 to Maneuver.RightLane,
			50 to Maneuver.LeftLane, 51 to Maneuver.RightMedium, 52 to Maneuver.Straight, 53 to Maneuver.RightLane, 54 to Maneuver.RoundaboutLeftExit4,
			55 to Maneuver.LeftMedium, 56 to Maneuver.RoundaboutLeftExit8, 57 to Maneuver.RoundaboutLeftExit7, 58 to Maneuver.Finish, 59 to Maneuver.RightHard,
			60 to Maneuver.Straight, 61 to Maneuver.RoundaboutExit6, 62 to Maneuver.UturnRight, 63 to Maneuver.LeftHard, 64 to Maneuver.LeftMedium,
			65 to Maneuver.RoundaboutLeftExit1, 66 to Maneuver.RightMedium, 67 to Maneuver.Straight, 68 to Maneuver.LeftHard, 69 to Maneuver.Finish,
			70 to Maneuver.RightLane, 71 to Maneuver.RightLane
		)

		var result: MutableSet<String> = mutableSetOf()
		for (fi in 0..71) {
			Log.d("progress","Loading ${fi}")
			val res = resources.getIdentifier("a%02d".format(fi), "drawable", packageName)
			val drawable = getDrawable(res)
			if (drawable != null) {
				var i1 = MatchableImage(drawable)
				result += i1.toString() + ";" + map[fi]!!.value
			}
		}
		val sortedResult = result.toTypedArray()
		sortedResult.sort()
		Log.i("result", sortedResult.joinToString("\n"))

	}
}
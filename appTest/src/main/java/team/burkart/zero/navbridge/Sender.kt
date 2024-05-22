package team.burkart.zero.navbridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Class to send navigation data to the Zero Nav Bridge app
 *
 * Usage example:
 * ~~~
 * val sender = Sender()
 * sender.startNavigation()
 * loop {
 * 	val navData = Sender.NavData()
 * 	navData.nextManeuver = Sender.NavData.Maneuver.RightMedium
 * 	navData.nextManeuverDistance = 1200 // meters
 * 	navData.currentName = "Bridge road"
 * 	navData.nextName = "River street"
 * 	navData.destinationDistance = 12300 // meters
 * 	navData.eta = navData.eta.plusMinutes(45) // alternatively set the arrival time as ZonedDateTime object with timeZone UTC
 * 	navData.speedLimit = 90 // speed limit in local units km/h or mph
 * 	sender.sendNavData(navData)
 * }
 * sender.stopNavigation()
 * ~~~
 */
class Sender(private val context: Context) {
	companion object {
		const val targetPackage : String = "team.burkart.zero.navbridge"
	}

	class NavData() {
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
		var nextManeuver: Maneuver = Maneuver.Undefined
		var nextManeuverDistance: Int = 0
		var currentName: String = ""
		var nextName: String = ""
		var destinationDistance: Int = 0
		var eta: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)
		var speedLimit: Byte = 0
	}

	fun startNavigation()  {
		/* For some reason this does not work...
		val intent = Intent()
		intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
		intent.setComponent(ComponentName(targetPackage, "${targetPackage}.BridgeService"))
		intent.setAction("${targetPackage}.UPDATENAV")
		context.startForegroundService(intent)
		*/

		val intent = Intent()
		intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
		intent.setComponent(ComponentName(targetPackage, "${targetPackage}.StartActivity"))
		intent.setAction("${targetPackage}.START")
		context.startActivity(intent)
	}

	fun stopNavigation() {
		val intent = Intent()
		intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
		intent.setComponent(ComponentName(targetPackage, "${targetPackage}.NavReceiver"))
		intent.setAction("${targetPackage}.STOPNAV")
		context.sendBroadcast(intent)
	}

	fun sendNavData(navData: NavData) {
		val intent = Intent()
		intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
		intent.setComponent(ComponentName(targetPackage, "${targetPackage}.NavReceiver"))
		intent.setAction("${targetPackage}.UPDATENAV")

		intent.putExtra("nextManeuver", navData.nextManeuver.value)
		intent.putExtra("nextManeuverDistance", navData.nextManeuverDistance)
		intent.putExtra("currentName", navData.currentName)
		intent.putExtra("nextName", navData.nextName)
		intent.putExtra("destinationDistance", navData.destinationDistance)
		intent.putExtra("eta", navData.eta)
		intent.putExtra("speedLimit", navData.speedLimit)

		context.sendBroadcast(intent)
	}
}
package team.burkart.zero.packet

import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.time.temporal.ChronoField

class NavPacket(private val requestOnly: Boolean = false) : BasePacket() {
	companion object {
		val packetType : Short = 31
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
	}
	override fun getPacketType(): Short {
		return packetType
	}

	override fun getPayload(): ByteArray {
		if (requestOnly) {return ByteArray(0);}
		var result = ByteArray(0)
		result += nextManeuver.value
		result += number(nextManeuverDistance)
		val nextNameEncoded = string(nextName, 54)
		result += nextNameEncoded.sizeBeforePadding.toByte()
		result += nextNameEncoded.data
		val currentNameEncoded = string(currentName, 54)
		result += currentNameEncoded.sizeBeforePadding.toByte()
		result += currentNameEncoded.data
		result += speedLimit
		result += number(destinationDistance-nextManeuverDistance)
		result += number(eta.get(ChronoField.YEAR).toShort())
		result += (eta.get(ChronoField.MONTH_OF_YEAR) + 1).toByte() // why +1 ?
		result += eta.get(ChronoField.DAY_OF_MONTH).toByte()
		result += eta.get(ChronoField.HOUR_OF_DAY).toByte()
		result += eta.get(ChronoField.MINUTE_OF_HOUR).toByte()
		result += eta.get(ChronoField.SECOND_OF_MINUTE).toByte()
		return result
	}

	var nextManeuver: Maneuver = Maneuver.Undefined
	var nextManeuverDistance: Int = 0
	var currentName: String = ""
	var nextName: String = ""
	var destinationDistance: Int = 0
	var eta: ZonedDateTime = ZonedDateTime.now(UTC)
	var speedLimit: Byte = 0

	override fun toString() : String {
		return if (requestOnly) "Request" else
			"${nextManeuver.name} in ${nextManeuverDistance}m from ${currentName} to ${nextName}" +
			"\nDestination in ${destinationDistance}m arriving ${eta}" +
			"\nSpeedlimit: ${speedLimit}"
	}

}
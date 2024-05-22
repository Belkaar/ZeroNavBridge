package team.burkart.zero.packet

import java.util.Date
import java.util.TimeZone

class TimePacket : BasePacket() {

	fun setFromCurrentTime() {
		timezone =	(TimeZone.getDefault().getOffset(Date().time) / 1000 / 60 / 30).toByte()
		time = (System.currentTimeMillis() / 1000L).toInt()
	}
	companion object {
		val packetType : Short = 20
	}
	override fun getPacketType(): Short {
		return packetType
	}
	override fun getPayload(): ByteArray {
		var result = ByteArray(0)
		result += timezone
		result += number(time)
		return result
	}

	var timezone: Byte = 0
	var time: Int = 0
}
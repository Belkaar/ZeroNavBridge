package team.burkart.zero.packet

class LiveDataPacket(private val requestOnly: Boolean = false) : BasePacket() {
	companion object {
		val packetType : Short = 22
		fun convertPosition(rawPosition: Int) : Double {
			return rawPosition.toDouble() * 57.29577951308e-8
		}
	}
	override fun getPacketType(): Short {
		return packetType
	}

	var odo: Int = 0 // * 100 meters
	var consumed: Int = 0 // kWh
	var consumption: Short = 0 // ??
	var speed: Short = 0 // km/h * 100
	var angle: Int = 0 // * degrees * 10, left is negative
	var power: Short = 0 // kW ?
	var torque: Int = 0 // Nm
	var regen: Int = 0 // W?
	var regenBrake: Int = 0 // W?
	var positionLong: Int = 0 // radians * 10^8
	var positionLat: Int = 0 // radians * 10^8
	var batteryLevel: Byte = 0 // %

	constructor(data: ByteArray) : this() {
		if (data.size >= 39) {
			odo = number(data.sliceArray(0..3))
			consumed = number(data.sliceArray(4..7))
			consumption = number(data.sliceArray(8..9)).toShort()
			speed = number(data.sliceArray(10..11)).toShort()
			angle = number(data.sliceArray(12..15))
			power = number(data.sliceArray(16..17)).toShort()
			torque = number(data.sliceArray(18..21))
			regen = number(data.sliceArray(22..25))
			regenBrake = number(data.sliceArray(26..29))
			positionLong = number(data.sliceArray(30..33))
			positionLat = number(data.sliceArray(34..37))
			batteryLevel = data[38]
		}
	}

	override fun toString() : String {
		return if (requestOnly) "Request" else
			"Odo:${odo} Consumed:${consumed} Consumtion:${consumption} Speed:${speed} Angle:${angle}" +
			" Power:${power} Torque:${torque} Regen:${regen} RegenB:${regenBrake} SoC:${batteryLevel}" +
			" Position: ${convertPosition(positionLong)}|${convertPosition(positionLat)}"
	}
}
package team.burkart.zero.packet

class DisplayConfigPacket(private val requestOnly: Boolean = false) : BasePacket() {
	companion object {
		val packetType : Short = 16
		enum class QuadrantItem(val value: Byte) {
			Undefined(0),
			SoC(1),
			Range(2),
			TempMotor(3),
			TempBattery(4),
			Consumption(5),
			ConsumptionAverage(6),
			ConsumptionLifetime(7),
			RPM(8),
			TripA(9),
			TripB(10),
			Disabled(11),
			TempAmbient(12);
			companion object {
				fun fromValue(value: Byte) : QuadrantItem {
					if ((value < Undefined.value) or (value > TempAmbient.value)) {return Undefined;}
					return QuadrantItem.entries[value.toInt()]
				}
			}
		}
	}
	override fun getPacketType(): Short {
		return packetType
	}

	override fun getPayload(): ByteArray {
		var result = ByteArray(0)
		if (!requestOnly) {
			if (time == 0) {
				time = (System.currentTimeMillis() / 1000L).toInt()
			}
			result += quadrant1.value
			result += quadrant2.value
			result += quadrant3.value
			result += quadrant4.value
			result += number(time)

		}
		return result
	}

	var quadrant1: QuadrantItem = QuadrantItem.Undefined
	var quadrant2: QuadrantItem = QuadrantItem.Undefined
	var quadrant3: QuadrantItem = QuadrantItem.Undefined
	var quadrant4: QuadrantItem = QuadrantItem.Undefined
	var time: Int = 0

	constructor(data: ByteArray) : this() {
		if (data.size >= 8) {
			quadrant1 = QuadrantItem.fromValue(data[0])
			quadrant2 = QuadrantItem.fromValue(data[1])
			quadrant3 = QuadrantItem.fromValue(data[2])
			quadrant4 = QuadrantItem.fromValue(data[3])
			time = number(data.sliceArray(4..7))
		}
	}

	override fun toString() : String {
		return if (requestOnly) "Request" else
			"Quadrants: ${quadrant1.name}, ${quadrant2.name}, ${quadrant3.name}, ${quadrant4.name}"
	}
}
package team.burkart.zero.packet

class ModeConfigPacket(private val requestOnly: Boolean = false) : BasePacket() {
	companion object {
		val packetType : Short = 18
		val packetTypeDefaultModes : Short = 13
	}
	override fun getPacketType(): Short {
		return packetType
	}

	class Mode() {
		companion object {
			enum class AbsMode(val value: Byte) {
				Off(0),
				On(1);
				companion object {
					fun fromValue(value: Byte) : AbsMode {
						if ((value < Off.value) or (value > On.value)) {return On;}
						return AbsMode.entries[value.toInt()]
					}
				}
			}
			enum class EtcMode(val value: Byte) {
				Off(0),
				Street(1),
				Sport(2),
				Rain(3),
				OffRoad(4);

				companion object {
					fun fromValue(value: Byte) : EtcMode {
						if ((value < Off.value) or (value > OffRoad.value)) {return Street;}
						return EtcMode.entries[value.toInt()]
					}
				}
			}
			enum class Color(val value: Byte) {
				None(0),
				DarkGreen(1),
				DarkBlue(2),
				LightOrange(3),
				LightBlue(4),
				DarkOrange(5);
				companion object {
					fun fromValue(value: Byte) : Color {
						if ((value < None.value) or (value > DarkOrange.value)) {return None;}
						return Color.entries[value.toInt()]
					}
				}
			}

			const val dataSize = 22
		}
		constructor(data: ByteArray) : this() {
			if (data.size >= dataSize) {
				name = string(data.sliceArray(0..11))
				maxSpeed = number(data.sliceArray(12..13)).toShort()
				maxTorque = data[14]
				regen = data[15]
				regenBrake = data[16]
				maxPower = data[17]
				etcMode = EtcMode.fromValue(data[18])
				absMode = AbsMode.fromValue(data[19])
				color = Color.fromValue(data[20])
				reserved1 = data[21]
			}
		}
		fun toData() : ByteArray {
			var result = ByteArray(0)
			result += string(name, 12).data
			result += number(maxSpeed)
			result += maxTorque
			result += regen
			result += regenBrake
			result += maxPower
			result += etcMode.value
			result += absMode.value
			result += color.value
			result += reserved1
			return result
		}

		var name: String = ""
		var maxSpeed: Short = 0 // mph
		var maxTorque: Byte = 0
		var regen: Byte = 0
		var regenBrake: Byte = 0
		var maxPower: Byte = 0
		var etcMode: EtcMode = EtcMode.Street
		var absMode: AbsMode = AbsMode.On
		var color: Color = Color.None
		var reserved1: Byte = 0 // always 0 ?

		override fun toString() : String {
			return "${name} maxSpeed:${maxSpeed} maxTorque:${maxTorque} regen:${regen}/${regenBrake} maxPower:${maxPower}" +
				" ETC:${etcMode.name} ABS:${absMode.name} color:${color.name} res1:${reserved1}"
		}
	}
	override fun getPayload(): ByteArray {
		var result = ByteArray(0)
		if (requestOnly) { return result; }
		modes.forEach { result += it.toData() }
		result += 0 // what is this?
		return result
	}

	var modes: Array<Mode> = arrayOf()
	var modeOrder: Byte = 0
	var activeMode: Byte = 0

	constructor(data: ByteArray) : this() {
		val modeCount = data.size / Mode.dataSize
		for (i in 0..< modeCount ) {
			modes += Mode(data.sliceArray(i * Mode.dataSize ..< (i+1) * Mode.dataSize))
		}
		if (data.size > modeCount * Mode.dataSize) {
			modeOrder = data[modeCount * Mode.dataSize]
		}
		if (data.size > modeCount * Mode.dataSize + 1) {
			activeMode = data[modeCount * Mode.dataSize + 1]
		}
	}

	override fun toString() : String {
		if (requestOnly) {return "Request"}
		var modeStr = "Active: ${activeMode}"
		modes.forEach { modeStr += it.toString() + "\n" }
		return modeStr
	}
}
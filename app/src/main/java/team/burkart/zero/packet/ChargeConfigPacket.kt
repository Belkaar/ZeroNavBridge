package team.burkart.zero.packet

class ChargeConfigPacket(private val requestOnly: Boolean = false) : BasePacket() {
	companion object {
		val packetType : Short = 27
	}
	override fun getPacketType(): Short {
		return packetType
	}

	override fun getPayload(): ByteArray {
		var result = ByteArray(0)
		if (!requestOnly) {
			result += (if (active) 1 else 0).toByte()
			result += (if (storageMode) 1 else 0).toByte()
			result += target
			result += (if (overCharge) 1 else 0).toByte()
			result += (if (recurringCharge) 1 else 0).toByte()
			result += (if (chargingOverride) 1 else 0).toByte()

		}
		return result
	}

	var active: Boolean = false
	var storageMode: Boolean = false
	var target: Byte = 0
	var overCharge: Boolean = false
	var recurringCharge: Boolean = false
	var chargingOverride: Boolean = false

	constructor(data: ByteArray) : this() {
		if (data.size >= 6) {
			active = data[0] != 0.toByte()
			storageMode = data[1] != 0.toByte()
			target = data[2]
			overCharge = data[3] != 0.toByte()
			recurringCharge = data[4] != 0.toByte()
			chargingOverride = data[5] != 0.toByte()
		}
	}

	override fun toString() : String {
		return if (requestOnly) "Request" else
			"Active: ${active} Storage: ${storageMode} Target:${target}" +
			" overCharge:${overCharge} recurringCharge:${recurringCharge} chargingOverride:${chargingOverride}"
	}

}
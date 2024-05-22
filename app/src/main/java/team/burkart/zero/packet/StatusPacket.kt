package team.burkart.zero.packet

class StatusPacket(private val requestOnly: Boolean = false) : BasePacket() {
	companion object {
		val packetType : Short = 3
	}
	override fun getPacketType(): Short {
		return packetType
	}

	var vin: String = String()
	var manufacturer: String = String()
	var model: String = String()
	var chargerConnected: Boolean = false
	var charging: Boolean = false
	var batteryLevel: Byte = 0 // %
	var ready: Boolean = false
	var chargePower: Short = 0 // watts
	var chargeTime: Short = 0 // minutes

	constructor(data: ByteArray) : this() {
		if (data.size >= 69) {
			vin = string(data.sliceArray(0..17))
			manufacturer = string(data.sliceArray(18..34))
			model = string(data.sliceArray(35..50))
			// 51: always 01 ?
			// 52: always 0a ?
			chargerConnected = data[53] != 0.toByte()
			charging = data[54] != 0.toByte()
			batteryLevel = data[55]
			ready = data[57] != 0.toByte()
			// 58..65 random data ?
			chargePower = (number(data.sliceArray(66..67)) * 100).toShort()
			chargeTime = number(data.sliceArray(68..69)).toShort()
		}
	}

	override fun toString() : String {
		return if (requestOnly) "Request" else
			"Bike: ${manufacturer} ${model} (${vin}) Plug: ${chargerConnected} Charge:${charging}" +
			" SoC:${batteryLevel} Power:${chargePower} ETA:${chargeTime} Ready:${ready}"
	}
}
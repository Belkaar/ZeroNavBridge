package team.burkart.zero.packet

class ProtocolVersionPacket(private val requestOnly: Boolean = false) : BasePacket() {
	companion object {
		val packetType : Short = 1
	}
	override fun getPacketType(): Short {
		return packetType
	}

	override fun getPayload(): ByteArray {
		return ByteArray(0)
		//return byteArrayOf(0x00.toByte(), 0x1D.toByte(), 0x00.toByte(), 0x00.toByte())
	}

	var major: Byte = 0
	var minor: Byte = 0
	var patch: Byte = 0

	constructor(data: ByteArray) : this() {
		if (data.size >= 4) {
			major = data[1]
			minor = data[2]
			patch = data[3]
		}
	}

	override fun toString() : String {
		return if (requestOnly) "Request" else "${major}.${minor}.${patch}"
	}

}
package team.burkart.zero.packet

class AckPacket() : BasePacket() {
	companion object {
		val packetType : Short = 19
	}
	override fun getPacketType(): Short {
		return packetType
	}

	override fun getPayload(): ByteArray {
		return ByteArray(0)
	}
	constructor(data: ByteArray) : this() {
	}
}
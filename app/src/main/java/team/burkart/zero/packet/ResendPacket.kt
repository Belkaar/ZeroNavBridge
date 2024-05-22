package team.burkart.zero.packet

class ResendPacket() : BasePacket() {
	companion object {
		val packetType : Short = 2
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
package team.burkart.zero.packet

import team.burkart.zero.LogUtil
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.zip.CRC32
import kotlin.math.ceil
import kotlin.text.Charsets.UTF_8

open class BasePacket {
	open fun getPacketType() : Short {
		return packetType
	}
	open fun getPayload() : ByteArray {
		return payload
	}

	// encode
	fun compileData() : ByteArray {
		var result = ByteArray(0)
		result += packetMagic

		var payload = getPayload()
		val blocks: Int = ceil(payload.size.toDouble() / 4.0).toInt()
		val padCount: Int = blocks * 4 - payload.size
		payload += ByteArray(padCount)

		result += number(blocks)
		result += number(getPacketType())
		result += number(counter)
		result += payload

		val crc32 = CRC32()
		crc32.update(result)
		result += number(crc32.value.toInt())
		result += packetMagic.reversed()
		//LogUtil.log("Compiled packet tag " + getPacketType() + ": " + result.toHex())
		return result
	}

	// decode
	private var packetType : Short = 0
	private var payload: ByteArray = ByteArray(0)
	fun parseData(data: ByteArray): Int {
		// header + size
		if (data.size < packetMagic.size + 4) {return 0;}
		if (!data.sliceArray(0..3).contentEquals(packetMagic)) {
			LogUtil.log("wrong magic: " + data.sliceArray(0..3).toHex())
			return -1
		}
		val blocks = number(data.sliceArray(4..7))
		val packetSize = packetMagic.size * 2 + 4 + 2 + 2 + blocks * 4 + 4
		if (data.size < packetSize) {return 0;}
		val crc32 = CRC32()
		crc32.update(data.sliceArray(0 ..< packetSize - 8))
		val packetCrc = number(data.sliceArray(12 + blocks*4 .. 15 + blocks*4))
		if (crc32.value.toInt() != packetCrc) {
			LogUtil.log("wrong crc: " + crc32.value + "!=" + packetCrc)
			return -1
		}

		packetType = number(data.sliceArray(8..9)).toShort()
		val packetCounter = number(data.sliceArray(10..11)).toShort()
		payload = data.sliceArray(12..11 + blocks*4)
		//LogUtil.log("Decoded packet tag " + packetType + " (" + packetCounter + "): " + packetSize + " " + payload.toHex())
		return packetSize
	}

	fun toSpecific() : BasePacket {
		var specificPacket = this
		if (packetType == AckPacket.packetType) {
			specificPacket = AckPacket(getPayload())
		}
		if (packetType == ResendPacket.packetType) {
			specificPacket = ResendPacket(getPayload())
		}
		if (packetType == ProtocolVersionPacket.packetType) {
			specificPacket = ProtocolVersionPacket(getPayload())
		}
		if (packetType == StatusPacket.packetType) {
			specificPacket = StatusPacket(getPayload())
		}
		if (packetType == ChargeConfigPacket.packetType) {
			specificPacket = ChargeConfigPacket(getPayload())
		}
		if (packetType == DisplayConfigPacket.packetType) {
			specificPacket = DisplayConfigPacket(getPayload())
		}
		if (packetType == LiveDataPacket.packetType) {
			specificPacket = LiveDataPacket(getPayload())
		}
		if ((packetType == ModeConfigPacket.packetType) or (packetType == ModeConfigPacket.packetTypeDefaultModes)) {
			specificPacket = ModeConfigPacket(getPayload())
		}
		return specificPacket
	}

	override fun toString(): String {
		return payload.toHex()
	}
	// statics
	companion object {
		private val packetMagic = byteArrayOf(0xF1.toByte(), 0xF2.toByte(), 0xF4.toByte(), 0xF8.toByte())
		private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
		private var counter : Short = 0
			get() = 0//field++
		fun number(data: ByteArray) : Int {
			data.reverse()
			val buf = ByteBuffer.wrap(data)
			if (data.size == 4) {
				return buf.getInt()
			}
			if (data.size == 2) {
				return buf.getShort().toInt()
			}
			return 0
		}
		fun number(value: Int) : ByteArray {
			val result = ByteBuffer.allocate(4).putInt(value).array()
			result.reverse()
			return result
		}
		fun number(value: Short) : ByteArray {
			val result = ByteBuffer.allocate(2).putShort(value).array()
			result.reverse()
			return result
		}

		fun string(data: ByteArray) : String {
			var endPos = data.indexOf(0)
			if (endPos < 0) {endPos = data.size - 1;}
			return String(data.sliceArray(0..<endPos), UTF_8)
		}

		data class StringInfo(val data: ByteArray, val sizeBeforePadding: Int)
		fun string(str: String, size: Int) : StringInfo {
			val charset: Charset = UTF_8
			var ba = str.toByteArray(charset)
			val sizeBeforePadding = ba.size
			if (ba.size > size) {
				ba.dropLast(ba.size - size)
			}
			if (ba.size < size) {
				ba += ByteArray(size - ba.size)
			}
			return StringInfo(ba, sizeBeforePadding)
		}

	}
}
package team.burkart.zero.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import team.burkart.zero.LogUtil
import team.burkart.zero.packet.BasePacket
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import kotlin.math.min

class Connection (private val device: BluetoothDevice, private val context: Context){
	companion object {
		val serviceUUID : UUID = UUID.fromString("0000FEFB-0000-1000-8000-00805F9B34FB")
		val charTxUUID : UUID = UUID.fromString("00000001-0000-1000-8000-008025000000")
		val charRxUUID : UUID = UUID.fromString("00000002-0000-1000-8000-008025000000")
		val localCreditsUUID : UUID = UUID.fromString("00000003-0000-1000-8000-008025000000")
		val remoteCreditsUUID : UUID = UUID.fromString("00000004-0000-1000-8000-008025000000")
		val mtuTxUUID : UUID = UUID.fromString("0000000A-0000-1000-8000-008025000000")
		val mtuRxUUID : UUID = UUID.fromString("00000009-0000-1000-8000-008025000000")
		var cccdUUID : UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34fB")
	}

	private var gatt: BluetoothGatt? = null
	private var charTx: BluetoothGattCharacteristic? = null
	private var charRx: BluetoothGattCharacteristic? = null
	private var localCredits: BluetoothGattCharacteristic? = null
	private var remoteCredits: BluetoothGattCharacteristic? = null
	private var mtuTx: BluetoothGattCharacteristic? = null
	private var mtuRx: BluetoothGattCharacteristic? = null

	private var sending: Boolean = false
	private var queue: ByteArray = ByteArray(0)
	private var mtu: Short = 20
	private var remoteCreditsLeft: Int = 0
	private var pendingLocalCredits: Int = 0
	private var inputBuffer : ByteArray = ByteArray(0)

	private lateinit var openedCallback: (connection: Connection?)->Unit
	private var incomingPacketCallback: ((packet: BasePacket)->Unit)? = null

	private var currentConnectStep = 0

	private val timer : Timer = Timer()
	private var stallTimerTask : TimerTask? = null

	@SuppressLint("MissingPermission")
	fun open(callback: (connection: Connection?)->Unit) {
		openedCallback = callback
		currentConnectStep = 0
		device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
	}

	@SuppressLint("MissingPermission")
	fun disconnect() {
		gatt?.close()
		openedCallback.invoke(null)
	}

	fun setPacketCallback(callback: (packet: BasePacket)->Unit) {
		incomingPacketCallback = callback
	}
	fun sendPacket(packet: BasePacket) {
		val data = packet.compileData()
		queueData(data)
	}

	private fun queueData(data: ByteArray) {
		queue += data
		sendData()
	}

	@Suppress("DEPRECATION")
	@SuppressLint("MissingPermission")
	private fun sendData() {
		val localGatt = gatt ?: run {return;}
		if (pendingLocalCredits >= 10) {
			grantLocalCredits(pendingLocalCredits)
			pendingLocalCredits = 0
			return
		}
		val localCharTx = charTx ?:  run {return;}
		var data : ByteArray
		synchronized(queue) {
			if (sending) {
				return
			}
			if (queue.isEmpty() || remoteCreditsLeft == 0) {
				if (remoteCreditsLeft == 0) {
					LogUtil.log("Send aborted. Out of credits!")
				}
				return
			}
			data = ByteArray(min(mtu.toInt(), queue.size))
			queue.copyInto(data, 0, 0, data.size)
			queue = queue.sliceArray(data.size..<queue.size)
			sending = true
			remoteCreditsLeft -= 1
		}
		LogUtil.log("Writing " + data.size + "bytes. credits left: " + remoteCreditsLeft)
		LogUtil.setTimeStamp("DataOut")
		//localGatt.writeCharacteristic(localCharTx, data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
		localCharTx.value = data
		localCharTx.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
		localGatt.writeCharacteristic(localCharTx)
		stallTimerTask?.cancel()
		stallTimerTask = object : TimerTask() {
			override fun run() {
				LogUtil.log("Connection stalled, disconnecting")
				disconnect()
			}
		}
		timer.schedule(stallTimerTask, 5000)
	}

	private fun incomingData(data: ByteArray) {
		stallTimerTask?.cancel()
		synchronized(inputBuffer) {
			inputBuffer += data
			var result = -1
			while (result != 0) {
				val packet = BasePacket()
				result = packet.parseData(inputBuffer)
				if (result < 0) {
					inputBuffer = inputBuffer.sliceArray(1..<inputBuffer.size)
				}
				if (result > 0) {
					inputBuffer = inputBuffer.sliceArray(result..<inputBuffer.size)
					incomingPacket(packet)
				}
			}
		}
	}
	private fun incomingPacket(packet: BasePacket) {
		val specificPacket = packet.toSpecific()
		LogUtil.log("Received " + specificPacket.javaClass.simpleName + ": " + specificPacket)
		incomingPacketCallback?.invoke(specificPacket)
	}

	@Suppress("DEPRECATION")
	@SuppressLint("MissingPermission")
	private fun grantLocalCredits(count: Int) {
		val localGatt = gatt ?: run {return;}
		val localLocalCredits = localCredits ?:  run {return;}
		val data = byteArrayOf((count and 255).toByte())
		LogUtil.log("Granting local credits: " +data.toHex())
		//localGatt.writeCharacteristic(localLocalCredits, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
		localLocalCredits.value = data
		localLocalCredits.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
		localGatt.writeCharacteristic(localLocalCredits)
	}

	@Suppress("DEPRECATION")
	@SuppressLint("MissingPermission")
	private fun subscribeToCharacteristic(characteristic: BluetoothGattCharacteristic) {
		val localGatt = gatt ?: run {return;}
		localGatt.setCharacteristicNotification(characteristic, true)
		val descriptor: BluetoothGattDescriptor = characteristic.getDescriptor(cccdUUID)
		if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
			//result = localGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) == 0
			descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
		} else if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
			//result = localGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == 0
			descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
		}
		val result = localGatt.writeDescriptor(descriptor)
		if (!result) {
			LogUtil.log("Subscribe to " + characteristic.uuid + " failed.")
		}
	}

	@Suppress("DEPRECATION")
	@SuppressLint("MissingPermission")
	private fun nextConnectionStep() : Boolean {
		if (currentConnectStep == -1) {return false;}
		when (++currentConnectStep) {
			1 -> {
				gatt!!.discoverServices()
			}
			2 -> {
				gatt!!.requestMtu(512)
			}
			3 -> {
				subscribeToCharacteristic(mtuTx!!)
			}
			4 -> {
				subscribeToCharacteristic(remoteCredits!!)
			}
			5 -> {
				subscribeToCharacteristic(charRx!!)
			}
			6 -> {
				//LogUtil.log("Sending mtu: $mtu")
				//gatt?.writeCharacteristic(mtuRx!!, BasePacket.number(mtu), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
				mtuRx!!.value = BasePacket.number(mtu)
				mtuRx!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
				gatt?.writeCharacteristic(mtuRx!!)
			}
			7 -> {
				grantLocalCredits(20)
			}
			8 -> {
				currentConnectStep = -1
				openedCallback.invoke(this@Connection)
			}
		}
		return true
	}
	private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

	private val gattCallback = object : BluetoothGattCallback() {
		override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
			nextConnectionStep()
		}

		@SuppressLint("MissingPermission")
		override fun onConnectionStateChange(localGatt: BluetoothGatt, status: Int, newState: Int) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					LogUtil.log("GATT connected, discovering services")
					gatt = localGatt
					nextConnectionStep()
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					LogUtil.log("GATT disconnected")
					disconnect()
				}
			} else {
				LogUtil.log("GATT disconnected: $status")
				disconnect()
			}
		}

		@SuppressLint("MissingPermission")
		override fun onServicesDiscovered(localGatt: BluetoothGatt, status: Int) {
			//LogUtil.log("onServicesDiscovered status: $status")

			if (status == 129 /*GATT_INTERNAL_ERROR*/) {
				// it should be a rare case, this article recommends to disconnect:
				// https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07
				disconnect()
				return
			}

			val service = localGatt.getService(serviceUUID) ?: run {
				disconnect()
				return
			}

			charTx = service.getCharacteristic(charTxUUID)
			charRx = service.getCharacteristic(charRxUUID)
			localCredits = service.getCharacteristic(localCreditsUUID)
			remoteCredits = service.getCharacteristic(remoteCreditsUUID)
			mtuTx = service.getCharacteristic(mtuTxUUID)
			mtuRx = service.getCharacteristic(mtuRxUUID)

			if ((charTx != null) && (charRx != null) && (localCredits != null) && (remoteCredits != null)
				&& (mtuTx != null) && (mtuRx != null)) {
				nextConnectionStep()
			} else {
				LogUtil.log("Service discovery failed")
			}

		}

		override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
			//LogUtil.log("onCharacteristicChanged " + characteristic.uuid + ": " + value.toHex())
			when (characteristic.uuid) {
				mtuTxUUID -> {
					mtu = BasePacket.number(value).toShort()
					LogUtil.log("MTU set to $mtu")
				}
				remoteCreditsUUID -> {
					var newCredits = value[0].toInt()
					if (newCredits < 0) {newCredits += 256}
					synchronized(queue) {
						remoteCreditsLeft += newCredits
					}
					LogUtil.log("Write credits received $newCredits --> $remoteCreditsLeft")
					sendData()
				}
				charRxUUID -> {
					++pendingLocalCredits
					//LogUtil.log("Received data " + value.toHex())
					incomingData(value)
				}
			}
		}

		/*
		override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, data: ByteArray, status: Int) {
			LogUtil.log("onCharacteristicRead " + characteristic.uuid + " " + status + " data: " + data.toHex())
			if (characteristic.uuid == remoteCreditsUUID) {
				// TODO: do something with incoming data
				LogUtil.log("Read " + data.toHex())
			}
		}
		*/

		override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
			//LogUtil.log("onCharacteristicWrite " + characteristic.uuid + " " + status)
			if (characteristic.uuid == mtuRxUUID) {
				nextConnectionStep()
			}
			if (characteristic.uuid == localCreditsUUID) {
				// granting local credits is done during connect
				if (!nextConnectionStep()) {
					sendData()
				}
			}
			if (characteristic.uuid == charTxUUID) {
				LogUtil.setTimeStamp("DataIn")
				if (status == BluetoothGatt.GATT_SUCCESS) {
					//LogUtil.log("Data write successfull")
					timer.schedule(object : TimerTask() {
						override fun run() {
							synchronized(queue) {
								sending = false
							}
							sendData()
						}
					}, 100)
				} else {
					LogUtil.log("Data write failed")
					synchronized(queue) {
						queue = ByteArray(0)
						sending = false
					}
				}
			}
		}

		override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
			nextConnectionStep()
		}
	}

}
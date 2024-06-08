package team.burkart.zero.navbridge

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import team.burkart.zero.LogUtil
import team.burkart.zero.Observable
import team.burkart.zero.ble.Connection
import team.burkart.zero.ble.Scanner
import team.burkart.zero.packet.BasePacket
import team.burkart.zero.packet.NavPacket
import team.burkart.zero.packet.ProtocolVersionPacket
import team.burkart.zero.packet.StatusPacket
import java.time.ZonedDateTime
import java.util.Timer
import java.util.TimerTask


class BridgeService : Service() {
	inner class LocalBinder : Binder() {
		fun getService(): BridgeService = this@BridgeService
	}
	private val binder = LocalBinder()
	override fun onBind(intent: Intent): IBinder {
		return binder
	}

	data class Status (var ready: Boolean = false, var description: String = String())

	private val bluetoothStatusReciever = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
				tryConnectBike()
			}
		}
	}
	override fun onCreate() {
		LogUtil.log("Bridge service start")

		val channel = NotificationChannel("ServiceRunning","Service running",
			NotificationManager.IMPORTANCE_DEFAULT
		)
		channel.description = "Zero Nav Bridge channel for foreground service notification"

		getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
		val notification = NotificationCompat.Builder(this, "ServiceRunning")
			.setContentTitle("Zero Nav Bridge running")
			.build()
		ServiceCompat.startForeground(this, 1, notification,
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
		)

		registerReceiver(bluetoothStatusReciever, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

		keepAlive()
		tryConnectBike()
	}

	override fun onDestroy() {
		LogUtil.log("Bridge service stop")
		closing = true
		bikeConnection?.disconnect()
		unregisterReceiver(bluetoothStatusReciever)
	}

	private fun keepAlive() {
		inactivityTimerTask?.cancel()
		inactivityTimerTask = object : TimerTask() {
			override fun run() {
				stopSelf()
			}
		}
		timer.schedule(inactivityTimerTask, 60000)
	}

	@SuppressLint("MissingPermission")
	private fun tryConnectBike() {
		var message = R.string.status_connecting
		if (settings.bridgeEnabled) {
			if (!scanner.connectBike(::onScanResult)) {
				message = R.string.status_bluetooth_off
			}
		} else {
			message = R.string.status_disabled
		}
		serviceStatus.set(Status(false, getString(message)))
		bikeInfo.set("")
	}

	private fun onScanResult(connection: Connection?) {
		if (closing) {return;}
		bikeConnection = connection
		if (bikeConnection == null) {
			timer.schedule(object : TimerTask() {
				override fun run() {
					tryConnectBike()
				}
			},10000)
		} else {
			serviceStatus.set(Status(true, getString(R.string.status_connected)))
			bikeConnection?.setPacketCallback(::onIncomingPacket)
			bikeConnection?.sendPacket(ProtocolVersionPacket(true))
		}
	}

	private fun onIncomingPacket(packet: BasePacket) {
		LogUtil.setTimeStamp("PacketIn")
		if (packet is ProtocolVersionPacket) {
			if (packet.major < 29) {
				serviceStatus.set(Status(false, getString(R.string.status_firmware_too_old)))
				return
			}
			bikeConnection?.sendPacket(StatusPacket(true))
		}
		if (packet is StatusPacket) {
			bikeInfo.set("${packet.manufacturer} ${packet.model}")
			sendNextPacket()
		}
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent?.action == "team.burkart.zero.navbridge.UPDATENAV") {
			val packet = currentNavPacket.get() ?: NavPacket()
			if (intent.hasExtra("nextManeuver")) {
				val maneuver = intent.getByteExtra("nextManeuver", 0)
				if ((maneuver >= 0) and (maneuver < NavPacket.Companion.Maneuver.entries.size)) {
					packet.nextManeuver = NavPacket.Companion.Maneuver.entries[maneuver.toInt()]
				}
			}
			if (intent.hasExtra("nextManeuverDistance")) {
				packet.nextManeuverDistance = intent.getIntExtra("nextManeuverDistance", 0)
			}
			if (intent.hasExtra("currentName")) {
				packet.currentName = intent.getStringExtra("currentName") ?: ""
			}
			if (intent.hasExtra("nextName")) {
				packet.nextName = intent.getStringExtra("nextName") ?: ""
			}
			if (intent.hasExtra("destinationDistance")) {
				packet.destinationDistance = intent.getIntExtra("destinationDistance", 0)
			}
			if (intent.hasExtra("eta")) {
				@Suppress("DEPRECATION")
				val eta = intent.getSerializableExtra("eta") as ZonedDateTime?
				if (eta != null) {
					packet.eta = eta
				}
			}
			if (intent.hasExtra("speedLimit")) {
				packet.speedLimit = intent.getByteExtra("speedLimit", 0)
			}
			LogUtil.log("intent packet: ${packet}")
			updateNavPacket(packet)
		}
		if (intent?.action == "team.burkart.zero.navbridge.STOPNAV") {
			stopNavigation()
			stopSelf()
		}
		return START_NOT_STICKY
	}

	private fun sendNextPacket() {
		LogUtil.setTimeStamp("SendTrigger")
		sendTimerTask?.cancel()
		sendTimerTask = null
		// Don't resend nav package to bike if showOnlyNearTurns is on and distance is
		// more than 2km. Bike will hide nav screen after ~15 seconds
		if (!suppressSend) {
			val connection = bikeConnection ?: run { return; }
			val packet = currentNavPacket.get() ?: run { return; }
			LogUtil.log("Bridge: Sending ${packet.javaClass.simpleName}: ${packet}")
			LogUtil.setTimeStamp("PacketOut")
			connection.sendPacket(packet)
		} else {
			LogUtil.log("Bridge: Send suppressed")
		}
		sendTimerTask = object : TimerTask() {
			override fun run() {
				sendNextPacket()
			}
		}
		timer.schedule(sendTimerTask , 5000)
	}

	val serviceStatus : Observable<Status> = Observable(Status(false,""))
	val bikeInfo : Observable<String> = Observable("")
	val currentNavPacket: Observable<NavPacket?> = Observable(null)

	fun recheckEnabled() {
		if (settings.bridgeEnabled) {
			if (bikeConnection == null) {
				tryConnectBike()
			}
		} else {
			scanner.abortScan()
			bikeConnection?.disconnect()
			tryConnectBike()
		}
	}
	fun updateNavPacket(packet: NavPacket) {
		LogUtil.setTimeStamp("NavUpdate")
		keepAlive()
		// Only show nav update if directions changed since last if in low distraction mode
		val lastNavPacket = currentNavPacket.get()
		if ((settings.maneuverShowDistance > 0) &&
			lastNavPacket != null &&
			(packet.nextManeuverDistance > settings.maneuverShowDistance) &&
			(packet.nextManeuver == lastNavPacket.nextManeuver) &&
			(packet.nextName == lastNavPacket.nextName)
		) {
			suppressSend = true
			return
		}
		suppressSend = false
		currentNavPacket.set(packet)
		sendNextPacket()
	}

	fun stopNavigation() {
		currentNavPacket.set(null)
	}

	private var closing: Boolean = false
	private val scanner: Scanner = Scanner(this)

	private val settings : Settings by lazy { Settings(this) }

	private var bikeConnection: Connection? = null
	private val timer : Timer = Timer()
	private var inactivityTimerTask : TimerTask? = null
	private var sendTimerTask : TimerTask? = null

	private var suppressSend : Boolean = false

}
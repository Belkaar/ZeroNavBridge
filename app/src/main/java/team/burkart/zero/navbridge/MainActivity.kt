package team.burkart.zero.navbridge

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import team.burkart.zero.LogUtil
import team.burkart.zero.ble.PermissionUtil
import java.util.Timer
import java.util.TimerTask

class MainActivity : ComponentActivity() {
	private var bridgeService: BridgeService? = null

	private val bluetoothOnRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
		doConnect()
	}
	@Suppress("MoveLambdaOutsideParentheses")
	private val connection = object : ServiceConnection {
		override fun onServiceConnected(className: ComponentName, service: IBinder) {
			val binder = service as BridgeService.LocalBinder
			bridgeService = binder.getService()
			bridgeService?.serviceStatus?.observe({ value, oldValue ->
				Handler(Looper.getMainLooper()).post {
					val color = if (value.ready) {R.color.statusGood} else {R.color.statusBad}
					statusText.setTextColor(resources.getColor(color))
					statusText.text = value.description
					if (value.description == getString(R.string.status_bluetooth_off)) {
						bluetoothOnRequest.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
					}
				}
			})
			bridgeService?.bikeInfo?.observe({ value, oldValue ->
				Handler(Looper.getMainLooper()).post {
					findViewById<TextView>(R.id.bikeInfo).text = value
				}
			})
			bridgeService?.currentNavPacket?.observe({ value, oldValue ->
				Handler(Looper.getMainLooper()).post {
					if (value != null) {
						findViewById<TextView>(R.id.currentPacket).text = value.toString()
					}
				}
			})
		}
		override fun onServiceDisconnected(arg0: ComponentName) {
			bridgeService = null
		}
	}
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		initSettings()
		setupDebugInfo()
		doConnect()
	}
	private var showingHelp: Boolean = false

	override fun onStart() {
		super.onStart()
		showingHelp = false
	}

	private val isDebuggable : Boolean by lazy { 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE}

	override fun onStop() {
		if (!showingHelp && !isDebuggable) {
			finish()
		}
		super.onStop()
	}
	private val settings : Settings by lazy { Settings(this) }

	private fun initSettings() {
		findViewById<Switch>(R.id.bridgeEnabled).isChecked = settings.bridgeEnabled
		findViewById<Switch>(R.id.bridgeEnabled).setOnCheckedChangeListener { buttonView, isChecked ->
			settings.bridgeEnabled = isChecked
			bridgeService?.recheckEnabled()
		}

		findViewById<Switch>(R.id.settingOnlyShowNearTurn).isChecked = settings.maneuverShowDistance > 0
		findViewById<Switch>(R.id.settingOnlyShowNearTurn).setOnCheckedChangeListener { buttonView, isChecked -> settings.maneuverShowDistance = if (isChecked) 2000 else 0 }

		findViewById<Switch>(R.id.listenToMapsNotifications).isChecked = settings.listenToMapsNotifications
		if (settings.listenToMapsNotifications) {NavNotificationListener.ensurePermission(this)	}
		findViewById<Switch>(R.id.listenToMapsNotifications).setOnCheckedChangeListener { buttonView, isChecked ->
			settings.listenToMapsNotifications = isChecked
			if (isChecked) {NavNotificationListener.ensurePermission(this)}
		}

		findViewById<Switch>(R.id.listenToOsmAndNotifications).isChecked = settings.listenToOsmAndNotifications
		if (settings.listenToOsmAndNotifications) {NavNotificationListener.ensurePermission(this)	}
		findViewById<Switch>(R.id.listenToOsmAndNotifications).setOnCheckedChangeListener { buttonView, isChecked ->
			settings.listenToOsmAndNotifications = isChecked
			if (isChecked) {NavNotificationListener.ensurePermission(this)}
		}

		findViewById<Switch>(R.id.showDebugInfo).isChecked = settings.showDebugInfo
		findViewById<Switch>(R.id.showDebugInfo).setOnCheckedChangeListener { buttonView, isChecked ->
			settings.showDebugInfo = isChecked
			setupDebugInfo()
		}
	}

	override fun onDestroy() {
		LogUtil.setLogView(null)
		if (bridgeService != null) {
			bridgeService?.stopNavigation()
			unbindService(connection)
		}
		super.onDestroy()
	}
	@Deprecated("Deprecated in Java")
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		doConnect()
	}
	fun doConnect(view: View) {
		doConnect()
	}
	private fun doConnect() {
		val permissionUtil = PermissionUtil(this)
		if (!permissionUtil.ensurePermissions()) {
			findViewById<TextView>(R.id.status).text = getString(R.string.status_missing_permissions)
			return
		}

		bindService(Intent(this, BridgeService::class.java), connection, Context.BIND_AUTO_CREATE)
	}

	fun doHelp(view: View) {
		showingHelp = true
		startActivity(Intent(this, HelpActivity::class.java))
	}

	private val timer = Timer()
	private var updateTimestampsTimertask : TimerTask? = null
	private fun setupDebugInfo() {
		val enabled = settings.showDebugInfo
		findViewById<LinearLayout>(R.id.debugInfo).visibility = if (enabled) View.VISIBLE else View.GONE
		LogUtil.setLogView(if (enabled) textLog else null)
		textLog.doOnTextChanged { text, start, before, count ->
			findViewById<ScrollView>(R.id.textLogScroll).fullScroll(View.FOCUS_DOWN)
		}
		if (!enabled) {
			textLog.text = ""

		}
		updateTimestampsTimertask?.cancel()
		if (enabled) {
			updateTimestampsTimertask = object: TimerTask() {
				override fun run() {
					Handler(Looper.getMainLooper()).post {
						findViewById<TextView>(R.id.timeStamps).text = LogUtil.getTimeStampString()
					}
				}
			}
			timer.schedule(updateTimestampsTimertask,500,500)
		}
	}

	private val statusText : TextView
		get() = findViewById(R.id.status)
	private val textLog : TextView
		get() = findViewById(R.id.textLog)

}

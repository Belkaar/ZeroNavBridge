package team.burkart.zero.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.core.content.ContextCompat.getSystemService
import team.burkart.zero.LogUtil
import java.util.Timer
import java.util.TimerTask

class Scanner (private val context: Context) {

	fun connectBike(callback: (connection: Connection?) -> Unit) : Boolean {
		connectCallback = callback
		return startScan()
	}

	fun abortScan() {
		stopScan()
	}

	private fun getBleScanner() : BluetoothLeScanner? {
		val bluetoothManager =
			getSystemService(context, BluetoothManager::class.java) as BluetoothManager
		val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
		return bluetoothAdapter?.bluetoothLeScanner
	}

	@SuppressLint("MissingPermission")
	private fun startScan() : Boolean {
		val bleScanner = getBleScanner() ?: return false

		val scanFilter = ScanFilter.Builder()
			.setServiceUuid(ParcelUuid(Connection.serviceUUID))
			.build()

		val scanSettings = ScanSettings.Builder()
			.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
			.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
			.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
			.setReportDelay(0)
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			scanSettings.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
		} else {
			scanSettings.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
		}
		bleScanner.startScan(mutableListOf(scanFilter), scanSettings.build(), scanCallback)

		timeoutTask = object: TimerTask() {
			override fun run() {
				stopScan()
				connectCallback(null)
			}
		}
		timer.schedule(timeoutTask, 30000)
		return true
	}

	@SuppressLint("MissingPermission")
	private fun stopScan() {
		getBleScanner()?.stopScan(scanCallback)
		timeoutTask?.cancel()
	}

	private val scanCallback = object : ScanCallback() {
		override fun onScanResult(callbackType: Int, result: ScanResult) {
			stopScan()
			val con = Connection(result.device, context)
			con.open(connectCallback)
		}

		override fun onBatchScanResults(results: MutableList<ScanResult>?) {
			LogUtil.log("scan batchResult $results")
		}

		override fun onScanFailed(errorCode: Int) {
			LogUtil.log("scan failed $errorCode")
			stopScan()
			connectCallback(null)
		}
	}

	private lateinit var connectCallback: (connection: Connection?) -> Unit
	private val timer = Timer()
	private var timeoutTask : TimerTask? = null
}
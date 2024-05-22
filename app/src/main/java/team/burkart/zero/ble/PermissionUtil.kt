package team.burkart.zero.ble

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import team.burkart.zero.LogUtil
class PermissionUtil(private val context: Activity) {
	private fun permissions() : Array<String> {
		val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
		} else {
			arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
		}
		return permissions
	}
	fun checkPermissions() : Boolean {
		return permissions().all { ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
	}
	fun ensurePermissions() : Boolean {
		if (checkPermissions()) {return true;}
		LogUtil.log("Permissions missing")
		ActivityCompat.requestPermissions(context, permissions(), 1)
		return false
	}
}
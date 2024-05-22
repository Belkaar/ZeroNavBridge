package team.burkart.zero.navbridge

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import team.burkart.zero.ble.PermissionUtil

class StartActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if ((intent.action == "team.burkart.zero.navbridge.START") and (PermissionUtil(this).checkPermissions())) {
			startForegroundService(Intent(this, BridgeService::class.java))
			finish()
			return
		}
	}
}

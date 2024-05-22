package team.burkart.zero.navbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NavReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		val relayIntent = Intent(context, BridgeService::class.java)
		relayIntent.action = intent.action
		intent.extras?.let { relayIntent.putExtras(it) }
		context.startForegroundService(relayIntent)
	}
}
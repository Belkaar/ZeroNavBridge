package team.burkart.zero.navbridgetest

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.ComponentActivity
import team.burkart.zero.navbridge.Sender

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
	}

	val sender = Sender(this)
	fun doStart(view: View) {
		sender.startNavigation()
	}

	fun doStop(view: View) {
		sender.stopNavigation()
	}

	fun doSend(view: View) {
		val packet = Sender.NavData()
		packet.nextManeuver = Sender.NavData.Maneuver.entries[findViewById<EditText>(R.id.nextManeuver).text.toString().toInt()]
		packet.nextManeuverDistance = findViewById<EditText>(R.id.nextManeuverDistance).text.toString().toInt()
		packet.currentName = findViewById<EditText>(R.id.currentName).text.toString()
		packet.nextName = findViewById<EditText>(R.id.nextName).text.toString()
		packet.destinationDistance = findViewById<EditText>(R.id.destinationDistance).text.toString().toInt()
		packet.eta = packet.eta.plusMinutes(findViewById<EditText>(R.id.eta).text.toString().toLong())
		packet.speedLimit = findViewById<EditText>(R.id.speedlimit).text.toString().toByte()

		sender.sendNavData(packet)
	}
}
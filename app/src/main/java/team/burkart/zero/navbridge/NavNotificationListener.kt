package team.burkart.zero.navbridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.Toast
import team.burkart.zero.packet.NavPacket

class NavNotificationListener : NotificationListenerService() {
	companion object {
		val mapsPackageName = "com.google.android.apps.maps"
		val osmAndPackageNames = arrayOf("net.osmand","net.osmand.plus")

		fun ensurePermission(context: Context) {
			val component = ComponentName(context, NavNotificationListener::class.java)
			val componentStrings = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").split(":")
			componentStrings.forEach {
				val testComponent = ComponentName.unflattenFromString(it)
				if (testComponent != null && testComponent == component) {
					return
				}
			}

			Toast.makeText(
				context, context.getString(R.string.permission_listenToMapsNotifications),
				Toast.LENGTH_SHORT
			).show()
			val int = Intent("android.settings.NOTIFICATION_LISTENER_DETAIL_SETTINGS")
			int.putExtra("android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME", component.flattenToString())
			context.startActivity(int)
		}

	}

	private val iconMatcher = MapsIconMatcher(this)

	private var bridgeService: BridgeService? = null

	private val connection = object : ServiceConnection {
		override fun onServiceConnected(className: ComponentName, service: IBinder) {
			val binder = service as BridgeService.LocalBinder
			bridgeService = binder.getService()
		}
		override fun onServiceDisconnected(arg0: ComponentName) {
			bridgeService = null
		}

		override fun onBindingDied(name: ComponentName?) {
			super.onBindingDied(name)
			bridgeService = null
		}
	}

	private val settings : Settings by lazy { Settings(this) }

	override fun onListenerConnected() {
		super.onListenerConnected()
	}

	override fun onListenerDisconnected() {
		super.onListenerDisconnected()
		if (bridgeService != null) {
			bridgeService = null
			unbindService(connection)
		}
	}

	private fun distanceFromString(string: String) : Int {
		val parts = string.split("\\s+".toRegex())
		var dist = parts[0].replace(",", ".").toFloat()
		if (parts[1] == "km") {
			dist *= 1000
		} else
		if (parts[1] == "mi") {
			dist *= 1609.34f
		} else
		if (parts[1] == "ft") {
			dist *= 0.3048f
		}
		return dist.toInt()
	}

	private fun minutesFromString(string: String) : Int {
		var result = 0
		val etaParts = string.split("\\s+".toRegex())
		for (i in 0..<etaParts.size/2) {
			var mins = etaParts[i * 2].toInt()
			if (etaParts[i * 2 + 1] == "h") {
				mins *= 60
			}
			result += mins
		}
		return result;
	}

	@Suppress("DEPRECATION")
	override fun onNotificationPosted(sbn: StatusBarNotification?) {
		super.onNotificationPosted(sbn)
		sbn ?: run {return}

		var navPacket : NavPacket? = null

		if (mapsPackageName == sbn.packageName && settings.listenToMapsNotifications) {
			navPacket = NavPacket()
			val notificationExtras = sbn.notification.extras

			/*
			notificationExtras.keySet().forEach {
				LogUtil.log("extra: " + it + "=" + notificationExtras.get(it).toString())
			}
			*/

			try {
				val str = notificationExtras.getCharSequence("android.title")!!.toString()
				navPacket.nextManeuverDistance = distanceFromString(str)
			} catch (_: Exception) {}

			try {
				val blocks = notificationExtras.getString("android.subText")!!.split(" · ")
				val mins = minutesFromString(blocks[0])
				navPacket.eta = navPacket.eta.plusMinutes(mins.toLong())
				//distance
				navPacket.destinationDistance = distanceFromString(blocks[1])
			} catch (_: Exception) {}

			navPacket.nextName = notificationExtras.getCharSequence("android.text")?.toString() ?: ""

			navPacket.nextManeuver = NavPacket.Companion.Maneuver.Straight
			try {
				val icon = notificationExtras.getParcelable("android.largeIcon") as Icon?
				if (icon != null) {
					val drawable = icon.loadDrawable(this)
					if (drawable != null) {
						navPacket.nextManeuver = iconMatcher.getManeuver(drawable)
					}
				}
			} catch (_: Exception) {}
		}

		if (osmAndPackageNames.contains(sbn.packageName) && settings.listenToOsmAndNotifications) {
			navPacket = NavPacket()
			val notificationExtras = sbn.notification.extras
			/*
			notificationExtras.keySet().forEach {
				LogUtil.log("extra: " + it + "=" + notificationExtras.get(it).toString())
			}
			*/
			try {
				val str = notificationExtras.getCharSequence("android.title")!!.toString()
				val blocks = str.split(" • ")
				navPacket.nextManeuverDistance = distanceFromString(blocks[0])
				navPacket.nextName = blocks[1]
			} catch (_: Exception) {}

			try {
				val str = notificationExtras.getCharSequence("android.bigText")!!.toString()
				val lines = str.split("\n")
				val blocks = lines[1].split(" • ")
				navPacket.destinationDistance = distanceFromString(blocks[0])
				val mins = minutesFromString(blocks[1])
				navPacket.eta = navPacket.eta.plusMinutes(mins.toLong());
			} catch (_: Exception) {}

			// unfortunately OsmAnd generates the icons on the fly, so matching is hard (for now)
			navPacket.nextManeuver = NavPacket.Companion.Maneuver.Straight
		}

		if (navPacket == null ||
			navPacket.nextManeuver == NavPacket.Companion.Maneuver.Undefined ||
			navPacket.nextManeuverDistance == 0 ||
			navPacket.nextName == ""
		) {return}
		
		if (bridgeService == null) {
			val startIntent = Intent(this, StartActivity::class.java)
			startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			startActivity(startIntent)
			bindService(Intent(this, BridgeService::class.java), connection, Context.BIND_AUTO_CREATE)
		}
		bridgeService?.updateNavPacket(navPacket)
	}

	override fun onNotificationRemoved(sbn: StatusBarNotification?) {
		super.onNotificationRemoved(sbn)
		sbn ?: run {return}
		if (
			(mapsPackageName == sbn.packageName && settings.listenToMapsNotifications) ||
			(osmAndPackageNames.contains(sbn.packageName) && settings.listenToOsmAndNotifications)
		) {
			// stop nav
			if (bridgeService != null) {
				bridgeService?.stopNavigation()
				try {
				unbindService(connection)
				} catch (_ : IllegalArgumentException) {}
			}
		}
	}
}
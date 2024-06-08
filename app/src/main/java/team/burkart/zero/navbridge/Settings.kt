package team.burkart.zero.navbridge

import android.content.Context
import android.content.SharedPreferences

class Settings(context: Context) {
	private val preferences : SharedPreferences = context.getSharedPreferences("bridgeSettings", Context.MODE_PRIVATE)
	var bridgeEnabled : Boolean
		get() = preferences.getBoolean("bridgeEnabled", true)
		set(value) {preferences.edit().putBoolean("bridgeEnabled", value).apply()}

	var maneuverShowDistance : Int
		get() = preferences.getInt("maneuverShowDistance", 0)
		set(value) {preferences.edit().putInt("maneuverShowDistance", value).apply()}

	var listenToMapsNotifications : Boolean
		get() = preferences.getBoolean("listenToMapsNotifications", false)
		set(value) {preferences.edit().putBoolean("listenToMapsNotifications", value).apply()}

	var listenToOsmAndNotifications : Boolean
		get() = preferences.getBoolean("listenToOsmAndNotifications", false)
		set(value) {preferences.edit().putBoolean("listenToOsmAndNotifications", value).apply()}

	var showDebugInfo : Boolean
		get() = preferences.getBoolean("showDebugInfo", false)
		set(value) {preferences.edit().putBoolean("showDebugInfo", value).apply()}
}
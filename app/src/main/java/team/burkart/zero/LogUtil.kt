package team.burkart.zero

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView

class LogUtil {
	companion object {
		@SuppressLint("SetTextI18n")
		fun log(message: String) {
			Log.i("zero", message)
			Handler(Looper.getMainLooper()).post {
				logView?.text = logView?.text.toString() + message + "\n"
			}
		}

		fun clear() {
			Handler(Looper.getMainLooper()).post {
				logView?.text = ""
			}
		}

		fun setLogView(view: TextView?) {
			logView = view
		}

		fun setTimeStamp(name: String) {
			timeStamps.put(name, System.currentTimeMillis())
		}

		fun getTimeStampString() : String {
			var result = ""
			val currentTime = System.currentTimeMillis()
			timeStamps.forEach {
				t, u -> result += t + ":"+ ((currentTime - u).toFloat() / 1000).toInt().toString() + "s; "
			}
			return result
		}

		private val timeStamps : MutableMap<String, Long> = HashMap()
		@SuppressLint("StaticFieldLeak")
		private var logView : TextView? = null
	}
}
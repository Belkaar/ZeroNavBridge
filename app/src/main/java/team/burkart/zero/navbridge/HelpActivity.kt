package team.burkart.zero.navbridge

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.ComponentActivity

class HelpActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_help)
		helpText.text = Html.fromHtml(resources.getString(R.string.help_text), Html.FROM_HTML_MODE_COMPACT)
		helpText.movementMethod = LinkMovementMethod.getInstance()
	}

	private val helpText : TextView
		get() = findViewById(R.id.helpText)
}
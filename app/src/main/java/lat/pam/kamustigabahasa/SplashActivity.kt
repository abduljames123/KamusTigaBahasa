package lat.pam.kamustigabahasa

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {


        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.ivLogo)
        val welcome = findViewById<TextView>(R.id.tvWelcome)

        // Mulai dari transparan
        logo.alpha = 0f
        welcome.alpha = 0f

        // Animasi fade in
        logo.animate().alpha(1f).setDuration(800).start()
        welcome.animate().alpha(1f).setDuration(800).start()

        // Delay ke MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000) // 3 detik
    }
}

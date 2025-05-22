package com.example.papercolor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var getStartButton: Button
    private lateinit var nextButton1: Button
    private lateinit var nextButton2: Button
    private lateinit var loadingText: TextView
    private lateinit var adsText: TextView

    // Các layout cho từng màn hình
    private lateinit var screen1: RelativeLayout
    private lateinit var screen2: RelativeLayout
    private lateinit var screen3: RelativeLayout
    private lateinit var screen4: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Khởi tạo tất cả view
        progressBar = findViewById(R.id.progressBar)
        getStartButton = findViewById(R.id.getStartButton)
        nextButton1 = findViewById(R.id.nextButton1)
        nextButton2 = findViewById(R.id.nextButton2)
        loadingText = findViewById(R.id.loadingText)
        adsText = findViewById(R.id.adsText)

        // Khởi tạo các màn hình
        screen1 = findViewById(R.id.screen1)
        screen2 = findViewById(R.id.screen2)
        screen3 = findViewById(R.id.screen3)
        screen4 = findViewById(R.id.screen4)

        // Ẩn tất cả các màn hình trừ màn hình đầu tiên
        screen1.visibility = View.VISIBLE
        screen2.visibility = View.GONE
        screen3.visibility = View.GONE
        screen4.visibility = View.GONE

        // Mô phỏng quá trình loading
        simulateLoading()

        // Xử lý sự kiện khi nhấn nút Next và Get Start
        setupButtonListeners()
    }

    private fun simulateLoading() {
        val handler = Handler(Looper.getMainLooper())
        var progress = 0

        handler.postDelayed(object : Runnable {
            override fun run() {
                progress += 5
                progressBar.progress = progress

                if (progress < 100) {
                    handler.postDelayed(this, 100)
                } else {
                    // Khi loading xong, chuyển sang màn hình 2
                    handler.post {
                        screen1.visibility = View.GONE
                        screen2.visibility = View.VISIBLE
                    }
                }
            }
        }, 100)
    }

    private fun setupButtonListeners() {
        // Chuyển từ màn hình 2 sang 3
        nextButton1.setOnClickListener {
            screen2.visibility = View.GONE
            screen3.visibility = View.VISIBLE
        }

        // Chuyển từ màn hình 3 sang 4
        nextButton2.setOnClickListener {
            screen3.visibility = View.GONE
            screen4.visibility = View.VISIBLE
        }

        // Chuyển từ màn hình 4 sang MainMenu
        getStartButton.setOnClickListener {
            startMainMenuActivity()
        }
    }

    private fun startMainMenuActivity() {
        val intent = Intent(this, MainMenuActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
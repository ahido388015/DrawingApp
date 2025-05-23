package com.example.papercolor.ui.splash


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.papercolor.R
import com.example.papercolor.databinding.FragmentSplashBinding
import com.example.papercolor.model.SplashScreenData

class SplashFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var getStartButton: Button
    private lateinit var nextButton1: Button
    private lateinit var nextButton2: Button
    private lateinit var loadingText: TextView
    private lateinit var adsText: TextView

    private lateinit var screen1: RelativeLayout
    private lateinit var screen2: RelativeLayout
    private lateinit var screen3: RelativeLayout
    private lateinit var screen4: RelativeLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo tất cả view
        progressBar = view.findViewById(R.id.progressBar)
        getStartButton = view.findViewById(R.id.getStartButton)
        nextButton1 = view.findViewById(R.id.nextButton1)
        nextButton2 = view.findViewById(R.id.nextButton2)
        loadingText = view.findViewById(R.id.loadingText)
        adsText = view.findViewById(R.id.adsText)

        screen1 = view.findViewById(R.id.screen1)
        screen2 = view.findViewById(R.id.screen2)
        screen3 = view.findViewById(R.id.screen3)
        screen4 = view.findViewById(R.id.screen4)

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
                    screen1.visibility = View.GONE
                    screen2.visibility = View.VISIBLE
                }
            }
        }, 100)
    }

    private fun setupButtonListeners() {
        nextButton1.setOnClickListener {
            screen2.visibility = View.GONE
            screen3.visibility = View.VISIBLE
        }

        nextButton2.setOnClickListener {
            screen3.visibility = View.GONE
            screen4.visibility = View.VISIBLE
        }

        getStartButton.setOnClickListener {
            findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
        }
    }
}
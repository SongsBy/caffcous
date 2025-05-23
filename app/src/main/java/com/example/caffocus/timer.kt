package com.example.caffocus

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class timer : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var timerText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startPauseButton: ImageButton
    private lateinit var quoteText: TextView
    private lateinit var resetButton: ImageButton
    private lateinit var stopButton: ImageButton

    private var isRunning = false
    private var isWorkTime = true
    private var timeLeftInMillis: Long = 25 * 60 * 1000
    private var totalTimeInMillis: Long = 25 * 60 * 1000
    private var countDownTimer: CountDownTimer? = null

    private lateinit var prefs: SharedPreferences

    private val quotes = listOf(
        "시작이 반이다.",
        "포기하지 마라. 큰일도 작은 행동에서 시작된다.",
        "공부는 미래의 너에게 보내는 선물이다.",
        "오늘 걷지 않으면 내일은 뛰어야 한다.",
        "성공은 작은 노력이 반복된 결과다.",

    )

    private val quoteHandler = Handler(Looper.getMainLooper())
    private val quoteRunnable = object : Runnable {
        override fun run() {
            if (prefs.getBoolean("show_quote", true)) {
                quoteText.text = quotes.random()
                quoteHandler.postDelayed(this, 5 * 60 * 1000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timer)

        prefs = getSharedPreferences("settings", MODE_PRIVATE)

        resetButton = findViewById(R.id.resetButton)
        stopButton = findViewById(R.id.stopButton)
        timerText = findViewById(R.id.timerText)
        progressBar = findViewById(R.id.progressBar)
        startPauseButton = findViewById(R.id.startPauseButton)
        quoteText = findViewById(R.id.quoteText)

        findViewById<ImageButton>(R.id.calendar).setOnClickListener {
            startActivity(Intent(this, calendaractivity::class.java))
        }

        findViewById<ImageButton>(R.id.user).setOnClickListener {
            startActivity(Intent(this, useractivity::class.java))
        }

        startPauseButton.setOnClickListener {
            if (isRunning) pauseTimer() else startTimer()
        }

        resetButton.setOnClickListener {
            resetTimer()
        }

        stopButton.setOnClickListener {
            stopTimerAndShowEndDialog()
        }

        quoteHandler.postDelayed(quoteRunnable, 0)
        updateQuoteVisibility()
        updateTimerText()
        updateProgress()

        // 초기 버튼 상태
        startPauseButton.setImageResource(R.drawable.play)
        resetButton.visibility = View.GONE
        stopButton.visibility = View.GONE
    }

    private fun startTimer() {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
                updateProgress()
            }

            override fun onFinish() {
                isRunning = false
                startPauseButton.setImageResource(R.drawable.play)
                mediaPlayer?.pause()

                showAlert("시간 종료", "다시 시작할까요?") {
                    resetTimer()
                }
            }
        }.start()

        val selectedMusic = if (isWorkTime) {
            prefs.getString("music", "whitenoise1")
        } else {
            prefs.getString("restmusic", "whitenoise2")
        }
        val volume = prefs.getInt("volume", 50) / 100f
        val resid = resources.getIdentifier(selectedMusic, "raw", packageName)

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resid)?.apply {
            isLooping = true
            setVolume(volume, volume)
            start()
        }

        isRunning = true
        startPauseButton.setImageResource(R.drawable.pause)
        resetButton.visibility = View.GONE
        stopButton.visibility = View.GONE
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        mediaPlayer?.pause()
        isRunning = false

        prefs.edit()
            .putBoolean("isRunning", false)
            .putLong("time_left", timeLeftInMillis)
            .apply()

        startPauseButton.setImageResource(R.drawable.play)
        resetButton.visibility = View.VISIBLE
        stopButton.visibility = View.VISIBLE
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        mediaPlayer?.pause()
        mediaPlayer?.seekTo(0)
        isRunning = false
        isWorkTime = true
        totalTimeInMillis = prefs.getInt("focus_minutes", 25) * 60 * 1000L
        timeLeftInMillis = totalTimeInMillis

        updateTimerText()
        updateProgress()

        startPauseButton.setImageResource(R.drawable.play)
        resetButton.visibility = View.GONE
        stopButton.visibility = View.GONE
    }

    private fun stopTimerAndShowEndDialog() {
        countDownTimer?.cancel()
        mediaPlayer?.pause()
        isRunning = false
        isWorkTime = true
        totalTimeInMillis = prefs.getInt("focus_minutes", 25) * 60 * 1000L
        timeLeftInMillis = totalTimeInMillis

        updateTimerText()
        updateProgress()

        startPauseButton.setImageResource(R.drawable.play)
        resetButton.visibility = View.GONE
        stopButton.visibility = View.GONE

        showAlert("타이머 종료", "홈으로 돌아갑니다.") {
        }
    }

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateProgress() {
        val progress = ((totalTimeInMillis - timeLeftInMillis) / 1000).toInt()
        progressBar.max = (totalTimeInMillis / 1000).toInt()
        progressBar.progress = progress
    }

    private fun updateQuoteVisibility() {
        val showQuote = prefs.getBoolean("show_quote", true)
        if (showQuote) {
            val randomQuote = quotes.random()
            quoteText.text = randomQuote
            quoteText.visibility = TextView.VISIBLE
            quoteHandler.removeCallbacks(quoteRunnable)
            quoteHandler.postDelayed(quoteRunnable, 5 * 60 * 1000L) // 5분 후 갱신
        } else {
            quoteText.visibility = TextView.GONE
            quoteHandler.removeCallbacks(quoteRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isRunning) {
            prefs.edit()
                .putLong("resume_time", System.currentTimeMillis())
                .putLong("time_left", timeLeftInMillis)
                .putBoolean("isRunning", true)
                .putBoolean("isWorkTime", isWorkTime)
                .apply()
        }
    }

    override fun onResume() {
        super.onResume()
        val wasRunning = prefs.getBoolean("isRunning", false)
        val resumeTime = prefs.getLong("resume_time", System.currentTimeMillis())
        val previousLeft = prefs.getLong("time_left", totalTimeInMillis)
        val passed = System.currentTimeMillis() - resumeTime
        timeLeftInMillis = previousLeft - passed
        if (timeLeftInMillis < 0L) timeLeftInMillis = 0L

        isRunning = wasRunning
        isWorkTime = prefs.getBoolean("isWorkTime", true)

        if (wasRunning) {
            startTimer()
        } else {
            updateTimerText()
            updateProgress()
            // paused 상태였던 경우 UI 유지
            startPauseButton.setImageResource(R.drawable.play)
            resetButton.visibility = View.VISIBLE
            stopButton.visibility = View.VISIBLE
        }

        updateQuoteVisibility()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        quoteHandler.removeCallbacks(quoteRunnable)
    }

    private fun showAlert(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("확인") { _, _ -> onConfirm() }
            .show()
    }
}
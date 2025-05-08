package com.example.pomo

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var timerText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startPauseButton: ImageButton
    private lateinit var quoteText: TextView

    private var isRunning = false
    private var isWorkTime = true
    private var timeLeftInMillis: Long = 25 * 60 * 1000
    private var totalTimeInMillis: Long = 25 * 60 * 1000
    private var countDownTimer: CountDownTimer? = null
    private var quoteTimer: CountDownTimer? = null

    private lateinit var prefs: SharedPreferences

    private val quotes = listOf(
        "시작이 반이다.",
        "포기하지 마라. 큰일도 작은 행동에서 시작된다.",
        "공부는 미래의 너에게 보내는 선물이다.",
        "오늘 걷지 않으면 내일은 뛰어야 한다.",
        "성공은 작은 노력이 반복된 결과다."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("settings", MODE_PRIVATE)

        statusText = findViewById(R.id.statusText)
        timerText = findViewById(R.id.timerText)
        progressBar = findViewById(R.id.progressBar)
        startPauseButton = findViewById(R.id.startPauseButton)
        quoteText = findViewById(R.id.quoteText)

        updateQuoteVisibility()
        updateTimerText()
        updateProgress()

        startPauseButton.setOnClickListener {
            if (isRunning) pauseTimer() else startTimer()
        }

        startQuoteTimer()
    }

    override fun onResume() {
        super.onResume()
        updateQuoteVisibility()
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
                updateProgress()
            }

            override fun onFinish() {
                isRunning = false
                startPauseButton.setImageResource(android.R.drawable.ic_media_play)
                val title = if (isWorkTime) "25분 종료" else "휴식 끝"
                val message = if (isWorkTime) "5분 휴식을 시작할까요?" else "다시 집중을 시작할까요?"
                showAlert(title, message) {
                    isWorkTime = !isWorkTime
                    totalTimeInMillis = if (isWorkTime) 25 * 60 * 1000 else 5 * 60 * 1000
                    timeLeftInMillis = totalTimeInMillis
                    statusText.text = if (isWorkTime) "집중 시간" else "휴식 시간"
                    updateTimerText()
                    updateProgress()
                    startTimer()
                }
            }
        }.start()

        isRunning = true
        startPauseButton.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isRunning = false
        startPauseButton.setImageResource(android.R.drawable.ic_media_play)
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
            quoteText.text = quotes.random()
            quoteText.visibility = TextView.VISIBLE
        } else {
            quoteText.visibility = TextView.GONE
        }
    }

    private fun startQuoteTimer() {
        quoteTimer?.cancel()
        if (!prefs.getBoolean("show_quote", true)) return

        quoteTimer = object : CountDownTimer(300000, 300000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                quoteText.text = quotes.random()
                startQuoteTimer()
            }
        }.start()
    }

    private fun showAlert(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("확인") { _, _ -> onConfirm() }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        quoteTimer?.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }
}

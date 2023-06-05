package com.example.wifiwithkotlin

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CCTVActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cctvactivity)

        val webview:WebView = findViewById(R.id.webview)
        webview.webViewClient = WebViewClient()
        webview.loadUrl("http://192.168.0.101/mjpeg/1")
        //webview.loadUrl("https://www.naver.com/")

        val finish_button:Button = findViewById(R.id.btnFinish)
        finish_button.setOnClickListener {
            finish()
        }
    }
//    private var backBtnTime: Long = 0
//
//    override fun onBackPressed() {
//        val curTime = System.currentTimeMillis()
//        val gapTime = curTime - backBtnTime
//        if (webview.canGoBack()) {
//            webview.goBack()
//        } else if (0 <= gapTime && 2000 >= gapTime) {
//            super.onBackPressed()
//        } else {
//            backBtnTime = curTime
//            Toast.makeText(this, "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
//        }
//    }

}
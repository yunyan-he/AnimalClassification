package com.example.animalclaasification


import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.animalclaasification.databinding.ActivityInfoBinding

class InfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInfoBinding
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar  // 用于显示加载进度条
    private var isPageLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化WebView和进度条
        webView = binding.webView
        progressBar = binding.progressBar

        // 启用JavaScript
        webView.settings.javaScriptEnabled = true

        // 设置WebViewClient处理页面跳转
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // 防止打开浏览器，使用WebView加载
                view.loadUrl(request.url.toString())
                return true
            }

            // 处理加载成功
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // 标记页面已加载成功
                isPageLoaded = true
                // 隐藏进度条
                progressBar.visibility = ProgressBar.GONE
            }

            // 处理加载错误
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                super.onReceivedError(view, request, error)
                // 只有在页面没有加载成功时，才显示错误消息
                if (!isPageLoaded) {
                    Toast.makeText(this@InfoActivity, "加载失败，请检查网络", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 设置WebChromeClient来显示加载进度
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                // 更新进度条
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = ProgressBar.GONE
                } else {
                    progressBar.visibility = ProgressBar.VISIBLE
                }
            }
        }

        // 获取传递的URL
        val url = intent.getStringExtra("url")
        if (url != null && url.isNotEmpty()) {
            // 校验 URL 是否有效
            if (url.startsWith("http://") || url.startsWith("https://")) {
                webView.loadUrl(url)
            } else {
                Toast.makeText(this, "无效的URL", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "URL为空", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理WebView资源，防止内存泄漏
        webView.stopLoading()
        webView.removeAllViews()
        webView.destroy()
    }

}

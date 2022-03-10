package com.persist.solution.atootdor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Sample extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        WebView webview = findViewById(R.id.webview);

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });
        webview.getSettings().setSupportZoom(true);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setSupportZoom(true);

        String pdfUrl = getIntent().getExtras().getString("pdf_url");
        webview.loadUrl("http://drive.google.com/gview?embedded=true&url=" + pdfUrl);
    }
}
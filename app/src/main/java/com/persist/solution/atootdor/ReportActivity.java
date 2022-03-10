package com.persist.solution.atootdor;

import android.app.ProgressDialog;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.persist.solution.atootdor.databinding.ActivityReportBinding;

public class ReportActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityReportBinding binding;
    private WebView webview;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityReportBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            setSupportActionBar(binding.toolbar);

            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Please wait...");
            getSupportActionBar().show();

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            webview = findViewById(R.id.webview);
            webview.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    progressDialog.dismiss();
                }
            });
            webview.getSettings().setSupportZoom(true);
            webview.getSettings().setJavaScriptEnabled(true);
            webview.getSettings().setSupportZoom(true);
            String pdfUrl = getIntent().getExtras().getString("pdf_url");
            pdfUrl = "https://file-examples-com.github.io/uploads/2017/10/file-sample_150kB.pdf";
            webview.loadUrl("http://drive.google.com/gview?embedded=true&url=" + pdfUrl);
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);

    }
}
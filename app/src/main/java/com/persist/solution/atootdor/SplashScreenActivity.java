package com.persist.solution.atootdor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.persist.solution.atootdor.R;
import com.persist.solution.atootdor.utils.AppSettingSharePref;

public class SplashScreenActivity extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 2000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                Intent i = new Intent(SplashScreenActivity.this, MainActivity.class);
                Intent intent = getIntent();
                if(intent != null && intent.hasExtra("url")) {
                    String url = intent.getStringExtra("url");
                    if(url!= null && !url.equals("")) {
                        i.putExtra("url", url);
                    }
                }
                startActivity(i);

                // close this activity
                finish();
            }
        }, SPLASH_TIME_OUT);


    }
}
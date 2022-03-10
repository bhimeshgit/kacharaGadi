package com.persist.solution.atootdor;

import android.app.Application;
import android.os.StrictMode;

public class ApplicationDemo extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
    }
}

package com.persist.solution.atootdor.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.View;

import com.persist.solution.atootdor.MainActivity;
import com.persist.solution.atootdor.R;


/**
 * Created by dragank on 2/25/2016.
 */
public class ProgressDialogHelper {
    private static ProgressDialog progress;

    public static void dismissProgress() {
        if (progress != null) {
            progress.dismiss();
            progress.cancel();
            progress = null;
        }
    }

    public static void showProgress(Context mContext) {
        if (progress != null) {
            return;
        }
        progress = new ProgressDialog(mContext, R.style.MyProgressTheme);
        progress.setProgressStyle(android.R.style.Widget_ProgressBar_Large);
        progress.show();


    }
}

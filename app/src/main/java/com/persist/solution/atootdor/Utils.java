package com.persist.solution.atootdor;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class Utils {


    public static boolean isObjNotNull(Object object) {
        boolean isValide = true;
        try {

            if (object == null) {
                isValide = false;
            } else {
                if (object.equals("")) {
                    isValide = false;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            isValide = false;
        }
        return isValide;

    }



    public static boolean isStringNotNull(String object) {
        boolean isValide = true;
        try {
            if (object == null) {
                isValide = false;
            } else {
                if (object.trim().equals("")) {
                    isValide = false;
                }
                if (object.trim().equalsIgnoreCase("null")) {
                    isValide = false;
                }
            }
        } catch (Exception e) {
        }
        return isValide;
    }

    public static void showToastMsg(Context context, String msg) {
        try {
            if (Utils.isObjNotNull(context)) {
                if (Utils.isStringNotNull(msg)) {
                    Toast.makeText(context,msg, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showToastMsgShort(Context context, String msg) {
        try {
            if (Utils.isObjNotNull(context)) {
                boolean isActivityRunning = false;
                try {
                    isActivityRunning = ((Activity) context).isFinishing();
                } catch (Exception e) {
                    e.printStackTrace();
                    //Crashlytics.logException(e);
                }

                if (isActivityRunning) {
                    Toast.makeText(context,msg, Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static boolean isNetworkAvailable(Context context) {
        boolean isConnected = false;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return isConnected;
        }
    }
}

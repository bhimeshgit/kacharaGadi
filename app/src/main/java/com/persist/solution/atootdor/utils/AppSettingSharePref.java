package com.persist.solution.atootdor.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettingSharePref {

    private static AppSettingSharePref appSettingSharePref = new AppSettingSharePref();
    private static SharedPreferences sharePre;
    private static SharedPreferences.Editor editor;

    private AppSettingSharePref() {
    }

    public static AppSettingSharePref getInstance(Context context) {
        try {
            if (sharePre == null) {
                sharePre = context.getSharedPreferences("PropertySharePref", 0);
                editor = sharePre.edit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appSettingSharePref;
    }


    public String getUserName() {
        String username = "";
        try {
            username = sharePre.getString("username", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return username;
    }

    public void setUsername(String username) {
        editor.putString("username", username);
        editor.apply();
    }


    public String getUid() {
        String username = "";
        try {
            username = sharePre.getString("uid", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return username;
    }

    public void setUid(String username) {
        editor.putString("uid", username);
        editor.apply();
    }

    public String getToken() {
        String username = "";
        try {
            username = sharePre.getString("token", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return username;
    }

    public void setToken(String token) {
        editor.putString("token", token);
        editor.apply();
    }

    public void setPopup() {
        editor.putInt("popup", 1);
        editor.apply();
    }

    public int getPopup() {
        int username = 0;
        try {
            username = sharePre.getInt("popup", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return username;
    }

    public String getDeviceList() {
        String username = "";
        try {
            username = sharePre.getString("device_list", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return username;
    }

    public void setDeviceList(String device_list) {
        editor.putString("device_list", device_list);
        editor.apply();
    }

    public String getOldDeviceList() {
        String username = "";
        try {
            username = sharePre.getString("old_device_list", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return username;
    }

    public void setOldDeviceList(String old_device_list) {
        editor.putString("old_device_list", old_device_list);
        editor.apply();
    }
}

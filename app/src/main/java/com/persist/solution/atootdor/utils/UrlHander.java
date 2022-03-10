package com.persist.solution.atootdor.utils;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;

import android.webkit.URLUtil;
import android.widget.Toast;



/**
 * Created by dragank on 10/1/2016.
 */
 public class UrlHander {

    public static boolean checkUrl(Activity mActivity, String url) {

        Uri uri = Uri.parse(url);
        if (url.startsWith("mailto:")) {
            email(mActivity, url);
            return true;
        } else if (url.startsWith("geo:") || uri.getHost().equals("maps.google.com")) {
            url = url.replace("https://maps.google.com/maps?daddr=", "geo:");
            map(mActivity, url);
            return true;
        } else if (url.contains("youtube")) {
            openYoutube(mActivity, url);
            return true;
        } else if (uri.getHost().equals("play.google.com")) {
            openGooglePlay(mActivity, url);
        }

        return false;
    }



    private static void email(Activity mActivity, String url) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse(url));
        mActivity.startActivity(emailIntent);
    }

    public static void map(Activity mActivity, String url) {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW);
        mapIntent.setData(Uri.parse(url));
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(mActivity.getPackageManager()) != null) {
            mActivity.startActivity(mapIntent);
        }
    }


    private static void openGooglePlay(Activity mActivity, String url) {
//        url = url.replace("http://play.google.com/store/apps/", "market://");
        Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (googlePlayIntent.resolveActivity(mActivity.getPackageManager()) != null) {
            mActivity.startActivity(googlePlayIntent);
        }
    }

    private static void openYoutube(Activity mActivity, String url) {
        Intent youtubeIntent = new Intent(Intent.ACTION_VIEW);
        youtubeIntent.setData(Uri.parse(url));
        if (youtubeIntent.resolveActivity(mActivity.getPackageManager()) != null) {
            mActivity.startActivity(youtubeIntent);
        }
    }


}

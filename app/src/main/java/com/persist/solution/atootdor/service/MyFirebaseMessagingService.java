package com.persist.solution.atootdor.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.persist.solution.atootdor.MainActivity;
import com.persist.solution.atootdor.R;
import com.persist.solution.atootdor.utils.AppSettingSharePref;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    String ADMIN_CHANNEL_ID="Admin channel";
    NotificationManager notificationManager;
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
    }
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        String not_msg=remoteMessage.getData().get("message");
        String not_title=remoteMessage.getData().get("title");
        String url = "";
        Log.d("iss","not_title = "+not_title);
        Log.d("iss","asdf notification come");
        if (remoteMessage.getData().get("url")!= null){
            url = remoteMessage.getData().get("url");

        }

        if (AppSettingSharePref.getInstance(this).getUid()!= null && !AppSettingSharePref.getInstance(this).getUid().equals("")) {
            Log.d("iss","notification come");
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            //Setting up Notification channels for android O and above
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                setupChannels();
            }
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("url", url);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);
            int notificationId = new Random().nextInt(60000);
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ADMIN_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)  //a resource for your custom small icon
                    .setContentTitle(not_title) //the "title" value you sent in your notification
                    .setContentText(not_msg) //ditto
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)  //dismisses the notification on click
                    .setSound(defaultSoundUri);
            if (remoteMessage.getNotification().getImageUrl() != null) {
                Bitmap bitmap = getBitmapfromUrl(remoteMessage.getData().get("image-url"));
                notificationBuilder.setStyle(
                        new NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .bigLargeIcon(null)
                ).setLargeIcon(bitmap);
            }

            notificationManager.notify(notificationId /* ID of notification */, notificationBuilder.build());
        }
    }

    public Bitmap getBitmapfromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);

        } catch (Exception e) {
            Log.e("awesome", "Error in getting notification image: " + e.getLocalizedMessage());
            return null;
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupChannels(){
        CharSequence adminChannelName = getString(R.string.notifications_admin_channel_name);
        String adminChannelDescription = getString(R.string.notifications_admin_channel_description);

        NotificationChannel adminChannel;
        adminChannel = new NotificationChannel(ADMIN_CHANNEL_ID, adminChannelName, NotificationManager.IMPORTANCE_LOW);
        adminChannel.setDescription(adminChannelDescription);
        adminChannel.enableLights(true);
        adminChannel.enableVibration(true);
        adminChannel.setLightColor(Color.RED);
        adminChannel.enableVibration(true);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(adminChannel);
        }
    }
}

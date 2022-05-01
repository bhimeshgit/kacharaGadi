package com.persist.solution.atootdor;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.messaging.FirebaseMessaging;
import com.persist.solution.atootdor.service.GetAllVehicleLocationWorker;
import com.persist.solution.atootdor.service.UserUpdateLocationWorker;
import com.persist.solution.atootdor.utils.AppSettingSharePref;
import com.persist.solution.atootdor.utils.JsonParserVolley;
import com.persist.solution.atootdor.utils.ProgressDialogHelper;
import com.persist.solution.atootdor.utils.UrlHander;
import com.persist.solution.atootdor.utils.WebUrl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity   {

    SwipeRefreshLayout mySwipeRefreshLayout;
    CoordinatorLayout main_content;
    ImageView imageView2;
    WebView myWebView ;
    String[] permissions= new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE,Manifest.permission.CAMERA};
    ConnectivityManager mConnectivityManager;
    ConnectivityManager.NetworkCallback mNetworkCallback ;
    ProgressDialog pDialog;
    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private final String share_link_url = "http://www.atootdor.com";
    MapFragment map_fragment;
    private WorkManager workManager;
    private WorkRequest workRequest;
    private WorkManager workManagerUser;
    private   WorkRequest workRequestUser;

    FloatingActionButton mAddFab;
    public static final int MULTIPLE_PERMISSIONS = 10;
    String url_load = WebUrl.MAIN_USER_URL;//"http://www.atootdor.com/app_atoot7/login.php";
    String login_url = WebUrl.MAIN_USER_URL;//"http://www.atootdor.com/app_atoot7/login.php";
    String url_load_inapp = WebUrl.MAIN_USER_HOME_URL;//"http://www.atootdor.com/app_atoot7/home.php";

    String offline_url = "file:///android_asset/app/offline.html";
    String currentUrl = url_load;
    boolean offline_url_flag = false;
    private static int RC_SIGN_IN = 202;
    private boolean show_content = true;
    private String mCameraPhotoPath;
    long reference = -1;long id = -2;
    private LinearLayoutCompat mainLinLay;
    private FrameLayout mapFrameLay;
    public static double USER_LATITUDE = 0;
    public static double USER_LONGITUDE = 0;
    public static double PICKUP_LATITUDE = 0;
    public static double PICKUP_LONGITUDE = 0;

    String mode = "user";
//    String mode = "driver";
    int LOCATION_REQUEST_CODE = 10001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        if (mode.equals("driver")){
            url_load = WebUrl.MAIN_DRIVER_URL;
            login_url = WebUrl.MAIN_DRIVER_URL;
            url_load_inapp = WebUrl.MAIN_DRIVER_HOME_URL;
        }
        mainLinLay = findViewById(R.id.mainLinLay);
        mySwipeRefreshLayout = (SwipeRefreshLayout)this.findViewById(R.id.swipeContainer);
        mapFrameLay = findViewById(R.id.mapFrameLay);
        myWebView = (WebView) findViewById(R.id.webview);
        mAddFab = findViewById(R.id.shareBtn);
        main_content = findViewById(R.id.main_content);
        imageView2 = findViewById(R.id.imageView2);
        setWebView();
//        startLocationTrackingWorker();
        turnOnGPS();
        startUpdatingUserLocation();
//        try {
//            if(!AppSettingSharePref.getInstance(this).getUid().equals("")) {
//                startUpdatingUserLocation();
//            }
//        } catch (Exception e){}
    }

    private void startLocationTrackingWorker(){
        workManager = WorkManager.getInstance(this);
        workRequest = new PeriodicWorkRequest.Builder(GetAllVehicleLocationWorker.class, 15, TimeUnit.MINUTES).addTag("ferDriverLoc").build();
        workManager.enqueue(workRequest);
    }

    private void stopLocationTrackingWorker(){
        WorkManager.getInstance(this).cancelAllWorkByTag("ferDriverLoc");
    }

    private void addMapFragment() {
        mainLinLay.setVisibility(View.GONE);
        mapFrameLay.setVisibility(View.VISIBLE);
        if(map_fragment == null) {
            map_fragment = new MapFragment(this);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mapFrameLay, map_fragment)
                .commit();
    }

    private void setWebView(){
        if(Utils.isNetworkAvailable(MainActivity.this)) {
            getDataFromNotification();
        } else {
//            myWebView.loadUrl(offline_url);
//            currentUrl = offline_url;
        }

        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        myWebView.getSettings().setAllowContentAccess(true);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setPluginState(WebSettings.PluginState.ON);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        myWebView.canGoBack();
        myWebView.setWebChromeClient(new WebChromeClient() {});

        myWebView.setWebChromeClient(new WebChromeClient()
        {
            // For 3.0+ Devices (Start)
            // onActivityResult attached before constructor
            protected void openFileChooser(ValueCallback uploadMsg, String acceptType)
            {
                mUploadMessage = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                i.addCategory(Intent.CATEGORY_OPENABLE);
//                i.setType("image/*");
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "File Browser"), FILECHOOSER_RESULTCODE);
            }


            // For Lollipop 5.0+ Devices
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams)
            {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                }
                uploadMessage = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Log.e("ErrorCreatingFile", "Unable to create Image File", ex);
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, REQUEST_SELECT_FILE);

                return true;
            }

            //For Android 4.1 only
            protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)
            {
                mUploadMessage = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "File Browser"), FILECHOOSER_RESULTCODE);
            }

            protected void openFileChooser(ValueCallback<Uri> uploadMsg)
            {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
            }
        });

        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        myWebView.reload();
                    }
                }
        );

        // mAddFab.setVisibility(View.GONE);

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(myWebView, url);
                mySwipeRefreshLayout.setRefreshing(false);
                show_content = false;
                ProgressDialogHelper.dismissProgress();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                boolean netAvailable = Utils.isNetworkAvailable(MainActivity.this);
//                if( netAvailable && currentUrl.equals(offline_url)) {
//                    myWebView.goBack();
//                }
//                if(!netAvailable){
//                    currentUrl = offline_url;
//                    myWebView.loadUrl(currentUrl);
//                    url = offline_url;
//                }

                boolean result = UrlHander.checkUrl(MainActivity.this, url);
                if (result) {
                    ProgressDialogHelper.dismissProgress();
                } else {
                    currentUrl = url;
                    if (!show_content) {
                        ProgressDialogHelper.showProgress(MainActivity.this);
                    }
                }

                if (AppSettingSharePref.getInstance(MainActivity.this).getPopup() == 1) {
                    char[] charArray = url.toCharArray();
                    boolean q_mark_found = false;
                    for (char ch : charArray) {
                        if (ch == '?') {
                            q_mark_found = true;
                            break;
                        }
                    }
                    if (q_mark_found) {
                        url = url + "&popup=1";
                    } else {
                        url = url + "?popup=1";
                    }
                }

                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        mAddFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                shareApplication();
//                shareApplication("enter ur message here", "application Name");
        //        testMap();;
                addMapFragment();
            }
        });

        checkPermissions();

        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("iss", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();
                        AppSettingSharePref.getInstance(MainActivity.this).setToken(token);
                    }
                });

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            NetworkRequest request = new NetworkRequest.Builder().build();
            mNetworkCallback = new ConnectivityManager.NetworkCallback(){
                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    Handler handler = new Handler(getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
//                            myWebView.loadUrl(offline_url);
//                            currentUrl = offline_url;
//                            offline_url_flag = true;
                        }
                    });

                }

                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    Handler handler = new Handler(getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
//                            myWebView.clearHistory();
                            currentUrl = url_load;
                            myWebView.loadUrl(currentUrl);
                        }
                    });
                }
            };
            mConnectivityManager.registerNetworkCallback(request, mNetworkCallback);
        }

        int myColor = Color.parseColor("#eb4d97");
        mySwipeRefreshLayout.setProgressBackgroundColorSchemeColor(myColor);

        mySwipeRefreshLayout.setVisibility(View.GONE);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            checkSettingsAndStartLocationUpdates();
        } else {
                askLocationPermission();
        }
    }

    public void turnOnGPS(){
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000/2);

        LocationSettingsRequest.Builder locationSettingsRequestBuilder = new LocationSettingsRequest.Builder();

        locationSettingsRequestBuilder.addLocationRequest(locationRequest);
        locationSettingsRequestBuilder.setAlwaysShow(true);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(locationSettingsRequestBuilder.build());


        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                if (e instanceof ResolvableApiException){
                    try {
                        ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                        resolvableApiException.startResolutionForResult(MainActivity.this,
                                10001);
                    } catch (IntentSender.SendIntentException sendIntentException) {
                        sendIntentException.printStackTrace();
                    }
                }
            }
        });
    }


    private void getDataFromNotification(){
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra("url")) {
            String url = intent.getStringExtra("url");
            if(url!= null && !url.equals("")) {
                url = url+  "&uid=" + AppSettingSharePref.getInstance(MainActivity.this).getUid()+ "&fid=" + AppSettingSharePref.getInstance(MainActivity.this).getToken();
                myWebView.loadUrl(url);
                currentUrl = url;
                url_load = currentUrl;
            } else {
                myWebView.loadUrl(url_load);
            }
            return;
        } if(intent != null && intent.getData()!= null) {
            Uri uri = intent.getData();
            if (uri.getPath()!= null){
                myWebView.loadUrl(share_link_url+uri.getPath());
                currentUrl = share_link_url+uri.getPath();
                return;
            }
        }

        String username = AppSettingSharePref.getInstance(MainActivity.this).getUserName();

        if(username != null && username.trim().length() > 0 ) {
            url_load_inapp = url_load_inapp + "?uid=" + AppSettingSharePref.getInstance(MainActivity.this).getUid();
            url_load = url_load_inapp;
            currentUrl = url_load;
            myWebView.loadUrl(url_load_inapp);
        } else {
            myWebView.loadUrl(url_load);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    private void openWhatsApp(String smsNumber) {
        boolean isWhatsappInstalled = whatsappInstalledOrNot("com.whatsapp");
        if (isWhatsappInstalled) {

            Intent sendIntent = new Intent("android.intent.action.MAIN");
            sendIntent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.Conversation"));
            sendIntent.putExtra("jid", PhoneNumberUtils.stripSeparators(smsNumber) + "@s.whatsapp.net");//phone number without "+" prefix

            startActivity(sendIntent);
        } else {
            Uri uri = Uri.parse("market://details?id=com.whatsapp");
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            Toast.makeText(this, "WhatsApp not Installed",
                    Toast.LENGTH_SHORT).show();
            startActivity(goToMarket);
        }
    }

    private boolean whatsappInstalledOrNot(String uri) {
        PackageManager pm = getPackageManager();
        boolean app_installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }
    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(MainActivity.this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissionsList[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:{
                if (grantResults.length > 0) {
                    String permissionsDenied = "";
                    for (String per : permissionsList) {
                        if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                            permissionsDenied += "\n" + per;

                        }

                    }
                    // Show permissionsDenied
                    //  updateViews();
                }
                return;
            }
        }
    }


    private void shareApplicationAndroid(String message, String appName) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, appName);
            String shareMessage= "\n"+message+"\n\n";
            shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID +"\n\n";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "choose one"));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    private void shareApplication() {
        try {

            ApplicationInfo app = getApplicationContext().getApplicationInfo();
            String filePath = app.sourceDir;

            Intent intent = new Intent(Intent.ACTION_SEND);

            // MIME of .apk is "application/vnd.android.package-archive".
            // but Bluetooth does not accept this. Let's use "*/*" instead.
            intent.setType("*/*");


            // Append file and send Intent
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
            startActivity(Intent.createChooser(intent, "Share app via"));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (mainLinLay.getVisibility() == View.GONE) {
                mainLinLay.setVisibility(View.VISIBLE);
                mapFrameLay.setVisibility(View.GONE);
                if (map_fragment != null) {
                    getSupportFragmentManager().beginTransaction().remove(map_fragment).commit();
                    getSupportFragmentManager().popBackStack();
                    map_fragment = null;
                }
                return;
        }

        if(main_content.getVisibility() == View.GONE){
            main_content.setVisibility(View.VISIBLE);
            imageView2.setVisibility(View.GONE);
        }
        else if(myWebView.canGoBack()){
            if (currentUrl.equals(offline_url)) {
                openConfirmationExitApp();
            } else if (offline_url_flag){
                openConfirmationExitApp();
            } else {
                myWebView.goBack();
            }
        }
        else{
            if (currentUrl.equals(offline_url)) {
                openConfirmationExitApp();
                return;
            }
            finish();
        }
        //super.onBackPressed();
    }

    class WebAppInterface {
        Context mContext;
        WebAppInterface(Context c) {
            mContext = c;
        }
        @JavascriptInterface
        public void goToHome() {
            Log.d("stest","got ot home");
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    myWebView.loadUrl("file:///android_asset/app/index.html");
                }
            });
        }

        @JavascriptInterface
        public void goToCareer() {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    myWebView.loadUrl("file:///android_asset/app/career.html");
                }
            });
        }

        @JavascriptInterface
        public void emailSend() {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto","info@zohraexport.com", null));
//                    intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                    startActivity(Intent.createChooser(intent, "Choose an Email client :"));
                }
            });
        }

        @JavascriptInterface
        public void onClickCall(String mob) {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Intent callIntent = new Intent(Intent.ACTION_CALL); //use ACTION_CALL class
                    callIntent.setData(Uri.parse("tel:"+mob));    //this is the phone number calling
                    //check permission
                    //If the device is running Android 6.0 (API level 23) and the app's targetSdkVersion is 23 or higher,
                    //the system asks the user to grant approval.
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        //request permission from user if the app hasn't got the required permission
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CALL_PHONE},   //request specific permission from user
                                10);
                        return;
                    }else {     //have got permission
                        try{
                            startActivity(callIntent);  //call activity and make phone call
                        }
                        catch (android.content.ActivityNotFoundException ex){
                        }
                    }
                }
            });
        }


        @JavascriptInterface
        public void goToAbout() {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    myWebView.loadUrl("file:///android_asset/app/about-us.html");
                }
            });
        }

        @JavascriptInterface
        public void logout() {
            AppSettingSharePref.getInstance(MainActivity.this).setUsername("");
            AppSettingSharePref.getInstance(MainActivity.this).setUid("");
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    myWebView.loadUrl(login_url);
                }

            });

        }

        @JavascriptInterface
        public void loginUser(String Username, String uid) {
            AppSettingSharePref.getInstance(MainActivity.this).setUsername(Username);
            AppSettingSharePref.getInstance(MainActivity.this).setUid(uid);
            Log.d("iss", "Username = "+ Username + " uid = "+uid);
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
//                    showProgressLoad();
                    final JsonParserVolley jsonParserVolley = new JsonParserVolley(MainActivity.this);
                    jsonParserVolley.addParameter("username", Username);
                    jsonParserVolley.addParameter("token", AppSettingSharePref.getInstance(MainActivity.this).getToken());
                    Log.d("iss","Username="+Username);
                    jsonParserVolley.executeRequest(Request.Method.POST, WebUrl.REGISTER_TOKEN ,new JsonParserVolley.VolleyCallback() {
                                @Override
                                public void getResponse(String response) {
                                    try {
                                        startUpdatingUserLocation();
                                        hideProgressLoad();
                                        Log.d("iss","response="+response);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        hideProgressLoad();
                                    }
                                }
                            }
                    );
                }
            });


        }

        @JavascriptInterface
        public void goToMedia() {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    myWebView.loadUrl("file:///android_asset/app/media.html");
                }
            });
        }

        @JavascriptInterface
        public void goToFaq() {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    myWebView.loadUrl("file:///android_asset/app/faq.html");
                }
            });
        }

        @JavascriptInterface
        public void goToContact() {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    myWebView.loadUrl("file:///android_asset/app/contact-us.html");
                }
            });
        }

        @JavascriptInterface
        public void goToBuyerForm() {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    myWebView.loadUrl("file:///android_asset/app/quotation-form.html");
                }
            });
        }

        @JavascriptInterface
        public void goToLogin() {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    myWebView.loadUrl("file:///android_asset/app/login.html");
                }
            });
        }

        @JavascriptInterface
        public void goToPlayStore() {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=yourpackegName")); /// here "yourpackegName" from your app packeg Name
                    startActivity(intent);
                }
            });
        }



        @JavascriptInterface
        public void goToLocation() {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String strUri = "http://maps.google.com/maps?q=loc:" + 21.214504 + "," + 79.136894 + " (" + "Label which you want" + ")";
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(strUri));

                    intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");

                    startActivity(intent);
                }
            });
        }


        @JavascriptInterface
        public void setLoginUserId(String userId) {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    AppSettingSharePref.getInstance(mContext).setUid(userId);
                }
            });
        }

        @JavascriptInterface
        public void onClickWhatsAppCall(String mobNo) {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    openWhatsApp(mobNo);
                }
            });
        }

        @JavascriptInterface
        public void onClickSharing(String url) {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, url);
                    sendIntent.setType("text/plain");
                    Intent shareIntent = Intent.createChooser(sendIntent, null);
                    startActivity(shareIntent);
                }
            });
        }

        @JavascriptInterface
        public void onDownloadFileFromUrl(String url, String fileName) {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    downloadFile(url, fileName);
                }
            });
        }

        @JavascriptInterface
        public void goToWebsite(String url) {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
                    startActivity(intent);
                }
            });
        }




        @JavascriptInterface
        public void shareApplication(String msg, String app_name) {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    shareApplicationAndroid(msg, app_name);
                }
            });
        }


        @JavascriptInterface
        public void setUserPopup() {
            AppSettingSharePref.getInstance(MainActivity.this).setPopup();
        }

        @JavascriptInterface
        public void showAllVehicleTracking() {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    addMapFragment();
                }
            });
        }
    }


    private void askLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d("Location", "askLocationPermission: you should show an alert dialog...");
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationTrackingWorker();
    }

    private void downloadFile(String url, String fileName){

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,fileName)
                .setAllowedOverMetered(true);
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                 id =intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
                if (id == reference){
                    Toast.makeText(MainActivity.this, "file downloaded successfully", Toast.LENGTH_SHORT).show();
                }
            }
        };
        reference = manager.enqueue(request);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (mNetworkCallback != null && mConnectivityManager != null) {
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            }
        } catch (Exception e){ }
    }



    private void showProgressLoad(){
        pDialog = new ProgressDialog(MainActivity.this);
        pDialog.setMessage("Please Wait...");
        pDialog.setCancelable(false);
        pDialog.show();
    }
    private void hideProgressLoad(){
        if (pDialog != null){
            pDialog.dismiss();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
//                if (uploadMessage == null)
//                    return;
//                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
//                uploadMessage = null;

                if (requestCode != REQUEST_SELECT_FILE || uploadMessage == null) {
                    super.onActivityResult(requestCode, resultCode, intent);
                    return;
                }

                Uri[] results = null;

                // Check that the response is a good one
                if (resultCode == Activity.RESULT_OK) {
                    if (intent == null) {
                        // If there is not data, then we may have taken a photo
                        if (mCameraPhotoPath != null) {
                            results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }

                uploadMessage.onReceiveValue(results);
                uploadMessage = null;

            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage)
                return;
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            Uri result = intent == null || resultCode != MainActivity.RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        } else
            Toast.makeText(getApplicationContext(), "Failed to Upload Image", Toast.LENGTH_LONG).show();
    }


    private void openConfirmationExitApp() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        builder1.setTitle("Confirmation");
        builder1.setMessage("Are you sure want to exit Atoot Dor app?");
        builder1.setCancelable(false);

        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                });

        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }

    private void startUpdatingUserLocation(){

        workManagerUser = WorkManager.getInstance(getApplicationContext());
        workRequestUser = new PeriodicWorkRequest.Builder(UserUpdateLocationWorker.class, 15, TimeUnit.MINUTES).build();
        workManagerUser.enqueue(workRequestUser);
//        workManager.cancelWorkById(workRequest.getId());
    }


}
package com.persist.solution.atootdor.service;

import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.persist.solution.atootdor.MainActivity;
import com.persist.solution.atootdor.utils.AppSettingSharePref;
import com.persist.solution.atootdor.utils.DataResponse;
import com.persist.solution.atootdor.utils.JsonParserVolley;
import com.persist.solution.atootdor.utils.WebUrl;

public class UserUpdateLocationWorker extends Worker {

    public static boolean workedStopped = false;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    Context context;
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for(Location location: locationResult.getLocations()) {
                MainActivity.USER_LATITUDE = location.getLatitude();
                MainActivity.USER_LONGITUDE = location.getLongitude();
                Log.d("iss","USER_LATITUDE="+MainActivity.USER_LATITUDE+" MainActivity.USER_LONGITUDE="+MainActivity.USER_LONGITUDE);
            }
        }
    };

    public UserUpdateLocationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        this.context =context;
    }

    void updateLocation() throws InterruptedException {
        while(!workedStopped){
            Thread.sleep(5000);
            final JsonParserVolley jsonParserVolley = new JsonParserVolley(getApplicationContext());
            jsonParserVolley.addHeader("Authorization",  "Cw87c8ewHiR5AifXsWVW");
            jsonParserVolley.executeRequest(Request.Method.GET, WebUrl.GET_ALL_VEHICLE_LOCATION,new JsonParserVolley.VolleyCallback() {
                        @Override
                        public void getResponse(String response) throws InterruptedException {
                            try {
                                AppSettingSharePref.getInstance(getApplicationContext()).setDeviceList(response);
                                if(AppSettingSharePref.getInstance(getApplicationContext()).getOldDeviceList() == null || AppSettingSharePref.getInstance(getApplicationContext()).getOldDeviceList().equals("") ){
                                    AppSettingSharePref.getInstance(getApplicationContext()).setOldDeviceList(AppSettingSharePref.getInstance(getApplicationContext()).getDeviceList());
                                }
                                DataResponse locoNavPojo =   new Gson().fromJson( response, DataResponse.class );
                                updateUserLocation();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );

        }

    }


    private void updateUserLocation(){
        Log.d("iss", "Drivver = " + MainActivity.USER_LATITUDE + "  " +MainActivity.USER_LONGITUDE);
        if(MainActivity.USER_LATITUDE != 0 && MainActivity.USER_LONGITUDE != 0) {
            final JsonParserVolley jsonParserVolley = new JsonParserVolley(context);
            jsonParserVolley.addParameter("uid", AppSettingSharePref.getInstance(context).getUid());
            jsonParserVolley.addParameter("lat", MainActivity.USER_LATITUDE + "");
            jsonParserVolley.addParameter("lang", MainActivity.USER_LONGITUDE + "");
            jsonParserVolley.executeRequest(Request.Method.POST, WebUrl.CUSTOMER_UPDATE_LOCATION_URL, new JsonParserVolley.VolleyCallback() {
                        @Override
                        public void getResponse(String response) {

                            try {
                                Log.d("iss", "response custome location update=" + response);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );
        }
    }

    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void checkSettingsAndStartLocationUpdates() {
        Log.d("iss","checkSettingsAndStartLocationUpdates");
        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();
        SettingsClient client = LocationServices.getSettingsClient(context);

        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);
        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //Settings of device are satisfied and we can start location updates
                Log.d("iss","locationSettingsResponseTask");
                startLocationUpdates();
            }
        });
        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            checkSettingsAndStartLocationUpdates();
            updateLocation();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Result.success();
    }
}

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
import com.persist.solution.atootdor.MainActivity;
import com.persist.solution.atootdor.utils.AppSettingSharePref;
import com.persist.solution.atootdor.utils.JsonParserVolley;
import com.persist.solution.atootdor.utils.WebUrl;

public class DriverLocationUpdateWorker extends Worker {
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
                WebUrl.DRIVER_LATITUDE = location.getLatitude();
                WebUrl.DRIVER_LONGITUDE = location.getLongitude();
                Log.d("ne_loc","DRIVER_LATITUDE="+WebUrl.DRIVER_LATITUDE+" MainActivity.DRIVER_LONGITUDE="+WebUrl.DRIVER_LONGITUDE);
            }
        }
    };

    public DriverLocationUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
            Log.d("iss", "Drivver = " + WebUrl.DRIVER_LATITUDE + "  " +WebUrl.DRIVER_LONGITUDE);
            final JsonParserVolley jsonParserVolley = new JsonParserVolley(getApplicationContext());
            jsonParserVolley.addParameter("mobile", AppSettingSharePref.getInstance(getApplicationContext()).getDriverMob());
            jsonParserVolley.addParameter("lat", WebUrl.DRIVER_LATITUDE+"" );
            jsonParserVolley.addParameter("lang",WebUrl.DRIVER_LONGITUDE+"" );
            jsonParserVolley.executeRequest(Request.Method.POST, WebUrl.UPDATE_DRIVER_LOCATION ,new JsonParserVolley.VolleyCallback() {
                        @Override
                        public void getResponse(String response) {

                            try {
                                Log.d("iss","response="+response);
//                                JSONObject jsonObject=new JSONObject(response);
//                                int success=jsonObject.getInt("success");
//                                if(success==1){
//
//                                }
//                                else{
//                                    Toast.makeText(currentActivity,jsonObject.getString("message"),Toast.LENGTH_SHORT).show();
//                                }
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


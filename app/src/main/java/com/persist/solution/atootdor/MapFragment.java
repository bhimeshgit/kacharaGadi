package com.persist.solution.atootdor;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.persist.solution.atootdor.service.GetDriverLocationWorker;

import java.util.concurrent.TimeUnit;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    boolean locationPermission = false;

    float Bearing = 0;
    boolean AnimationStatus = false;
    static Marker carMarker;
    Bitmap BitMapMarker;

    public static double DRIVER_LATITUDE = 0;
    public static double DRIVER_LONGITUDE = 0;
    public static double LAST_DRIVER_LATITUDE = 0;
    public static double LAST_DRIVER_LONGITUDE = 0;
    private WorkManager workManager;
    private WorkRequest workRequest;
    public MapFragment() {
        // Required empty public constructor
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.car_marker);
        Bitmap b = bitmapdraw.getBitmap();
        BitMapMarker = Bitmap.createScaledBitmap(b, 110, 60, false);

        workManager = WorkManager.getInstance(getContext());
        workRequest = new PeriodicWorkRequest.Builder(GetDriverLocationWorker.class, 15, TimeUnit.MINUTES).build();
        workManager.enqueue(workRequest);


        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);

    }
    
    float getBearing(){
        double dLon = (DRIVER_LONGITUDE-LAST_DRIVER_LONGITUDE);
        double y = Math.sin(dLon) * Math.cos(DRIVER_LATITUDE);
        double x = Math.cos(LAST_DRIVER_LATITUDE)*Math.sin(DRIVER_LATITUDE) - Math.sin(LAST_DRIVER_LATITUDE)*Math.cos(DRIVER_LATITUDE)*Math.cos(dLon);
        double brng = Math.toDegrees((Math.atan2(y, x)));
        brng = (360 - ((brng + 360) % 360));
        return (float)brng;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        Thread t = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(10000);  //1000ms = 1 sec
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (DRIVER_LATITUDE!= 0 && DRIVER_LONGITUDE != 0) {
                                    if (carMarker == null) {
                                        LatLng latlng = new LatLng(DRIVER_LATITUDE, DRIVER_LONGITUDE);
                                        carMarker = mMap.addMarker(new MarkerOptions().position(latlng).
                                                flat(true).icon(BitmapDescriptorFactory.fromBitmap(BitMapMarker)));
                                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                                                latlng, 17f);
                                        mMap.animateCamera(cameraUpdate);
                                    }
                                    Bearing = getBearing();
                                    LatLng updatedLatLng = new LatLng(DRIVER_LATITUDE, DRIVER_LONGITUDE);
                                    Log.d("iss", "lst=" + LAST_DRIVER_LATITUDE + " lat=" + DRIVER_LATITUDE);
                                    changePositionSmoothly(carMarker, updatedLatLng, Bearing);
                                }
                            }
                        });

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        t.start();

    }

    void changePositionSmoothly(final Marker myMarker, final LatLng newLatLng, final Float bearing) {

        final LatLng startPosition = new LatLng(LAST_DRIVER_LATITUDE, LAST_DRIVER_LONGITUDE);
        final LatLng finalPosition = newLatLng;
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final float durationInMs = 3000;
        final boolean hideMarker = false;

        handler.post(new Runnable() {
            long elapsed;
            float t;
            float v;

            @Override
            public void run() {
                myMarker.setRotation(bearing);
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / durationInMs;
                v = interpolator.getInterpolation(t);

                LatLng currentPosition = new LatLng(
                        startPosition.latitude * (1 - t) + finalPosition.latitude * t,
                        startPosition.longitude * (1 - t) + finalPosition.longitude * t);

                myMarker.setPosition(currentPosition);

                // Repeat till progress is complete.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        myMarker.setVisible(false);
                    } else {
                        myMarker.setVisible(true);
                    }
                }

            }
        });
    }

}
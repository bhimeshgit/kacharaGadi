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
import android.widget.Button;

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
import com.persist.solution.atootdor.utils.WebUrl;

import java.util.concurrent.TimeUnit;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    boolean locationPermission = false;

    float Bearing = 0;
    boolean AnimationStatus = false;
    static Marker carMarker;
    Bitmap BitMapMarker;
    private Button startRideBtn;
    private WorkManager workManager;
    private WorkRequest workRequest;
    private Thread thread;
    private MainActivity mainActivity;
    public MapFragment(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
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

        startRideBtn = view.findViewById(R.id.startRideBtn);
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);

        startRideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.onBackPressed();
            }
        });

    }


    
    float getBearing(){
        double dLon = (WebUrl.DRIVER_LONGITUDE-WebUrl.LAST_DRIVER_LONGITUDE);
        double y = Math.sin(dLon) * Math.cos(WebUrl.DRIVER_LATITUDE);
        double x = Math.cos(WebUrl.LAST_DRIVER_LATITUDE)*Math.sin(WebUrl.DRIVER_LATITUDE) - Math.sin(WebUrl.LAST_DRIVER_LATITUDE)*Math.cos(WebUrl.DRIVER_LATITUDE)*Math.cos(dLon);
        double brng = Math.toDegrees((Math.atan2(y, x)));
        brng = (360 - ((brng + 360) % 360));
        return (float)brng;
    }

    private void createUserPositionMarker(){
        LatLng sydney = new LatLng(-33.852, 151.211);
        mMap.addMarker(new MarkerOptions()
                .position(sydney)
                .title("Pickup Location"));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        if(MainActivity.PICKUP_LATITUDE!=0 && MainActivity.PICKUP_LONGITUDE!=0) {
            createUserPositionMarker();
        }


        thread = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        if (MapFragment.this.isVisible() && getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (MapFragment.this.isVisible()) {
                                        if (WebUrl.DRIVER_LATITUDE != 0 && WebUrl.DRIVER_LONGITUDE != 0) {
                                            if (carMarker == null) {
                                                LatLng latlng = new LatLng(WebUrl.DRIVER_LATITUDE, WebUrl.DRIVER_LONGITUDE);
                                                carMarker = mMap.addMarker(new MarkerOptions().position(latlng).
                                                        flat(true).icon(BitmapDescriptorFactory.fromBitmap(BitMapMarker)));
                                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                                                        latlng, 17f);
                                                mMap.animateCamera(cameraUpdate);
                                            }
                                            Bearing = getBearing();
                                            LatLng updatedLatLng = new LatLng(WebUrl.DRIVER_LATITUDE, WebUrl.DRIVER_LONGITUDE);
                                            Log.d("iss", "lst=" + WebUrl.LAST_DRIVER_LATITUDE + " lat=" + WebUrl.DRIVER_LATITUDE);
                                            changePositionSmoothly(carMarker, updatedLatLng, Bearing);
                                        }
                                    }
                                }
                            });
                            Thread.sleep(5000);   //1000ms = 1 sec
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.start();

    }

    void changePositionSmoothly(final Marker myMarker, final LatLng newLatLng, final Float bearing) {

        final LatLng startPosition = new LatLng(WebUrl.LAST_DRIVER_LATITUDE, WebUrl.LAST_DRIVER_LONGITUDE);
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
package com.example.matej.location;

import android.Manifest;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

/**
 * Created by Matej on 20.3.2018.
 */

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{

    //VARIABLES
    private static final String TAG = "Location";

    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager mLocationManager;

    private Double lon;
    private Double lat;
    private String cityNameAndCountryCode;

    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 2 * 1000;  /* 2 secs */
    private long FASTEST_INTERVAL = 1000; /* 1   sec */

    private LocationManager locationManager;

    private TextView tvCoordinates;
    private TextView tvCityState;
    private Button btnGetLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  Get UI elements
        btnGetLoc = findViewById(R.id.btn_get_loc);
        tvCoordinates = findViewById(R.id.tv_lat_lon);
        tvCityState = findViewById(R.id.tv_city_state);

        //  Set click listener
        btnGetLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cityNameAndCountryCode == null){
                    if (mGoogleApiClient != null) {
                        mGoogleApiClient.connect();
                    }
                }else{
                    if (mGoogleApiClient.isConnected()) {
                        mGoogleApiClient.disconnect();
                    }
                }
            }
        });

        //  Setup API client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        //  Check if is GPS and Internet available
        checkLocation();
        checkInternet();
        mLocationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if(cityNameAndCountryCode==null){
            startLocationUpdates();
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mLocation == null){
            startLocationUpdates();
        }
        if (mLocation != null) {

        } else {
            Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        //  Setup an API client
        /*
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        */
    }

    @Override
    protected void onStop() {
        super.onStop();
        //  Disconnect API client
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
        Log.d("reque", "--->>>>");
    }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lon = location.getLongitude();

        String msg = "Updated Location: " + Double.toString(lat) + "," + Double.toString(lon);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        tvCoordinates.setText(Double.toString(lat) + "\n" + Double.toString(lon));

        try {
            translateCoordinates();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // You can now create a LatLng Object for use with maps
        LatLng latLng = new LatLng(lat, lon);
    }

    /*
    *   Returns availability of GPS
    *   Ask user -> Show alert dialog to turn up the GPS
    * */
    private boolean checkLocation() {
        //  Is GPS ON?
        if(!isLocationEnabled()){
            //  GPS is off -> show alert dialog to turn up the GPS
            showAlertGPS();
        }

        return isLocationEnabled();
    }

    /*
    *   Returns availability of Internet
    *   Ask user -> Show alert dialog to turn up the Internet
    * */
    private boolean checkInternet() {
        //  Is INTERNET ON?
        if(!isInternetEnabled()){
            //  Internet is off -> show alert dialog to turn up the internet
            showAlertInternet();
        }

        return isInternetEnabled();
    }

    /*
    *   Show alert dialog to turn up the GPS
    * */
    private void showAlertGPS() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setTitle(R.string.allert_gps_title)
                .setMessage(R.string.allert_gps_message_1 + "\n" + R.string.allert_gps_message_2)
                .setPositiveButton(R.string.allert_gps_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        //  Open Settings for GPS
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton(R.string.allert_gps_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        //  Dismiss
                    }
                });

        dialog.show();
    }

    /*
    *   Show alert dialog to turn up the Internet
    * */
    private void showAlertInternet() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setTitle(R.string.allert_internet_title)
                .setMessage(R.string.allert_internet_message_1 + "\n" + R.string.allert_internet_message_2)
                .setPositiveButton(R.string.allert_internet_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        //  Open Settings for Internet
                        Intent myIntent = new Intent(Settings.ACTION_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton(R.string.allert_internet_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        //  Dismiss
                    }
                });

        dialog.show();
    }

    /*
    *   Returns availability of GPS
    * */
    private boolean isLocationEnabled() {
        //  Return GPS status (ON/OFF)
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /*
    *   Returns availability of Internet
    * */
    private boolean isInternetEnabled() {
        //  Return INTERNET status (ON/OFF)
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final ConnectivityManager connectivityManager = ((ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    /*
    *   Translate Latitude & Longtitude to City name and Country code
    * */
    private void translateCoordinates() throws IOException{
        Geocoder geocoder = new Geocoder(getApplicationContext());
        try{
            List<Address> addresses = geocoder.getFromLocation(lat,lon,1);
            if (addresses.isEmpty()) {
                Toast.makeText(getApplicationContext(),"Lat" + lat,Toast.LENGTH_LONG).show();
                tvCityState.setText(R.string.waiting_for_location);
            }
            else {
                if (addresses.size() > 0) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        sb.append(address.getAddressLine(i)).append("\n");
                    }
                    sb.append(address.getLocality()).append("\n");
                    sb.append(address.getCountryCode());
                    cityNameAndCountryCode = sb.toString();
                    tvCityState.setText(cityNameAndCountryCode);

                    //vypnutie gpska
                    if (mGoogleApiClient.isConnected()) {
                        mGoogleApiClient.disconnect();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

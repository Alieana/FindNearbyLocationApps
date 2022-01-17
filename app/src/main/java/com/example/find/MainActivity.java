package com.example.find;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;


import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    //Initialize value
    Spinner spType;
    Button findBtn;
    SupportMapFragment supportMapFragment;
    GoogleMap map;
    FusedLocationProviderClient fusedLocationProviderClient;
    double currentLat = 0, currentLong = 0;
    protected static final String TAG = "LocationOnOff";
    private GoogleApiClient googleApiClient;
    final static int REQUEST_LOCATION = 199;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Assign variable
        spType = findViewById(R.id.sp_type);
        spType.setOnItemSelectedListener(this);
        findBtn = findViewById(R.id.findBtn);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.googleMap);

        //Initialize array of place keyword
        String[] placeKeywordList = {"Recycling Center", "Recycling bins", "Paper Recycling Center", "Bottle & Can Recycling Center", "Metal Recycling Center", "Waste Management Service"};
        //Initialize array of place name
        String[] placeNameList = {"Recycling Centre", "Recycling Bins", "Paper Recycle Centre", "Bottle & Can Recycle Centre", "Metal Recycle Centre", "Waste Management"};

        //Set adapter on spinner
        spType.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, placeNameList));
        spType.setSelection(0);

        //Initialize fused location provider
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //Check permission
        if (ActivityCompat.checkSelfPermission(MainActivity.this
                , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Check location on or off
            locationEnabled();
            //Get user current location
            getCurrentLocation();
        } else {
            //When permission denied
            //Request permission
            ActivityCompat.requestPermissions(MainActivity.this
                    , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }

        findBtn.setOnClickListener(v -> {
            //Get Current Location
            getCurrentLocation();
            //Get selected position of spinner
            //Find nearby location
            int i = spType.getSelectedItemPosition();
            //Initialize url
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" + //Url
                    "?keyword=" + placeKeywordList[i] +
                    "&location=" + currentLat + "," + currentLong + //Location latitude and longitude
                    "&radius=10000" + //Nearby radius
                    "&sensor=true" + //Sensor
                    "&key=" + getResources().getString(R.string.google_maps_key); //Google map key

            //Execute place task method to download json data
            new PlaceTask().execute(url);
        });
    }

    public void getCurrentLocation() {
        //When permission granted

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null){
                    //Get user current location
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            //When success
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            //Get current latitude
                            currentLat = location.getLatitude();
                            //Get current longitude
                            currentLong = location.getLongitude();
                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(latLng);
                            markerOptions.title("I'm here");
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                            map = googleMap;
                            //Zoom current location on map
                            map.addMarker(markerOptions);
                            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            map.animateCamera(CameraUpdateFactory.zoomTo(10));
                        }
                    });
                }
            }
        });
    }

    private void locationEnabled () {
        LocationManager lm = (LocationManager)
                getSystemService(Context. LOCATION_SERVICE ) ;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(MainActivity. this )
                    .setMessage( "Turn on your location" )
                    .setPositiveButton( "Settings" , new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                    startActivity( new Intent(Settings. ACTION_LOCATION_SOURCE_SETTINGS ));
                                }
                            })
                    .setNegativeButton( "Cancel" , null )
                    .show() ;
        }else {
            getCurrentLocation();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(parent.getContext(),
                "Select : " + parent.getItemAtPosition(position).toString(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @SuppressLint("StaticFieldLeak")
    private class PlaceTask extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String... strings) {
            String data = null;
            try {
                //Initialize data
                data = downloadUrl(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String s) {
            //Execute parser task
            new ParserTask().execute(s);
        }
    }

    private String downloadUrl(String string) throws IOException {
        //Initialize url
        URL url = new URL(string);
        //Initialize connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        //Connect connection
        connection.connect();
        //Initialize input stream
        InputStream stream = connection.getInputStream();
        //Initialize buffer reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        //Initialize string builder
        StringBuilder builder = new StringBuilder();
        //Initialize string variable
        String line;
        //Use while loop
        while ((line = reader.readLine()) != null){
            //Append line
            builder.append(line);
        }
        //Get append data
        String data = builder.toString();
        //Close reader
        reader.close();
        //Return data
        return data;
    }

        @SuppressLint("StaticFieldLeak")
        private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String, String>>> {
            @Override
            protected List<HashMap<String, String>> doInBackground(String... strings) {
                //Create json parser class
                JsonParser jsonParser = new JsonParser();
                //Initialize hash map list
                List<HashMap<String, String>> mapList = null;
                JSONObject object;
                try {
                    //Initialize json object
                    object = new JSONObject(strings[0]);
                    //Parser json object
                    mapList = jsonParser.parseResult(object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Return map list
                return mapList;
            }

            @Override
            protected void onPostExecute(List<HashMap<String, String>> hashMaps) {
                //Clear map
                map.clear();
                //Use for loop
                for (int i = 0; i < hashMaps.size(); i++) {
                    //Initialize hash map
                    HashMap<String, String> hashMapList = hashMaps.get(i);
                    //Get latitude
                    double lat = Double.parseDouble(Objects.requireNonNull(hashMapList.get("lat")));
                    //Get longitude
                    double lng = Double.parseDouble(Objects.requireNonNull(hashMapList.get("lng")));
                    //Get name
                    String name = hashMapList.get("name");
                    //Concat latitude and longitude
                    LatLng latLng = new LatLng(lat, lng);
                    //Initialize marker options
                    MarkerOptions options = new MarkerOptions();
                    //Set position
                    options.position(latLng);
                    //Set title
                    options.title(name);
                    //Add marker on map
                    map.addMarker(options);
                }
            }
        }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 44){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //When permission granted
                @SuppressLint("MissingPermission") Task<Location> task = fusedLocationProviderClient.getLastLocation();
                task.addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        locationEnabled();
                        if (location != null){
                            supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                                @Override
                                public void onMapReady(@NonNull GoogleMap googleMap) {
                                    //When success
                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    //Get current latitude
                                    currentLat = location.getLatitude();
                                    //Get current longitude
                                    currentLong = location.getLongitude();
                                    MarkerOptions markerOptions = new MarkerOptions();
                                    markerOptions.position(latLng);
                                    markerOptions.title("I'm here");
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                    map = googleMap;
                                    //Zoom current location on map
                                    map.addMarker(markerOptions);
                                    map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                    map.animateCamera(CameraUpdateFactory.zoomTo(10));
                                }
                            });
                        }
                    }
                });
            }
        }
    }
}
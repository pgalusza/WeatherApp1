package com.example.pawel.weatherapp;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements ConnectionCallbacks,
        OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    static final int SETTINGS_REQUEST = 1;

    static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 1;
    static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 2;
    static final int MY_PERMISSION_INTERNET = 3;

    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private LocationRequest mLocationRequest = null;

    protected WeatherClient weather = null;
    private String notificationMessage = "";
    private double latitude = 40.1164;
    private double longitude = -88.2434;
    String locationName = "";


    /**
     * Initializes some variables and ensures permissions have been granted.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make sure all permissions have been granted
        requestPermission(Manifest.permission.INTERNET, MY_PERMISSION_INTERNET);
        requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, MY_PERMISSION_ACCESS_FINE_LOCATION);
        requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION, MY_PERMISSION_ACCESS_COARSE_LOCATION);

        // Create an instance of GoogleAPIClient and a location request
        buildGoogleApiClient();
        createLocationRequest();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //getLocationCoords(locationName);

    }

    /**
     * Initializes mGoogleApiClient
     */
    protected synchronized void buildGoogleApiClient() {
        if(mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    /**
     * Initializes mLocationRequest
     */
    protected synchronized void createLocationRequest() {
        if(mLocationRequest == null) {
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setInterval(10 * 1000)
                    .setFastestInterval(1 * 1000);
        }
    }

    /**
     * Initializes Google Play Services connection on start.
     */
    @Override
    protected void onStart() {
        if(!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }

        super.onStart();
    }

    /**
     * Disconnects from Google Play on stop.
     */
    @Override
    protected void onStop() {
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    /**
     * When connecting to Google Play Services, checks if there is a recent location.
     * If one does not exist, requests that a fresh location be obtained.
     * @param connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("MainActivity", "onConnected()");
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, MY_PERMISSION_ACCESS_COARSE_LOCATION);
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.d("MainActivity", "onConnected() 2");

        if(mLastLocation == null) {
            Log.d("MainActivity", "mLastLocation is null");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            Log.d("MainActivity", "mLastLocation is null 2");
        } else {
            Log.d("MainActivity", "Location before update: " + latitude + ", " + longitude);
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            Log.d("MainActivity", "Location after update: " + latitude + ", " + longitude);

            if(weather == null) {
                handleLocation(mLastLocation);
            }
        }

        handleLocation(mLastLocation);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d("MainActivity", "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d("MainActivity", "GogleApiClient connection failed: " + result.toString());
    }

    // Updates the mLastLocation to the most recent (current) location.
    @Override
    public void onLocationChanged(Location location) {
        Log.d("MainActivity", "onLocationChanged()");
        try {
            mLastLocation.set(location);
            Log.d("MainActivity", "Location before update: " + latitude + ", " + longitude);
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            Log.d("MainActivity", "Location after update: " + latitude + ", " + longitude);

            if (weather == null) {
                handleLocation(mLastLocation);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Disconnects from Google Play on pause.
    @Override
    public void onPause() {
        Log.d("MainActivity", "onPause()");
        super.onPause();
        if(mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }



    /**
     * Checks if a permission is granted.  If it is not, requests the permission from the user.
     * @param permissionString
     * @param permissionCode
     */
    private void requestPermission(String permissionString, int permissionCode) {
        if(ContextCompat.checkSelfPermission(this, permissionString) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { permissionString }, permissionCode);
        }
    }

    /**
     * Respond to button press by going to settings.
     * @param view
     */
    public void goToSettings(View view) {
        Log.d("SETTINGSBUTTON", "goToSettings has been called");

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, SETTINGS_REQUEST);
    }

    /**
     * Posts a notification if the message exists
     * @param view
     */
    public void addNotification(View view) {
        if(notificationMessage != "") {
            Log.d("ADDNOTIFICATION", "addNotification has been called");
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.drawable.ic_notifications_black_24dp);
            builder.setContentTitle("Weather Notification");
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(notificationMessage));
            builder.setContentText(notificationMessage);

            int notificationId = 001;
            NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notifyMgr.notify(notificationId, builder.build());
        }
    }

    /**
     * Gets the locationName coordinates from the locationName name
     * @param latitude
     * @param longitude
     */
    private void setLocationName(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            Address returnAddress = addresses.get(0);
            Log.d("Address", latitude + "," + longitude + " " + returnAddress.getAddressLine(0));
            if (returnAddress != null) {
                locationName = "";
                if (returnAddress.getLocality() != null) {
                    locationName += returnAddress.getLocality() + ", ";
                }
                if (returnAddress.getAdminArea() != null) {
                    locationName += returnAddress.getAdminArea();
                    Log.d("Address", returnAddress.getCountryName());
                } else if (returnAddress.getCountryCode() != null) {
                    locationName += returnAddress.getCountryCode();
                }
            }else {
                Log.d("Address", "UNKNOWN PLACE");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Refresh the weather.
     */
    public void handleLocation(Location location) {
        if(location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }

        setLocationName(latitude, longitude);

        new RetrieveDataTask().execute();
    }


    public class RetrieveDataTask extends AsyncTask<Void, Void, WeatherClient> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected WeatherClient doInBackground(Void... urls) {
            if(weather == null) {
                weather = new WeatherClient(getFilesDir().getPath() + "/" + "weatherData.json", latitude, longitude);
            }

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

            if(weather.allData == null ||
                    sharedPref.getLong("last_update_time", 0) < System.currentTimeMillis() + 1000*60*60) {
                weather.updateData(latitude, longitude);
            }

            return weather;
        }

        @Override
        protected void onPostExecute(WeatherClient response) {
            if(response == null || response.allData == null) {
                Log.e("ERROR", "response is null");
            } else {
                Log.d("SUCCESS", "response is not null");

                weather = response;
                super.onPostExecute(response);
                setContentView(R.layout.activity_main);

                // Set weatherText
                TextView weatherText = (TextView) findViewById(R.id.weatherText);
                weatherText.setText(Double.toString(response.getCurrTemp()) + "F  " +
                        response.getCurrPrec());
                TextView locationText = (TextView) findViewById(R.id.locationText);
                locationText.setText(locationName);

                // Set update time
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong("last_update_time", System.currentTimeMillis());
                editor.commit();

                notificationMessage = checkTemperatures(sharedPref) + "  " + checkAllPrecipitation(sharedPref);
            }
        }
    }

    /**
     * Finds if the temperature will leave the comfortable range in the upcoming 24 hours
     * @param sharedPref
     * @return
     */
    private String checkTemperatures(SharedPreferences sharedPref) {
        String result = "";
        double minTemp = Double.parseDouble(sharedPref.getString("pref_min_temp", "50"));
        double maxTemp = Double.parseDouble(sharedPref.getString("pref_max_temp", "75"));

        if(weather.tempGreaterThan(maxTemp)) {
            result += "too hot";
            Log.d("TOOHOT", "weather is too hot");
        }
        if(weather.tempLessThan(minTemp)) {
            if(result.equals("")) {
                result += "too cold";
            } else {
                result += " and too cold";
            }
            Log.d("TOOCOLD", "weather is too cold");
        }

        if(result.equals(""))
            return "";
        else
            return "Today's temperature will be " + result +".";
    }


    /**
     * Finds which of the selected precipitation types are expected in the upcoming 24 hours
     * @param sharedPref
     * @return
     */
    protected String checkAllPrecipitation(SharedPreferences sharedPref) {
        String expectedPrecip = "";
        if(sharedPref.getBoolean("pref_fog", false) && weather.checkPrecipitation("Fog")) {
            if(expectedPrecip.equals("")) {
                expectedPrecip += "fog";
            } else {
                expectedPrecip += ", fog";
            }
        }
        if(sharedPref.getBoolean("pref_drizzle", false) && weather.checkPrecipitation("Drizzle")) {
            if(expectedPrecip.equals("")) {
                expectedPrecip += "drizzle";
            } else {
                expectedPrecip += ", drizzle";
            }
        }
        if(sharedPref.getBoolean("pref_rain", false) && weather.checkPrecipitation("Rain")) {
            if(expectedPrecip.equals("")) {
                expectedPrecip += "rain";
            } else {
                expectedPrecip += ", rain";
            }
        }
        if(sharedPref.getBoolean("pref_snow", false) && weather.checkPrecipitation("Snow")) {
            if(expectedPrecip.equals("")) {
                expectedPrecip += "snow";
            } else {
                expectedPrecip += ", snow";
            }
        }
        if(sharedPref.getBoolean("pref_hail", false) && weather.checkPrecipitation("Hail")) {
            if(expectedPrecip.equals("")) {
                expectedPrecip += "hail";
            } else {
                expectedPrecip += ", hail";
            }
        }

        if(expectedPrecip.equals(""))
            return "";
        else
            return "There is a chance of " + expectedPrecip + " today.";
    }
}


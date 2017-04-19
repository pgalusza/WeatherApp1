package com.example.pawel.weatherapp;

import android.app.NotificationManager;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    protected WeatherClient weather = null;
    private double minTemp = 0.0;
    private double maxTemp = 75.0;
    private String notificationMessage = "";
    private double longitude = 40.1164;
    private double latitude = -88.2434;
    String location = "Champaign, IL";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getLocationCoords(location);

        if(weather == null) {
            refreshWeather();
        }
    }

    /**
     * Respond to button press.
     * @param view
     */
    public void goToSettings(View view) {
        Log.d("SETTINGSBUTTON", "goToSettings has been called");

        Intent intent = new Intent(this, DisplayMessageActivity.class);
        startActivity(intent);
    }

    public void addNotification(View view) {
        Log.d("ADDNOTIFICATION", "addNotification has been called");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notifications_black_24dp);
        builder.setContentTitle("Weather Notification");
        builder.setContentText(notificationMessage);

        int notificationId = 001;
        NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifyMgr.notify(notificationId, builder.build());
    }

    private void getLocationCoords(String location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(location, 1);
            if(addresses.size() > 0) {
                Address address = addresses.get(0);
                double longitude = address.getLongitude();
                double latitude = address.getLatitude();
            }
            Log.d("Geocoder", Double.toString(longitude) + " " + Double.toString(latitude));
        } catch (Exception e) {
            Log.d("Geocoder", "Location not found");
        }
    }

    /*
    private void addNotification() {
        Log.d("ADDNOTIFICATION", "addNotification has been called");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notifications_black_24dp);
        builder.setContentTitle("Weather Notification");
        builder.setContentText(notificationMessage);

        int notificationId = 001;
        NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifyMgr.notify(notificationId, builder.build());
    }*/



    /**
     * Refresh the weather.
     */
    public void refreshWeather() {
        new RetrieveDataTask().execute();
        return;
    }

    public class RetrieveDataTask extends AsyncTask<Void, Void, WeatherClient> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected WeatherClient doInBackground(Void... urls) {
            weather = new WeatherClient(getFilesDir());
            weather.updateData(longitude, latitude);
            return weather;
        }

        @Override
        protected void onPostExecute(WeatherClient response) {
            if(response == null || response.allData == null) {
                Log.e("ERROR", "response is null");
            } else {
                weather = response;
                Log.d("SUCCESS", "response is not null");
                Log.d("SUCCESS", response.getCurrPrec());
                super.onPostExecute(response);
                setContentView(R.layout.activity_main);

                TextView weatherText = (TextView) findViewById(R.id.weatherText);
                weatherText.setText(Double.toString(response.getCurrTemp()) + "F  " +
                        response.getCurrPrec());
                TextView locationText = (TextView) findViewById(R.id.locationText);
                locationText.setText(location);

                if(weather.tempGreaterThan(maxTemp)) {
                    notificationMessage += "Weather will be too hot today.  ";
                    Log.d("TOOHOT", "weather is too hot");
                }
                if(weather.tempLessThan(minTemp)) {
                    notificationMessage += "Weather will be too cold today.  ";
                    Log.d("TOOCOLD", "weather is too cold");
                }
            }
        }
    }
}
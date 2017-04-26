package com.example.pawel.weatherapp;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static final int SETTINGS_REQUEST = 1;

    protected WeatherClient weather = null;
    private double minTemp = 10.0;
    private double maxTemp = 75.0;
    private String notificationMessage = "";
    private double longitude = 40.1164;
    private double latitude = -88.2434;
    String location = "Champaign, IL";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        getLocationCoords(location);

        if(weather == null) {
            refreshWeather();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SETTINGS_REQUEST) {
            if(resultCode == RESULT_OK) {

            }
        }
    }


    /**
     * Respond to button press.
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
     * Gets the location coordinates from the location name
     * @param location
     */
    private void getLocationCoords(String location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(location, 10);
            for(int i =0; i < addresses.size(); i++) {
                if (addresses.get(i) != null) {
                    Address address = addresses.get(i);
                    double longitude = address.getLongitude();
                    double latitude = address.getLatitude();
                    break;
                }
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
            if(weather == null) {
                weather = new WeatherClient(getFilesDir().getPath() + "/" + "weatherData.json", longitude, latitude);
            }

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            if(weather.allData == null ||
                    sharedPref.getLong("last_update_time", 0) < System.currentTimeMillis() + 1000*60*60) {
                weather.updateData(longitude, latitude);
            }

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

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong("last_update_time", System.currentTimeMillis());
                editor.commit();

                notificationMessage = checkTemperatures(sharedPref) + "  " + checkAllPrecipitation(sharedPref);
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
        private String checkAllPrecipitation(SharedPreferences sharedPref) {
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
}
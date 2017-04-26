package com.example.pawel.weatherapp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * A class for collecting weather data
 * Created by Pawel
 */

public class WeatherClient {
    private static final String DARKSKY_API_KEY = "29de5cf6598f94fbdc7eb4a295b8a089";

    private String ADDRESS = "https://api.darksky.net/forecast/" + DARKSKY_API_KEY + "/40.1164,-88.2434";
    public JSONObject allData;
    private File file;

    public WeatherClient(File file) {
        this.file = file;
    }

    /**
     * Gets new data from the Dark Sky API
     */
    public void updateData(double longitude, double latitude) {
        ADDRESS = "https://api.darksky.net/forecast/" + DARKSKY_API_KEY + "/" + Double.toString(longitude) + "," + Double.toString(latitude);
        Log.d("UPDATEDATA", "updateData is being executed");
        try {
            URL url = new URL(ADDRESS);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

            readStream(inputStream);

            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Reads the data from the input stream, stores in class variable.
     * @param inputStream
     */
    private void readStream(InputStream inputStream) {
        Log.d("READSTREAM", "readStream is being executed");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();
            Log.d("READSTREAM", "website has been read");

            allData = new JSONObject(line);
            Log.d("READSTREAM", "json array has been stored");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets current temperature from all data
     * @return
     */
    public double getCurrTemp() {
        double result = 666;
        try{
            result = allData.getJSONObject("currently").getDouble("temperature");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Get current precipitation from all data.
     * @return
     */
    public String getCurrPrec() {
        String result = "no data";
        try{
            result = allData.getJSONObject("currently").getString("summary");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * Returns true when the temperature exceeds a given value
     * @param max
     * @return
     */
    public boolean tempGreaterThan(double max) {
        Log.d("CONDITIONCHECK", "tempGreaterThan called");
        try {
            JSONArray hourlyData = allData.getJSONObject("hourly").getJSONArray("data");

            long currTime = (long) System.currentTimeMillis() / 1000;
            long maxTime = currTime + 60 * 60 * 24;
            for (int i = 0; i < hourlyData.length(); i++) {
                if (hourlyData.getJSONObject(i).getDouble("temperature") > max &&
                        hourlyData.getJSONObject(i).getInt("time") > currTime &&
                        hourlyData.getJSONObject(i).getInt("time") < maxTime)
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Returns true when the temperature drops below a given value
     * @param min
     * @return
     */
    public boolean tempLessThan(double min) {
        Log.d("CONDITIONCHECK", "tempLessThan called");
        try {
            JSONArray hourlyData = allData.getJSONObject("hourly").getJSONArray("data");
            Log.d("CONDITIONCHECK", "hourlyData collected");

            long currTime = (long) System.currentTimeMillis() / 1000;
            long maxTime = currTime + 60 * 60 * 24;
            Log.d("TIME", Long.toString(currTime) + " " + Long.toString(maxTime));
            for (int i = 0; i < hourlyData.length(); i++) {
                if (hourlyData.getJSONObject(i).getDouble("temperature") < min &&
                        hourlyData.getJSONObject(i).getInt("time") > currTime &&
                        hourlyData.getJSONObject(i).getInt("time") < maxTime)
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Checks to see if a given precipitation type is expected
     * @param type
     * @return
     */
    public boolean checkPrecipitation(String type) {
        Log.d("CONDITIONCHECK", "checkPrecipitation called");
        try {
            JSONArray hourlyData = allData.getJSONObject("hourly").getJSONArray("data");
            Log.d("CONDITIONCHECK", "hourlyData collected");

            long currTime = (long) System.currentTimeMillis() / 1000;
            long maxTime = currTime + 60 * 60 * 24;
            Log.d("TIME", Long.toString(currTime) + " " + Long.toString(maxTime));
            for (int i = 0; i < hourlyData.length(); i++) {
                if (hourlyData.getJSONObject(i).getString("summary").equals(type) &&
                        hourlyData.getJSONObject(i).getInt("time") > currTime &&
                        hourlyData.getJSONObject(i).getInt("time") < maxTime){
                    Log.d("CONDITIONCHECK", type + " expected");
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}

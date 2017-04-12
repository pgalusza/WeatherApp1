package com.example.pawel.weatherapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    protected WeatherClient weather = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(weather == null) {
            new RetrieveDataTask().execute();
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

    /**
     * Refresh the weather.
     */
    public void refreshWeather() {
        new RetrieveDataTask().execute();
    }

    public class RetrieveDataTask extends AsyncTask<Void, Void, WeatherClient> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected WeatherClient doInBackground(Void... urls) {
            weather = new WeatherClient(getFilesDir());
            weather.updateData();
            return weather;
        }

        @Override
        protected void onPostExecute(WeatherClient response) {
            if(response == null || response.allData == null) {
                Log.e("ERROR", "response is null");
            } else {
                Log.d("SUCCESS", "response is not null");
                Log.d("SUCCESS", response.getCurrPrec());
                super.onPostExecute(response);
                setContentView(R.layout.activity_main);

                TextView weatherText = (TextView) findViewById(R.id.weatherText);
                weatherText.setText(Double.toString(response.getCurrTemp()) + "F  " +
                        response.getCurrPrec());
            }
        }
    }
}
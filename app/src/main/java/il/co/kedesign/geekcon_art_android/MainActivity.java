//Copyright (c) Microsoft Corporation All rights reserved.
//
//MIT License:
//
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software.
//
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package il.co.kedesign.geekcon_art_android;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandCaloriesEvent;
import com.microsoft.band.sensors.BandCaloriesEventListener;
import com.microsoft.band.sensors.BandDistanceEvent;
import com.microsoft.band.sensors.BandDistanceEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.GsrSampleRate;

import java.util.Locale;

public class MainActivity extends Activity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private BandClient band;
    private TextView textViewHartRate;
    private TextView textViewCalories;
    private TextView textViewGsr;
    private TextView textViewSkinTemperature;
    private TextView textViewDistance;

    private BandHeartRateEventListener bandHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {

            if (event != null)
            {
                String text = String.format(Locale.getDefault(), "Heart Rate = %d beats per minute\n"
                        + "Quality = %s\n", event.getHeartRate(), event.getQuality());
                appendHartRate(text);
                Log.v(TAG, text);
            }
            else
            {
                Log.v(TAG, "onBandHeartRateChanged event is null");
            }
        }
    };

    private BandGsrEventListener bandGsrEventListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(BandGsrEvent event) {
            if (event != null)
            {
                String text = String.format(Locale.getDefault(), "Resistance = %d\n", event.getResistance());
                appendGrs(text);
                Log.v(TAG, text);
            }
            else
            {
                Log.v(TAG, "onBandGsrChanged event is null");
            }
        }
    };

    private BandSkinTemperatureEventListener bandSkinTemperatureEventListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent event) {
            if (event != null) {
                String text = String.format(Locale.getDefault(), "SkinTemperature: Temp = %f \n",
                        event.getTemperature());
                appendSkinTemperature(text);
                Log.v(TAG, text);
            }
            else
            {
                Log.v(TAG, "onBandSkinTemperatureChanged event is null");
            }
        }
    };

    private BandCaloriesEventListener bandCaloriesEventListener = new BandCaloriesEventListener() {
        @Override
        public void onBandCaloriesChanged(BandCaloriesEvent event) {
            if (event != null) {
                String text = String.format(Locale.getDefault(), "Calories = %d\n",
                        event.getCalories());
                appendCalories(text);
                Log.v(TAG, text);
            }
            else
            {
                Log.v(TAG, "onBandCaloriesChanged event is null");
            }
        }
    };

    private BandDistanceEventListener bandDistanceEventListener = new BandDistanceEventListener() {
        @Override
        public void onBandDistanceChanged(BandDistanceEvent event) {
            if (event != null) {
                String text = String.format(Locale.getDefault(), "MotionType = %s\n",
                        event.getMotionType());
                appendDistance(text);
                Log.v(TAG, text);
            }
            else
            {
                Log.v(TAG, "onBandCaloriesChanged event is null");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewHartRate = (TextView) findViewById(R.id.textViewHartRate);
        textViewGsr = (TextView) findViewById(R.id.textViewGsr);
        textViewSkinTemperature = (TextView) findViewById(R.id.textViewSkinTemperature);
        textViewCalories = (TextView) findViewById(R.id.textViewCalories);
        textViewDistance = (TextView) findViewById(R.id.textViewDistance);


        Button btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new SubscriptionTask().execute();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        new SubscriptionTask().execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
        if (band != null) {
            try
            {
                band.getSensorManager().unregisterAllListeners();
            }
            catch (BandIOException e)
            {
                Log.wtf(TAG, e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (band != null) {
            try {
                band.disconnect().await();
                Log.v(TAG, "Band disconnected.\n");
            }
            catch (InterruptedException | BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    private class SubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    if (band.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED)
                    {
                        band.getSensorManager().registerHeartRateEventListener(bandHeartRateEventListener);
                        band.getSensorManager().registerGsrEventListener(bandGsrEventListener, GsrSampleRate.MS5000);
                        band.getSensorManager().registerSkinTemperatureEventListener(bandSkinTemperatureEventListener);
                        band.getSensorManager().registerCaloriesEventListener(bandCaloriesEventListener);
                        band.getSensorManager().registerDistanceEventListener(bandDistanceEventListener);
                    }
                    else
                    {
                        Log.wtf(TAG, "You have not given this application consent to access heart rate data yet."
                                + " Please press the Heart Rate Consent button.\n");
                    }
                } else {
                    Log.wtf(TAG, "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage;
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                Log.wtf(TAG, exceptionMessage);

            } catch (Exception e) {
                Log.wtf(TAG, e.getMessage());
            }
            return null;
        }
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (band == null) {
            Log.v(TAG, "Band new connection.\n");
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                Log.wtf(TAG, "Band isn't paired with your phone.\n");
                return false;
            }
            band = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == band.getConnectionState()) {
            Log.v(TAG, "Band has connection.\n");
            return true;
        }

        Log.wtf(TAG, "Band is connecting...\n");
        boolean b = ConnectionState.CONNECTED == band.connect().await();
        if (b) {
            Log.wtf(TAG, "Connected.\n");
        }
        return b;
    }


    private void appendHartRate(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewHartRate.setText(string);
            }
        });
    }

    private void appendGrs(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewGsr.setText(string);
            }
        });
    }

    private void appendSkinTemperature(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewSkinTemperature.setText(string);
            }
        });
    }

    private void appendCalories(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewCalories.setText(string);
            }
        });
    }

    private void appendDistance(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewDistance.setText(string);
            }
        });
    }


}

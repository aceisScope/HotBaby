package com.example.binghui.hotbaby;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public static Camera camera = null;// has to be static, otherwise onDestroy() destroys it
    private RegexThread[] regexThreads;
    private CPUTask cpuTask;
    private NetworkTask networkTask;
    private BroadcastReceiver wifiReceiver;
    private LocationListener locationListener;
    public static final String TAG = "Hot Baby";
    private boolean running = false;

    private CPUTask[] cpuTasks;
    private NetworkTask[] networkTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                wifiManager.getScanResults();
                wifiManager.startScan();
            }
        };

        final Button play = (Button) findViewById(R.id.start_button);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (running) {
                    running = false;
                    play.setText(R.string.start);
                    stop();
                } else {
                    running = true;
                    play.setText(R.string.stop);
                    start();
                }
            }
        });
    }

    private void start() {
        fullBrightness();
        startLocation();
        startWifiScanning();
        flashLightOn();
        burnCPU();
        startNetwork();
    }

    private void stop() {
        restBrightness();
        stopLocation();
        stopWifiScanning();
        flashLightOff();
        stopCPU();
        stopNetwork();
    }


    private void fullBrightness() {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = 1F;
        getWindow().setAttributes(layout);
    }

    private void startLocation() {
        // Acquire a reference to the system Location Manager
        final LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Retrieve information about current GPS status
                locationManager.getGpsStatus(null);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Retrieve information about current GPS status
                locationManager.getGpsStatus(null);
            }

            public void onProviderEnabled(String provider) {
                // Retrieve information about current GPS status
                locationManager.getGpsStatus(null);
            }

            public void onProviderDisabled(String provider) {
                // Retrieve information about current GPS status
                locationManager.getGpsStatus(null);
            }
        };

        // Register the listener with the Location Manager to receive location updates
        for (String provider : locationManager.getAllProviders()) {
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

            locationManager.requestLocationUpdates(provider, 0, 0, locationListener);
        }

    }

    private void startWifiScanning() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();

        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    // deprecated from lollipop
    private void flashLightOn() {

        try {
            if (getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH)) {
                camera = Camera.open();
                Camera.Parameters p = camera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(p);
                camera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Exception flashLightOn()",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // http://stackoverflow.com/questions/7396766/how-can-i-stress-my-phones-cpu-programatically
    private void burnCPU() {

        /*
        int NUM_THREADS = 10; // run 10 threads

        regexThreads = new RegexThread [NUM_THREADS];

        for(int i = 0; i < NUM_THREADS; ++i) {
            regexThreads[i] = new RegexThread(); // create a new thread
        }
        */

//        cpuTask = new CPUTask();
//        cpuTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);



        cpuTasks = new CPUTask[4];
        for (int i = 0; i < cpuTasks.length; ++i) {
            cpuTasks[i] = new CPUTask();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                try {
                    cpuTasks[i].executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    continue;
                } catch (RejectedExecutionException e) {
                    // falls through
                }
            }
            cpuTasks[i].execute();
        }
    }

    private void startNetwork() {
        networkTask = new NetworkTask();
        networkTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    //-------------------------------------------------
    // turn off
    private void restBrightness() {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = -1;
        getWindow().setAttributes(layout);
    }

    private void flashLightOff() {
        try {
            if (getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH)) {
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Exception flashLightOff",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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
        locationManager.removeUpdates(locationListener);
    }

    private void stopWifiScanning() {
        unregisterReceiver(wifiReceiver);
    }

    private void stopCPU() {
        /*
        for (int i = 0; i < regexThreads.length; i++) {
            regexThreads[i].terminate();
        }
        */

        //cpuTask.cancel(true);

        for (int i = 0; i < cpuTasks.length; ++i) {
            cpuTasks[i].cancel(true);
        }
    }

    private void stopNetwork() {
        networkTask.cancel(true);
    }

    //-----helper class-----------------------------------
    class RegexThread extends Thread {

        private volatile boolean running = true;

        RegexThread() {
            // Create a new, second thread
            super("Regex Thread");
            start(); // Start the thread
        }

        // This is the entry point for the second thread.
        public void run() {
            while(running) {
                Pattern p = Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+(?:[A-Z]{2}|com|org|net|edu|gov|mil|biz|info|mobi|name|aero|asia|jobs|museum)\b");
            }
        }

        public void terminate() {
            running = false;
        }
    }

    // try cpu task from another approach
    private static class CPUTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params)
        {
            while (true)
            {
                if (isCancelled()) {
                    break;
                }

                Pattern p = Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+(?:[A-Z]{2}|com|org|net|edu|gov|mil|biz|info|mobi|name|aero|asia|jobs|museum)\b");
            }
            return null;
        }
    }

    // network task
    private static class NetworkTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            while (true) {
                if (isCancelled()) {
                    break;
                }

                try {
                    HttpURLConnection httpConnection = (HttpURLConnection) new URL("http://www.theverge.com/").openConnection();
                    BufferedInputStream is = new BufferedInputStream(httpConnection.getInputStream());
                    int read;
                    byte[] response = new byte[0];
                    final byte[] buffer = new byte[2048];
                    while ((read = is.read(buffer)) > -1) {
                        byte[] tmp = new byte[response.length + read];
                        System.arraycopy(response, 0, tmp, 0, response.length);
                        System.arraycopy(buffer, 0, tmp, response.length, read);
                        response = tmp;
                    }
                    //System.out.println("--> " + new String(response));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }


}

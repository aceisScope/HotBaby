package hut.hotbaby;

import android.app.Activity;
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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.util.regex.Pattern;


public class MainActivity extends Activity {

    public static Camera camera = null;// has to be static, otherwise onDestroy() destroys it
    private RegexThread[] regexThreads;
//    private NetworkTask[] networkTasks;
    private BroadcastReceiver wifiReceiver;
    private LocationListener locationListener;
    public static final String TAG = "Hot Baby";
    private boolean running = false;

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
                if(running) {
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void start(){
        fullBrightness();
        startLocation();
        startWifiScanning();
        flashLightOn();
        burnCPU();
    }

    private void stop() {
        restBrightness();
        stopLocation();
        stopWifiScanning();
        flashLightOff();
        stopCPU();
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
        int NUM_THREADS = 10; // run 10 threads

        regexThreads = new RegexThread [NUM_THREADS];

        for(int i = 0; i < NUM_THREADS; ++i) {
            regexThreads[i] = new RegexThread(); // create a new thread
        }
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
        locationManager.removeUpdates(locationListener);
    }

    private void stopWifiScanning() {
        unregisterReceiver(wifiReceiver);
    }

    private void stopCPU() {
        for (int i = 0; i < regexThreads.length; i++) {
            regexThreads[i].terminate();
        }
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

}

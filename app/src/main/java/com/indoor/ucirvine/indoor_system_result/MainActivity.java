package com.indoor.ucirvine.indoor_system_result;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;

    private int progressStatus = 0;
    private Handler handler = new Handler();

    ListView listview;
    Adapter_Rssi adapter;

    TextView device1;
    TextView device2;
    TextView device3;

    TextView device1_distance;
    TextView device2_distance;
    TextView device3_distance;
    // every 3s restart
    private Handler mHandlerToRestart;
    private Runnable mRunableToRestart;

    //play music in background
    private Handler mHandlerToMusic;
    private Runnable mRunableToMusic;
    private Runnable mRunableToMusic2;
    private Runnable mRunableToMusic3;


    int avg1[] = new int[8];
    int avg2[] = new int[8];
    int avg3[] = new int[8];
    int avg4[] = new int[8];

    int size1 = 0;
    int size2 = 0;
    int size3 = 0;
    int size4 = 0;

    double result1 = 0;
    double result2 = 0;
    double result3 = 0;
    double result4 = 0;

    boolean full1 = false;
    boolean full2 = false;
    boolean full3 = false;
    boolean full4 = false;


    boolean music1 = false;
    boolean music2 = false;
    boolean music3 = false;

    Uri myUri = Uri.parse("file:///sdcard/Download/1.mp3"); // initialize Uri here
    Uri myUri2 = Uri.parse("file:///sdcard/Download/2.mp3"); // initialize Uri here
    Uri myUri3 = Uri.parse("file:///sdcard/Download/3.mp3"); // initialize Uri here

    MediaPlayer mediaPlayer = new MediaPlayer();

    //TODO predict value
    double predict_x = 0;
    double predict_p = 0.1;
    double consitant_r = 0.1;

    double distance;


    private final static int REQUEST_ENABLE_BT = 1;

    ProgressBar pb_default;

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        pb_default = (ProgressBar) findViewById(R.id.pb_default);

        device1 = (TextView) findViewById(R.id.device1);
        device2 = (TextView) findViewById(R.id.device2);
        device3 = (TextView) findViewById(R.id.device3);

        device1_distance = (TextView) findViewById(R.id.device1_distance);
        device2_distance = (TextView) findViewById(R.id.device2_distance);
        device3_distance = (TextView) findViewById(R.id.device3_distance);

        adapter = new Adapter_Rssi();
//        listview = (ListView) findViewById(R.id.item_list);
//        listview.setAdapter(adapter);


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mBluetoothAdapter.startLeScan(mLeScanCallback);


        mRunableToRestart = new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mHandlerToRestart.postDelayed(mRunableToRestart, 3000);
                Log.e("isGood", "working");

            }
        };
        //play music in background
        mRunableToMusic = new Runnable() {
            @Override
            public void run() {
                MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                try {
                    mediaPlayer.setDataSource(getApplicationContext(), myUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaPlayer.start();
            }
        };

        mHandlerToRestart = new Handler();
        mHandlerToRestart.postDelayed(mRunableToRestart, 3000);

        mHandlerToMusic = new Handler();

        progressStatus = 0;

    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                double d1 = 0, d2 = 0, d3 = 0, d4 = 0;
                double x = 0, y = 0;

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (device.getAddress().equals("B8:27:EB:A6:A1:E9") || device.getAddress().equals("B8:27:EB:26:28:F4") || device.getAddress().equals("B8:27:EB:25:31:D6") || device.getAddress().equals("B8:27:EB:3A:91:F4")) {

                                Long tsLong = System.currentTimeMillis() / 1000;
                                String ts = tsLong.toString();

                                //device 1
                                if (device.getAddress().equals("B8:27:EB:A6:A1:E9")) {
                                    double a = -26.01613193, b = -25.60966355, y = rssi - (-25.1716666667);

                                    avg1[size1++] = rssi;

                                    if (size1 > 7) {
                                        full1 = true;
                                        size1 = 0;
                                    }

                                    if (full1) {
                                        result1 = (avg1[0] + avg1[1] + avg1[2] + avg1[3]
                                                + avg1[4] + avg1[5] + avg1[6] + avg1[7]) / 8.0;
                                        y = result1 - (-25.1716666667);


                                        d1 = (y - b) / a;
                                        d1 = Math.pow(10, d1);

                                        device1.setText("" + result1);
                                        device1_distance.setText(" dis " + d1);

                                    }
                                    adapter.notifyDataSetChanged();
                                }

                                //device 2
                                if (device.getAddress().equals("B8:27:EB:26:28:F4")) {

                                    double a = -27.37390491, b = -26.44442921, y = rssi - (-25.6375);

                                    avg2[size2++] = rssi;

                                    if (size2 > 7) {
                                        full2 = true;
                                        size2 = 0;
                                    }

                                    if (full2) {
                                        result2 = (avg2[0] + avg2[1] + avg2[2] + avg2[3]
                                                + avg2[4] + avg2[5] + avg2[6] + avg2[7]) / 8.0;
                                        y = result2 - (-25.6375);


                                        d2 = (y - b) / a;
                                        d2 = Math.pow(10, d2);


                                        device2.setText("" + result2);
                                        device2_distance.setText(" dis " + d2);

                                    }
                                    adapter.notifyDataSetChanged();
                                }

                                //device 3
                                if (device.getAddress().equals("B8:27:EB:25:31:D6")) {

                                    //double a = -23.13184903, b = -25.48251426 , y = rssi - (-25.8245833333) ;
                                    double a = -22.94102589, b = -24.71862505, y = rssi - (-25.1366666667);

                                    avg3[size3++] = rssi;

                                    if (size3 > 7) {
                                        full3 = true;
                                        size3 = 0;
                                    }

                                    if (full3) {
                                        result3 = (avg3[0] + avg3[1] + avg3[2] + avg3[3]
                                                + avg3[4] + avg3[5] + avg3[6] + avg3[7]) / 8.0;

                                        y = result3 - (-25.8245833333);


                                        d3 = (y - b) / a;
                                        d3 = Math.pow(10, d3);

                                        Log.e("dd", "dd");
                                        device1.setText("" + result3);
                                        device1_distance.setText(" dis " + d3);

                                    }
                                    adapter.notifyDataSetChanged();
                                }

                                //device 4
                                if (device.getAddress().equals("B8:27:EB:3A:91:F4")) {

                                    double a = -22.94102589, b = -24.71862505, y = rssi - (-25.1366666667);

                                    avg4[size4++] = rssi;

                                    if (size4 > 7) {
                                        full4 = true;
                                        size4 = 0;
                                    }

                                    if (full4) {
                                        result4 = (avg4[0] + avg4[1] + avg4[2] + avg4[3]
                                                + avg4[4] + avg4[5] + avg4[6] + avg4[7]) / 8.0;
                                        y = result4 - (-25.1366666667);


                                        d4 = (y - b) / a;
                                        d4 = Math.pow(10, d4);

                                    }
                                    adapter.notifyDataSetChanged();

                                }

                                //TODO calculate the distance

                                //weight
                                double w1, w2;

                                w1 = Math.exp(-d2);
                                w2 = Math.exp(-d3);

                                if (d2 > 0.0 && d3 > 0.0) {
                                    distance = ((3.0 - d2) * w1 + d3 * w2) / (w1 + w2);

                                    double update_g = predict_p / (predict_p + consitant_r);
                                    predict_x = predict_x + update_g * (distance - predict_x);
                                    predict_p = (1 - update_g) * predict_p;

                                    adapter.addItem("yyg", "result", "" + ts, "" + rssi, d2 + " " + d3 + " " + distance + " " + predict_x);
                                    device3.setText("" + distance);

                                    if (0 < distance && distance < 1 && !music1) {
                                        music1 = true;
                                        music2 = false;
                                        music3 = false;

                                        mediaPlayer.stop();
                                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                                        try {
                                            mediaPlayer.setDataSource(getApplicationContext(), myUri);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            mediaPlayer.prepare();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        mediaPlayer.start();

                                    } else if (1 < distance && distance < 2 && !music2) {
                                        music1 = false;
                                        music2 = true;
                                        music3 = false;

                                        mediaPlayer.stop();
                                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                                        try {
                                            mediaPlayer.setDataSource(getApplicationContext(), myUri2);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            mediaPlayer.prepare();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        mediaPlayer.start();

                                    } else if (2 < distance && distance < 3 && !music3) {
                                        music1 = false;
                                        music2 = false;
                                        music3 = true;

                                        mediaPlayer.stop();
                                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                                        try {
                                            mediaPlayer.setDataSource(getApplicationContext(), myUri3);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            mediaPlayer.prepare();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        mediaPlayer.start();

                                    }

                                    device3_distance.setText("" + predict_x);

                                    pb_default.setProgress((int) (distance * 100));

                                    adapter.notifyDataSetChanged();

                                }
                            }
                        }
                    });
                }
            };
}


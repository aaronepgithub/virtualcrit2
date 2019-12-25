package com.aaronep.andy.antbletest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private static final UUID CSC_SERVICE_UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    private static final UUID CSC_CHARACTERISTIC_UUID = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
    private static final UUID BTLE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final long SCAN_PERIOD = 3000;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    //SERVICE VARS / SERVICE MANAGEMENT
    private BluetoothLeService mBluetoothLeService;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.i(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(TAG, "onReceive: Broadcast Receiver");
            


            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i(TAG, "onReceive: ACTION_GATT_CONNECTED  " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //displayData("ACTION_GATT_CONNECTED", intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                displayData("CONNECTED");
                refreshDevicesList();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i(TAG, "onReceive: ACTION_GATT_DISCONNECTED  " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //displayData("ACTION_GATT_DISCONNECTED", intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //displayDataDISCONNECTED(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                displayData("DISCONNECTED");
                refreshDevicesList();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.i(TAG, "onReceive: ACTION_GATT_SERVICES_DISCOVERED");
                displayData("SVC DISCO");
                //displayData("ACTION_GATT_SERVICES_DISCOVERED");
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.i(TAG, "onReceive: ACTION DATA AVAILABLE");
                //displayData("ACTION_DATA_AVAILABLE  " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_HR.equals(action)) {
                Log.i(TAG, "onReceive: ACTION DATA AVAILABLE_HR");
                displayDataHR(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_SPD.equals(action)) {
                Log.i(TAG, "onReceive: ACTION DATA AVAILABLE_SPD");
                displayDataSPD(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_CAD.equals(action)) {
                Log.i(TAG, "onReceive: ACTION DATA AVAILABLE_CAD");
                displayDataCAD(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_DISTANCE.equals(action)) {
                Log.i(TAG, "onReceive: ACTION DATA AVAILABLE_DISTANCE");
                displayDataDISTANCE(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE_WHEEL.equals(action)) {
                Log.i(TAG, "onReceive: ACTION DATA AVAILABLE_WHEEL");
                displayDataWHEEL(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }

        }
    };

    private void displayData(final String data) {
        if (data != null) {
            Log.i(TAG, "displayData: " + data);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Button btn1 = (Button) findViewById(R.id.button1);
                    btn1.setText(data);
                }
            });
        }
    }

    private void displayData(final String data1, final String data2) {
        if (data1 != null) {
            Log.i(TAG, "displayData1: " + data1);
        }
        if (data2 != null) {
            Log.i(TAG, "displayData2: " + data2);
        }
    }




    private void displayDataHR(final String data) {
        if (data != null) {
        Log.i(TAG, "displayDataHR: " + data);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tview10 = (TextView) findViewById(R.id.TextView10);
                tview10.setText(data);
            }
        });
        }
    }

    private void displayDataSPD(final String data) {
        if (data != null) {
            Log.i(TAG, "displayDataSPD: " + data);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tview11 = (TextView) findViewById(R.id.TextView11);
                    tview11.setText(data);
                }
            });
        }
    }

    private void displayDataCAD(final String data) {
        if (data != null) {
            Log.i(TAG, "displayDataCAD: " + data);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tview12 = (TextView) findViewById(R.id.TextView12);
                    tview12.setText(data);
                }
            });
        }
    }

    private void displayDataDISTANCE(final String data) {
        if (data != null) {
            Log.i(TAG, "displayDataDISTANCE: " + data);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tview13 = (TextView) findViewById(R.id.TextView13);
                    tview13.setText(data);
                }
            });
        }
    }

        private void displayDataWHEEL(final String data) {
            if (data != null) {
                Log.i(TAG, "displayDataWHEEL: " + data);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        TextView tview13 = (TextView) findViewById(R.id.TextView13);
//                        tview13.setText(data);
                        Button btn = (Button) findViewById(R.id.button0);
                        btn.setText(data);
                    }
                });
            }
        }

    private void displayDataDISCONNECTED(final String nameOfDisconnectedDevice) {
        if (nameOfDisconnectedDevice != null) {
            Log.i(TAG, "displayDataDISCONNECTED: name:  " + nameOfDisconnectedDevice);
        }
    }

    //END SERVICE MANAGEMENT


    private TextView mTextMessage;
    private Button btn0;
    private Button btn1;
    private Button btn2;


    ListView listView ;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        btn0 = (Button) findViewById(R.id.button0);
        btn1 = (Button) findViewById(R.id.button1);
        btn2 = (Button) findViewById(R.id.button2);


        btn0.setText("SCAN");
        btn1.setText("DISCONNECT");
        btn2.setVisibility(View.GONE);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //Log.i(TAG, "onCreate: getSizeOfDevicesDiscovered: " + mBluetoothLeService.getSizeOfDevicesDiscovered());
    }


    private int localCounter = 0;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        //refreshDevicesList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_HR);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_SPD);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_CAD);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_DISTANCE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE_WHEEL);
        return intentFilter;
    }



    //SCAN CALLBACK mScanCallback
    private ArrayList<BluetoothDevice> devicesDiscoveredX = new ArrayList<>();
    private ArrayList<BluetoothDevice> devicesConnectedX = new ArrayList<>();
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice deviceDiscovered = result.getDevice();
            if (deviceDiscovered.getName() != null) {

                if (!mBluetoothLeService.getDevicesDiscovered().contains(deviceDiscovered)) {
                    mBluetoothLeService.addDeviceDiscovered(deviceDiscovered);
                    Log.i(TAG, "onScanResult: " + deviceDiscovered.getName());
                    localCounter += 1;
                    Log.i(TAG, "onScanResult: localCounter:  " + localCounter);

                }
            }
            Log.i(TAG, "onScanResult: getSizeOfDevicesDiscovered:  " + mBluetoothLeService.getSizeOfDevicesDiscovered());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.i(TAG, "onBatchScanResults: " + results.toString());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i(TAG, "onScanFailed: " + errorCode);
        }
    };
    //END SCAN CB

    private BluetoothGatt mGatt0;
    private BluetoothGatt mGatt1;
    private BluetoothGatt mGatt2;
    private BluetoothGatt mGatt3;
    private BluetoothGatt mGatt4;
    private BluetoothGatt mGatt5;



    private void connectToBtDevice(Integer indexValue, BluetoothDevice indexDevice, String indexDeviceAddress) {
        mBluetoothLeService.connectToBtDevice(indexValue, indexDeviceAddress);
    }


    public void onClick_0(View view) {
        Log.i(TAG, "onClick_0:  SCANNING");

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "PROMPT FOR LOCATION ENABLED");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        Log.i(TAG, "CHECK FOR BT ENABLED");
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();
            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString("0000180D-0000-1000-8000-00805f9b34fb"))
                    .build();
            filters.add(scanFilter);
            ScanFilter scanFilter2 = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString("00001816-0000-1000-8000-00805f9b34fb"))
                    .build();
            filters.add(scanFilter2);

            // Show Alert
            Toast.makeText(getApplicationContext(),
                    "SCANNING" , Toast.LENGTH_SHORT)
                    .show();

            //START SCAN
            Log.i(TAG, "START SCANNING");
            mLEScanner.startScan(filters, settings, mScanCallback);
            Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                    Log.i(TAG, "run: STOP SCANNING");
                    if (mBluetoothLeService.getSizeOfDevicesDiscovered() > 0) {
                        Log.i(TAG, "devicesDiscovered.size = " + mBluetoothLeService.getSizeOfDevicesDiscovered());
                        refreshDevicesList();
                    } else {
                        Log.i(TAG, "NO DEVICES DISCOVERED");
                        btn0.setText("SCAN");
                    }

                }
            }, SCAN_PERIOD);

        }

    }


    public void onClick_1(View view) {
        Log.i(TAG, "onClick_1");
        mBluetoothLeService.close();
    }



    public ArrayList<String> devicesDiscoveredFromMainActivityNames = new ArrayList<String>();
    public void refreshDevicesList() {

        Log.i(TAG, "refreshDevicesList");
        devicesDiscoveredFromMainActivityNames = new ArrayList<>();

        String y;
        Integer arrCounter = 0;
        
        if (mBluetoothLeService.getDevicesDiscovered().size() == 0) {
            Log.i(TAG, "refreshDevicesList: NO DEVICES DISCOVERED");
            return;
        }

        for (BluetoothDevice i : mBluetoothLeService.getDevicesDiscovered()) {
            y = (i.getName() + " : " +i.getAddress());
            Log.i(TAG, "refreshDevicesList: y:  " + y);
            devicesDiscoveredFromMainActivityNames.add(y);
            arrCounter += 1;
        }

        Log.i(TAG, "refreshDevicesList: devicesDiscoveredFromMainActivityNames  " + devicesDiscoveredFromMainActivityNames.size());
        
        listView = findViewById(R.id.list0);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, devicesDiscoveredFromMainActivityNames);
        listView.setAdapter(adapter);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                int itemPosition = position;

                // ListView Clicked item value
                String  itemValue = (String) listView.getItemAtPosition(position);


                if (mBluetoothLeService.getDevicesConnected().contains(mBluetoothLeService.getDevicesDiscovered().get(itemPosition))) {
                    Log.i(TAG, "onItemClick: mBluetoothLeService.getSizeOfDevicesConnected():  " + mBluetoothLeService.getSizeOfDevicesConnected());
                    Log.i(TAG, "connectToBtDevices: Already connected, so do nothing");
                    return;
                }

                // Show Alert
                Toast.makeText(getApplicationContext(),
                        "Position :"+itemPosition+"  ListItem : " +itemValue , Toast.LENGTH_LONG)
                        .show();
                connectToBtDevice(itemPosition, mBluetoothLeService.getDevicesDiscovered().get(itemPosition), mBluetoothLeService.getDevicesDiscovered().get(itemPosition).getAddress());
            }
        });
    }



//    public String getTimeStringFromMilli(long totalMilliseconds) {
//        return String.format(Locale.US,"%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(totalMilliseconds),
//                TimeUnit.MILLISECONDS.toMinutes(totalMilliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(totalMilliseconds)),
//                TimeUnit.MILLISECONDS.toSeconds(totalMilliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalMilliseconds)));
//    }


}

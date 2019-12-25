package com.aaronep.andy.antbletest;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private static final UUID CSC_SERVICE_UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    private static final UUID CSC_CHARACTERISTIC_UUID = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
    private static final UUID BTLE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    private ArrayList<BluetoothDevice> devicesConnected = new ArrayList<>();
    private ArrayList<BluetoothDevice> devicesReconnected = new ArrayList<>();


    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private boolean isBusy = false;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_DATA_AVAILABLE_HR =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE_HR";
    public final static String ACTION_DATA_AVAILABLE_SPD =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE_SPD";
    public final static String ACTION_DATA_AVAILABLE_DISTANCE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE_DISTANCE";
    public final static String ACTION_DATA_AVAILABLE_CAD =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE_CAD";
    public final static String ACTION_DATA_AVAILABLE_WHEEL =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE_WHEEL";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";


    private void broadcastUpdate(final String action) {
        Log.i(TAG, "broadcastUpdate: action:  " + action);
        final Intent intent = new Intent(action);
        //intent.putExtra(EXTRA_DATA, "EXTRA_DATA");
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String xtraData) {
        Log.i(TAG, "broadcastUpdate: action:  " + action);
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, xtraData);
        sendBroadcast(intent);
    }


    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        //close();
        Log.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }


        Log.i(TAG, "initialize: BluetoothLeService");
        return true;
    }


    //GATT CALLBACK  mBluetoothGattCallback0
    private BluetoothGattCallback mBluetoothGattCallback0 = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i(TAG, "onConnectionStateChange: gatt:  " + gatt.getDevice().getName());
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS: {
                    Log.i(TAG, "onConnectionStateChange: GATT_SUCCESS  " + gatt.getDevice().getName());
                    break;
                }
                case BluetoothGatt.GATT_FAILURE: {
                    Log.i(TAG, "onConnectionStateChange: GATT_FAILURE  " + gatt.getDevice().getName());
                    break;
                }
                default:
                    Log.i(TAG, "onConnectionStateChange: NOT SUCCESS OR FAILURE  " + gatt.getDevice().getName());
            }

            switch (newState) {
                case BluetoothAdapter.STATE_CONNECTED: {
                    Log.i(TAG, "onConnectionStateChange: STATE CONNECTED");

//                    Log.i(TAG, "onConnectionStateChange: CHECK FOR RECONNECTION");
//                    if (devicesReconnected.contains(gatt.getDevice())) {
//                        Log.i(TAG, "onConnectionStateChange: THIS IS FOR A RECONNECT, DO NOT DISCOVER SERVICES...");
//                        return;
//                    }

                    Log.i(TAG, "onConnectionStateChange: STATE CONNECTED, DISCOVER SERVICES: " + gatt.getDevice().getName());
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    if (!devicesDiscovered.contains(gatt.getDevice())) {
                        devicesDiscovered.add(gatt.getDevice());
                    }

                    if (!devicesConnected.contains(gatt.getDevice())) {
                        devicesConnected.add(gatt.getDevice());
                    }

                    //broadcastUpdate(ACTION_GATT_CONNECTED, gatt.getDevice().getName());
                    gatt.discoverServices();
                    break;
                }
                case BluetoothAdapter.STATE_CONNECTING: {
                    Log.i(TAG, "onConnectionStateChange: STATE CONNECTING:  " + gatt.getDevice().getName());
                    break;
                }
                case BluetoothAdapter.STATE_DISCONNECTED: {
                    Log.i(TAG, "onConnectionStateChange: STATE DISCONNECTED:  " + gatt.getDevice().getName());
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    broadcastUpdate(ACTION_GATT_DISCONNECTED, gatt.getDevice().getName());

                    if (devicesDiscovered.contains(gatt.getDevice())) {
                        devicesDiscovered.remove(gatt.getDevice());
                    }

                    if (devicesConnected.contains(gatt.getDevice())) {
                        devicesConnected.remove(gatt.getDevice());
                    }

                    closeSpecificGatt(gatt.getDevice());
                    break;
                }

            }

        }

        private void closeSpecificGatt(BluetoothDevice bluetoothDevice) {
            Log.i(TAG, "closeSpecificGatt after disconnection & try to reconnect");
            if (mGatt0 == null) {
                Log.i(TAG, "closeSpecificGatt no mGatt0");
            } else {
                if (mGatt0.getDevice() != null && bluetoothDevice == mGatt0.getDevice()) {
                    Log.i(TAG, "close: mGatt0:  " + bluetoothDevice.getName());
                    devicesConnected.remove(mGatt0.getDevice());
                    devicesDiscovered.remove(mGatt0.getDevice());
                    devicesReconnected.add(mGatt0.getDevice());
//                    broadcastUpdate(ACTION_GATT_DISCONNECTED, mGatt0.getDevice().getName());
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    Log.i(TAG, "closeSpecificGatt: attempt reconnect");
                    bluetoothDevice.connectGatt(BluetoothLeService.this, false, mBluetoothGattCallback0);
                    //MAYBE SHOULDN'T CLOSE?
                    //mGatt0.close();
                }
            }

            if (mGatt1 == null) {
                Log.i(TAG, "closeSpecificGatt: no mGatt1");
            } else {
                if (mGatt1.getDevice() != null && bluetoothDevice == mGatt1.getDevice()) {
                    Log.i(TAG, "close: mGatt1:  " + bluetoothDevice.getName());
                    devicesConnected.remove(mGatt1.getDevice());
                    devicesDiscovered.remove(mGatt1.getDevice());
                    devicesReconnected.add(mGatt1.getDevice());
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    //broadcastUpdate(ACTION_GATT_DISCONNECTED, mGatt1.getDevice().getName());
                    Log.i(TAG, "closeSpecificGatt: attempt reconnect");
                    bluetoothDevice.connectGatt(BluetoothLeService.this, false, mBluetoothGattCallback0);
                    //MAYBE SHOULDN'T CLOSE?
                    //mGatt1.close();
                    //mGatt1 = null;
                }
            }

            if (mGatt2 == null) {
                Log.i(TAG, "closeSpecificGatt: no mGatt2");
            } else {
                if (mGatt2.getDevice() != null && bluetoothDevice == mGatt2.getDevice()) {
                    Log.i(TAG, "close: mGatt2");
                    devicesConnected.remove(mGatt2.getDevice());
                    devicesDiscovered.remove(mGatt2.getDevice());
                    devicesReconnected.add(mGatt2.getDevice());
                    //broadcastUpdate(ACTION_GATT_DISCONNECTED, mGatt2.getDevice().getName());
                    bluetoothDevice.connectGatt(BluetoothLeService.this, false, mBluetoothGattCallback0);
                    //mGatt2.close();
                    //mGatt2 = null;
                }
            }

            if (mGatt3 == null) {
                Log.i(TAG, "closeSpecificGatt: no mGatt3");
            } else {
                if (mGatt3.getDevice() != null && bluetoothDevice == mGatt3.getDevice()) {
                    Log.i(TAG, "close: mGatt3");
                    devicesConnected.remove(mGatt3.getDevice());
                    devicesDiscovered.remove(mGatt3.getDevice());
                    devicesReconnected.add(mGatt3.getDevice());
                    //broadcastUpdate(ACTION_GATT_DISCONNECTED, mGatt3.getDevice().getName());
                    bluetoothDevice.connectGatt(BluetoothLeService.this, false, mBluetoothGattCallback0);
                    //mGatt3.close();
                }
            }

            if (mGatt4 == null) {
                Log.i(TAG, "closeSpecificGatt: no mGatt4");
            } else {
                if (mGatt4.getDevice() != null && bluetoothDevice == mGatt4.getDevice()) {
                    Log.i(TAG, "close: mGatt4");
                    devicesConnected.remove(mGatt4.getDevice());
                    devicesDiscovered.remove(mGatt4.getDevice());
                    devicesReconnected.add(mGatt4.getDevice());
                    //broadcastUpdate(ACTION_GATT_DISCONNECTED, mGatt4.getDevice().getName());
                    bluetoothDevice.connectGatt(BluetoothLeService.this, false, mBluetoothGattCallback0);
                    //mGatt4.close();
                }
            }

            if (mGatt5 == null) {
                Log.i(TAG, "closeSpecificGatt: no gatt5");
            } else {
                if (mGatt5.getDevice() != null && bluetoothDevice == mGatt5.getDevice()) {
                    Log.i(TAG, "close: mGatt5");
                    devicesConnected.remove(mGatt5.getDevice());
                    devicesDiscovered.remove(mGatt5.getDevice());
                    devicesReconnected.add(mGatt5.getDevice());
                    //broadcastUpdate(ACTION_GATT_DISCONNECTED, mGatt5.getDevice().getName());
                    bluetoothDevice.connectGatt(BluetoothLeService.this, false, mBluetoothGattCallback0);
                    //mGatt5.close();
                }
            }

        }

        private Boolean tryVelo = false;
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            Boolean hasHR = false;
            Boolean hasCSC = false;

            Log.i(TAG, "onServicesDiscovered");
            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                if (service.getUuid().equals(HR_SERVICE_UUID)) {
                    hasHR = true;
                    BluetoothGattCharacteristic valueCharacteristic = gatt.getService(HR_SERVICE_UUID).getCharacteristic(HR_CHARACTERISTIC_UUID);
                    boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);
                    Log.i(TAG, "registered for HR updates " + (notificationSet ? "successfully" : "unsuccessfully"));
                    BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
                    Log.i(TAG, "wrote Descriptor for HR updates " + (writeDescriptorSuccess ? "successfully" : "unsuccessfully"));
                }
                if (service.getUuid().equals(CSC_SERVICE_UUID)) {
                    hasCSC = true;
                    if (hasHR) {
                        Log.i(TAG, "onServicesDiscovered: IS A VELO");
                        if (!tryVelo) {
                            tryVelo = true;
                        }
                    }
                    BluetoothGattCharacteristic valueCharacteristic = gatt.getService(CSC_SERVICE_UUID).getCharacteristic(CSC_CHARACTERISTIC_UUID);
                    boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);
                    Log.i(TAG, "registered for CSC updates " + (notificationSet ? "successfully" : "unsuccessfully"));
                    BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
                    Log.i(TAG, "wrote Descriptor for CSC updates " + (writeDescriptorSuccess ? "successfully" : "unsuccessfully"));
                }
            }
            Log.i(TAG, "onServicesDiscovered: IS BUSY...NOW FALSE");
            isBusy = false;
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG, "onCharacteristicChanged");

            if (HR_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                Log.i(TAG, "onCharacteristicChanged: HR: " + gatt.getDevice().getAddress());
                getHeartrateValue(characteristic);

                if (tryVelo) {
                    tryVelo = false;
                    Log.i(TAG, "onCharacteristicChanged: TRYING VELO...SET NOTIFY");
                    BluetoothGattCharacteristic valueCharacteristic = gatt.getService(CSC_SERVICE_UUID).getCharacteristic(CSC_CHARACTERISTIC_UUID);
                    boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);
                    Log.i(TAG, "registered for VELO CSC updates " + (notificationSet ? "successfully" : "unsuccessfully"));
                    BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
                    Log.i(TAG, "wrote Descriptor for VELO CSC updates " + (writeDescriptorSuccess ? "successfully" : "unsuccessfully"));
                }

            }
            if (CSC_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                Log.i(TAG, "onCharacteristicChanged: CSC" + gatt.getDevice().getAddress());
                getSpeedCadenceValue(characteristic);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };
    //GATT CALLBACK END


    private BluetoothGatt mGatt0;
    private BluetoothGatt mGatt1;
    private BluetoothGatt mGatt2;
    private BluetoothGatt mGatt3;
    private BluetoothGatt mGatt4;
    private BluetoothGatt mGatt5;

    public void connectToBtDeviceAfterDelay(final Integer indexValue, String indexDeviceAddress) {
        Log.i(TAG, "connectToBtDeviceAfterDelay");
        final BluetoothDevice sIndexDevice = mBluetoothAdapter.getRemoteDevice(indexDeviceAddress);

        if (sIndexDevice == null) {
            Log.i(TAG, "Device not found.  Unable to connect.");
            isBusy = false;
        } else {
            if (indexValue == 0) {
                mGatt0 = sIndexDevice.connectGatt(this, false, mBluetoothGattCallback0);
                devicesConnected.add(mGatt0.getDevice());
            }
            if (indexValue == 1) {
                mGatt1 = sIndexDevice.connectGatt(this, false, mBluetoothGattCallback0);
                devicesConnected.add(mGatt1.getDevice());
            }
        }
    }

    public void connectToBtDevice(final Integer indexValue, final String indexDeviceAddress) {
        Log.i(TAG, "connectToBtDevice");

        if (isBusy) {
            Log.i(TAG, "connectToBtDevice: IS BUSY...SHOULD RETURN AND WAIT...");
            //return;
            Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "run: AFTER DELAY, IS BUSY");
                    connectToBtDeviceAfterDelay(indexValue, indexDeviceAddress);
                }
            }, 30000);
        }
        isBusy = true;
        
        final BluetoothDevice sIndexDevice = mBluetoothAdapter.getRemoteDevice(indexDeviceAddress);

        if (sIndexDevice == null) {
            Log.i(TAG, "Device not found.  Unable to connect.");
            isBusy = false;
        } else {
            if (indexValue == 0) {
                mGatt0 = sIndexDevice.connectGatt(this, false, mBluetoothGattCallback0);
                devicesConnected.add(mGatt0.getDevice());
            }
            if (indexValue == 1) {
                mGatt1 = sIndexDevice.connectGatt(this, false, mBluetoothGattCallback0);
                devicesConnected.add(mGatt1.getDevice());
            }
            if (indexValue == 2) {
                mGatt2 = sIndexDevice.connectGatt(this, false, mBluetoothGattCallback0);
                devicesConnected.add(mGatt2.getDevice());
            }
            if (indexValue == 3) {
                mGatt3 = sIndexDevice.connectGatt(this, false, mBluetoothGattCallback0);
                devicesConnected.add(mGatt3.getDevice());
            }
            if (indexValue == 4) {
                mGatt4 = sIndexDevice.connectGatt(this, false, mBluetoothGattCallback0);
                devicesConnected.add(mGatt4.getDevice());
            }
            if (indexValue == 5) {
                mGatt5 = sIndexDevice.connectGatt(this, false, mBluetoothGattCallback0);
                devicesConnected.add(mGatt5.getDevice());
            }

        }

    }

    public void close() {
        Log.i(TAG, "close");

        if (mGatt0 != null) {
            Log.i(TAG, "close: mGatt0");

            //TEST
            devicesConnected.remove(mGatt0.getDevice());
            devicesDiscovered.remove(mGatt0.getDevice());
            //END TEST

            mGatt0.close();
            mGatt0 = null;
        }
        if (mGatt1 != null) {
            Log.i(TAG, "close: mGatt1");
            mGatt1.close();
            mGatt1 = null;
        }
        if (mGatt2 != null) {
            Log.i(TAG, "close: mGatt2");
            mGatt2.close();
            mGatt2 = null;
        }
        if (mGatt3 != null) {
            Log.i(TAG, "close: mGatt3");
            mGatt3.close();
            mGatt3 = null;
        }
        if (mGatt4 != null) {
            Log.i(TAG, "close: mGatt4");
            mGatt4.close();
            mGatt4 = null;
        }
        if (mGatt5 != null) {
            Log.i(TAG, "close: mGatt5");
            mGatt5.close();
            mGatt5 = null;
        }
//        devicesDiscovered = new ArrayList<>();
//        devicesConnected = new ArrayList<>();
        //broadcastUpdate(ACTION_GATT_DISCONNECTED);



    }

    private void getHeartrateValue(BluetoothGattCharacteristic characteristic) {
        Log.i(TAG, "getHeartrateValue: ");
        final int flag = characteristic.getValue()[0]; // 1 byte
        int format;
        if ((flag & 0x01) != 0) {
            format = BluetoothGattCharacteristic.FORMAT_UINT16;
        } else {
            format = BluetoothGattCharacteristic.FORMAT_UINT8;
        }
        final int hrValue = characteristic.getIntValue(format, 1);
        Log.i(TAG, "getHeartrateValue: HR Value: " + hrValue);

        @SuppressLint("DefaultLocale") final String hrString = String.format("%d H", hrValue);
        broadcastUpdate(ACTION_DATA_AVAILABLE_HR, hrString);
    }

    final byte WHEEL_REVOLUTIONS_DATA_PRESENT = 0x01; // 1 bit
    final byte CRANK_REVOLUTION_DATA_PRESENT = 0x02; // 1 bit
    private void getSpeedCadenceValue(BluetoothGattCharacteristic characteristic) {
        Log.i(TAG, "getSpeedCadenceValue: ");
        byte[] value = characteristic.getValue();
        final int flags = characteristic.getValue()[0]; // 1 byte
        final boolean wheelRevPresent = (flags & WHEEL_REVOLUTIONS_DATA_PRESENT) > 0;
        final boolean crankRevPresent = (flags & CRANK_REVOLUTION_DATA_PRESENT) > 0;

        if (wheelRevPresent) {

            final int cumulativeWheelRevolutions = (value[1] & 0xff) | ((value[2] & 0xff) << 8);
            final int lastWheelEventReadValue = (value[5] & 0xff) | ((value[6] & 0xff) << 8);

            @SuppressLint("DefaultLocale") final String wString = String.valueOf(cumulativeWheelRevolutions);
            Log.i(TAG, "getSpeedCadenceValue wString: " + wString);
            broadcastUpdate(ACTION_DATA_AVAILABLE_WHEEL, wString);

            onWheelMeasurementReceived(cumulativeWheelRevolutions, lastWheelEventReadValue);


            if (crankRevPresent) {
                final int cumulativeCrankRevolutions = (value[7] & 0xff) | ((value[8] & 0xff) << 8);
                final int lastCrankEventReadValue = (value[9] & 0xff) | ((value[10] & 0xff) << 8);
                onCrankMeasurementReceived(cumulativeCrankRevolutions, lastCrankEventReadValue);
            }
        } else {
            if (crankRevPresent) {
                final int cumulativeCrankRevolutions = (value[1] & 0xff) | ((value[2] & 0xff) << 8);
                final int lastCrankEventReadValue = (value[3] & 0xff) | ((value[4] & 0xff) << 8);
                onCrankMeasurementReceived(cumulativeCrankRevolutions, lastCrankEventReadValue);
            }
        }
    }

    private int mFirstWheelRevolutions = -1;
    private int mLastWheelRevolutions = -1;
    private int mLastWheelEventTime = -1;
    private int mFirstCrankRevolutions = -1;
    private int mLastCrankRevolutions = -1;
    private int mLastCrankEventTime = -1;

    private double totalWheelRevolutions = 0;
    private double totalTimeInSeconds = 0;


    public void onWheelMeasurementReceived(final int wheelRevolutionValue, final int wheelRevolutionTimeValue) {


//        final int circumference = Integer.parseInt(preferences.getString(SettingsFragment.SETTINGS_WHEEL_SIZE, String.valueOf(SettingsFragment.SETTINGS_WHEEL_SIZE_DEFAULT))); // [mm]
        final int circumference = 2155; // [mm]

        if (mFirstWheelRevolutions < 0) {
            mFirstWheelRevolutions = wheelRevolutionValue;
            mLastWheelRevolutions = wheelRevolutionValue;
            mLastWheelEventTime = wheelRevolutionTimeValue;
            return;
        }

        if (mLastWheelEventTime == wheelRevolutionTimeValue) {
            return;
        }


        final int timeDiff = do16BitDiff(wheelRevolutionTimeValue, mLastWheelEventTime);
        final int wheelDiff = do16BitDiff(wheelRevolutionValue, mLastWheelRevolutions);

        if (wheelDiff == 0 || wheelDiff > 35) {
            mLastWheelRevolutions = wheelRevolutionValue;
            mLastWheelEventTime = wheelRevolutionTimeValue;
            return;
        }

        if (timeDiff < 1000) {
            //LET'S NOT PROCESS SO MANY, IGNORE EVERY OTHER ONE?
            return;
        }

        if (timeDiff > 30000) {
            mLastWheelRevolutions = wheelRevolutionValue;
            mLastWheelEventTime = wheelRevolutionTimeValue;
            return;
        }


        totalWheelRevolutions += (double) wheelDiff;
        double localDistance = (totalWheelRevolutions * ( (((double) circumference) / 1000) * 0.000621371 ));
        totalTimeInSeconds += (double) timeDiff / 1024.0;

        mLastWheelRevolutions = wheelRevolutionValue;
        mLastWheelEventTime = wheelRevolutionTimeValue;

        final double wheelTimeInSeconds = timeDiff / 1024.0;
        final double wheelCircumference = (double) circumference;
        final double wheelCircumferenceCM = wheelCircumference / 10;
        final double wheelRPM = (double) wheelDiff / (wheelTimeInSeconds / 60.0);
        final double cmPerMi = 0.00001 * 0.621371;
        final double minsPerHour = 60.0;
        final double speed = wheelRPM * wheelCircumferenceCM * cmPerMi * minsPerHour;  //MPH CURRENT
        final double totalDistance = totalWheelRevolutions * wheelCircumferenceCM * cmPerMi;

        final double btAvgSpeed = totalDistance / (totalTimeInSeconds / 60.0 / 60.0);
        Log.d(TAG, "onWheelMeasurementReceived: btAvgSpeed = " + String.format("%.1f Avg Speed", btAvgSpeed));

        @SuppressLint("DefaultLocale") final String spdString = String.format("%.2f S", speed);
        @SuppressLint("DefaultLocale") final String distanceString = String.format("%.2f MILES", localDistance);
        broadcastUpdate(ACTION_DATA_AVAILABLE_SPD, spdString);
        broadcastUpdate(ACTION_DATA_AVAILABLE_DISTANCE, distanceString);

    }  //END WHEEL CALC

    public void onCrankMeasurementReceived(final int crankRevolutionValue, final int crankRevolutionTimeValue) {

        if (mFirstCrankRevolutions < 0) {
            mFirstCrankRevolutions = crankRevolutionValue;
            mLastCrankRevolutions = crankRevolutionValue;
            mLastCrankEventTime = crankRevolutionTimeValue;
            return;
        }

        if (mLastCrankEventTime == crankRevolutionTimeValue) {
            return;
        }


        final int timeDiff = do16BitDiff(crankRevolutionTimeValue, mLastCrankEventTime);
        final int crankDiff = do16BitDiff(crankRevolutionValue, mLastCrankRevolutions);

        if (crankDiff == 0) {
            mLastCrankRevolutions = crankRevolutionValue;
            mLastCrankEventTime = crankRevolutionTimeValue;
            return;
        }

        if (timeDiff < 2000) {
            return;
        }

        if (timeDiff > 30000) {
            mLastCrankRevolutions = crankRevolutionValue;
            mLastCrankEventTime = crankRevolutionTimeValue;
            return;
        }


        ////Log.i("CAD", "onWheelMeasurementReceived: crankDiff, timeDiff: " + crankDiff + ", " + timeDiff);
        final double cadence = (double) crankDiff / ((((double) timeDiff) / 1024.0) / 60);
        if (cadence == 0) {
            return;
        }
        if (cadence > 150) {
            return;
        }


        @SuppressLint("DefaultLocale") final String cadString = String.format("%.1f C", cadence);
        broadcastUpdate(ACTION_DATA_AVAILABLE_CAD, cadString);

    }
    //END CAD CALC

    private int do16BitDiff(int a, int b) {
        if (a >= b)
            return a - b;
        else
            return (a + 65536) - b;
    }


    //DEVICE MANAGEMENT

    public ArrayList<BluetoothDevice> getDevicesDiscovered() {
        return devicesDiscovered;
    }

    public ArrayList<BluetoothDevice> getDevicesConnected() {
        return devicesConnected;
    }

    public ArrayList<BluetoothDevice> getDevicesReonnected() {
        return devicesReconnected;
    }

    public int getSizeOfDevicesDiscovered() {
        Log.i(TAG, "getSizeOfDevicesDiscovered: " + devicesDiscovered.size());
        return devicesDiscovered.size();
    }

    public void addDeviceDiscovered(BluetoothDevice device) {
        Log.i(TAG, "addDevice: " + device.getName());
        devicesDiscovered.add(device);
    }

    public int getSizeOfDevicesConnected() {
        Log.i(TAG, "getSizeOfDevicesConnected: " + devicesConnected.size());
        return devicesConnected.size();
    }

    public void addDeviceConnected(BluetoothDevice device) {
        Log.i(TAG, "addDevice: " + device.getName());
        devicesConnected.add(device);
    }

    public void addDeviceReconnected(BluetoothDevice device) {
        Log.i(TAG, "addDevice, Reconnected: " + device.getName());
        devicesReconnected.add(device);
    }

    public String getDeviceAddress(BluetoothDevice device) {
        Log.i(TAG, "getDeviceAddress: " + device.getAddress());
        return device.getAddress();
    }

    public String getDeviceName(BluetoothDevice device) {
        Log.i(TAG, "getDeviceName: " + device.getName());
        return device.getName();
    }

    //DEVICE MANAGEMENT - END






}

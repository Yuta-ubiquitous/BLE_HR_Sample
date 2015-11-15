package com.example.yuta.ble_hr;

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
import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();
    final int REQUEST_ENABLE_BT = 1;
    private static final String DEVICE_HEART_RATE_SERVICE_UUID = "0000180d-0000-1000-8000-00805f9b34fb";
    private static final String DEVICE_HEART_RATE_CHARACTERISTIC_UUID = "00002a37-0000-1000-8000-00805f9b34fb";
    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private BluetoothAdapter bluetoothAdapter;
    private boolean isScanning;
    private Handler handler;
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, device.getName());
            if(device.getName().equals("MIO GLOBAL LINK")){
                bluetoothGatt = device.connectGatt(getApplicationContext(), false, bluetoothGattCallback);
                bluetoothAdapter.stopLeScan(this);
            }
        }
    };
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED){
                bluetoothGatt.discoverServices();
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                bluetoothGatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            if(status == BluetoothGatt.GATT_SUCCESS){
                BluetoothGattService service = gatt.getService(UUID.fromString(DEVICE_HEART_RATE_SERVICE_UUID));
                if(service != null){
                    BluetoothGattCharacteristic characteristic =
                            service.getCharacteristic(UUID.fromString(DEVICE_HEART_RATE_CHARACTERISTIC_UUID));
                    if (characteristic != null){
                        boolean registerd = gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG)
                        );
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                        if(registerd){
                            Log.d(TAG,"success notification setting");
                        }else{
                            Log.d(TAG,"failed notification setting");
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
            Log.d(TAG, "onCharacteristicChanged");
            if(UUID.fromString(DEVICE_HEART_RATE_CHARACTERISTIC_UUID).equals(characteristic.getUuid())){
                int flag = characteristic.getProperties();
                int format = -1;
                if((flag & 0x01) != 0){
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                }else{
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                }
                final int heartRate = characteristic.getIntValue(format, 1);
                Log.d(TAG, String.valueOf(heartRate));
            }
        }
    };

    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        BluetoothManager bluetoothManager = (BluetoothManager)this.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        handler = new Handler();
        this.scanLeDevice();
    }

    private void scanLeDevice(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isScanning = false;
                bluetoothAdapter.stopLeScan(leScanCallback);
            }
        }, SCAN_PERIOD);
        isScanning = true;
        bluetoothAdapter.startLeScan(leScanCallback);
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
}

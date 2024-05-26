package com.example.myapplicationtrial;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView pHTextView, tdsTextView, pumpTextView;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;
    InputStream inputStream;
    static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pHTextView = findViewById(R.id.pHTextView);
        tdsTextView = findViewById(R.id.tdsTextView);
        pumpTextView = findViewById(R.id.pumpTextView);

        // Bluetooth setup
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        String bluetoothAddress = "00:23:08:34:CF:14"; // Replace with your Bluetooth device address
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(bluetoothAddress);

        // Check and request permissions
        checkAndRequestBluetoothPermission();
    }

    private void checkAndRequestBluetoothPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            }, REQUEST_BLUETOOTH_PERMISSION);
        } else {
            // Permissions already granted
            connectBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                connectBluetooth();
            } else {
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void connectBluetooth() {
        // Check if Bluetooth is supported and enabled
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        // Check for Bluetooth admin permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_PERMISSION);
            return;
        }

        // Connect to Bluetooth device
        // Consider performing this in a background thread
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            bluetoothSocket.connect();
            inputStream = bluetoothSocket.getInputStream();
            readBluetoothData();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to connect to Bluetooth device", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void readBluetoothData() {
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[1024];
                    int bytes = inputStream.read(buffer);
                    String data = new String(buffer, 0, bytes);
                    Log.d("BluetoothData", "Received data: " + data); // Log received data
                    String[] values = data.split(",");

                    // Ensure there are enough values in the array
                    if (values.length >= 3) {
                        // Update UI with received values
                        pHTextView.setText("Ph value: " + values[0]);
                        tdsTextView.setText("Tds value: " + values[1]);
                        pumpTextView.setText("Pump status: " + values[2]);
                    } else {
                        Log.e("BluetoothData", "Received data does not contain enough values");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                handler.postDelayed(this, 1000); // Update every second
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (inputStream != null)
                inputStream.close();
            if (bluetoothSocket != null)
                bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
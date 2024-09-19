package com.carrinho.controle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String APP_TAG = "OPEN_CVV";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISCOVER_BT = 2;
    private static final int REQUEST_SCAN_BT = 3;
    private final UUID APP_UUID = new UUID(205, 654);
    private final BroadcastReceiver incomingPairRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)){
                    
            }
        }
    };
    
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
            }
        }
    };
    private ClientSocketThread blueSocketThread;
    private Button pairBtn;
    private TextView statusBluethootTv;
    private ImageView bluethootIv;
    private boolean isConnected;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> blueDevicesSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        isConnected = false;
        bluethootIv = findViewById(R.id.bluetoothIv);
        statusBluethootTv = findViewById(R.id.statusBluethootTv);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairBtn = findViewById(R.id.pairBtn);
        if (bluetoothAdapter == null) {
            statusBluethootTv.setText("Não tem Bluetooth");
        } else {
            statusBluethootTv.setText("Tem Bluetooth");
        }

        pairBtn.setOnClickListener((click) -> {
            IntentFilter filter = new IntentFilter((BluetoothDevice.ACTION_FOUND));
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            if (!bluetoothAdapter.isEnabled()) {
                Intent enbaleBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enbaleBtIntent, REQUEST_ENABLE_BT);
            }
            startActivityForResult(discoverableIntent, REQUEST_DISCOVER_BT);
            
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        unregisterReceiver(incomingPairRequestReceiver);
    }

    private class ClientSocketThread extends Thread {
        private final BluetoothSocket blueSocket;
        private final BluetoothDevice blueDevice;

        public ClientSocketThread(BluetoothDevice blueDevice){
            BluetoothSocket tmpSocket = null;
            this.blueDevice = blueDevice;
            try{
                tmpSocket = blueDevice.createRfcommSocketToServiceRecord(APP_UUID);
            }catch (Exception e){
                Log.e(APP_TAG, e.getMessage());
            }
            blueSocket = tmpSocket;
        }

        @Override
        public void run() {
            try{
                bluetoothAdapter.cancelDiscovery();
                blueSocket.connect();
            }catch (IOException e){
            }catch (SecurityException e){
                
            }
        }
    }
}
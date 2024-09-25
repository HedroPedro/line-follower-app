package com.carrinho.controle;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String APP_TAG = "line_follower";
    private final UUID APP_UUID = new UUID(205, 654);
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                blueDevicesSet.add(newDevice);
            }
        }
    };

    private Handler handler;
    private ListView listView;
    private Toast toast;
    private BluetoothDevice currentDevice;
    private Button pairBtn;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> blueDevicesSet;
    private LineFollowerService service;
    private PreviewView view;
    private final ActivityResultLauncher permissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
            (permissions) -> {
                permissions.forEach((string, value) -> {
                    switch(string){
                        case Manifest.permission.CAMERA:
                            if(value)
                                service.startCamera(this);
                        default:
                            break;
                    }
                });
            });
    private final ActivityResultLauncher<Intent> startActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            (result) -> {});
    private final String[] permissionsArray = new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH_ADVERTISE};
    private BluetoothState state;
    private ClientSocketThread clientSocketThread;
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
        List<String> s = new ArrayList<String>();
        listView = findViewById(R.id.listView);
        view = findViewById(R.id.surfaceView);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairBtn = findViewById(R.id.pairBtn);
        service = new LineFollowerService(this, view, false);
        state = BluetoothState.NOT_ENABLED;
        handler = new Handler();
        permissionResultLauncher.launch(permissionsArray);

        pairBtn.setOnClickListener((click) -> {
            if(state.getId() == BluetoothState.CONNECTED.getId()){
                clientSocketThread.cancel();
                listView.setClickable(true);
                state = BluetoothState.ENABLED;
                pairBtn.setText(state.getMsg());
                service.setCanProcess(false);
                return;
            }

            if(!bluetoothAdapter.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityLauncher.launch(enableBtIntent);
                state = BluetoothState.ENABLED;
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
                return;
            }

            if(!bluetoothAdapter.isDiscovering()){
                Intent discoverBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityLauncher.launch(discoverBtIntent);
                displayToastMsg("Descobrindo aprelhos...", Toast.LENGTH_SHORT);
            }


            if(state.getId() == BluetoothState.ENABLED.getId()){
                List<String> appNames = new ArrayList<>();
                blueDevicesSet = bluetoothAdapter.getBondedDevices();
                if(blueDevicesSet.size() > 0){
                    for(BluetoothDevice device : blueDevicesSet){
                        appNames.add(device.getName());
                    }
                    listView.setAdapter(new ArrayAdapter<String>(this, R.layout.list, R.id.listElementText, appNames));
                }
            }

            pairBtn.setText(state.getMsg());
        });

        blueDevicesSet = bluetoothAdapter.getBondedDevices();
        if(blueDevicesSet.size() > 0){
            for(BluetoothDevice bt : blueDevicesSet){
                s.add(bt.getName());
            }
        }

        if(bluetoothAdapter.isEnabled()){
            listView.setAdapter(new ArrayAdapter<String>(this, R.layout.list, R.id.listElementText, s));
            state = BluetoothState.ENABLED;
        }

        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        pairBtn.setText(state.getMsg());
        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            if(state.getId() != BluetoothState.CONNECTED.getId()){
                Object obj = adapterView.getItemAtPosition(i);
                String str = obj.toString();
                BluetoothDevice device = null;
                for (BluetoothDevice d: blueDevicesSet) {
                    if(d.getName().equals(str)){
                        device = d;
                        break;
                    }
                }
                state = BluetoothState.CONNECTED;
                pairBtn.setText(state.getMsg());
                service.setCanProcess(true);
                clientSocketThread = new ClientSocketThread(device);
                clientSocketThread.run();
            }

            if(!OpenCVLoader.initDebug())
                Log.i(APP_TAG, "Incapaz de carregar o OpenCv");
            else
                Log.i(APP_TAG, "OpenCv carregado com sucesso");
        });
    }

    private void displayToastMsg(CharSequence msg, int length){
        toast = Toast.makeText(this, msg, length);
        toast.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private class ClientSocketThread extends Thread {
        private final BluetoothSocket blueSocket;

        public ClientSocketThread(BluetoothDevice blueDevice){
            BluetoothSocket tmpSocket = null;
            bluetoothAdapter.cancelDiscovery();
            try{
                tmpSocket = blueDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            }catch (SecurityException e){
                Log.e(APP_TAG, e.getMessage());
            }catch (Exception e){
                Log.e(APP_TAG, e.getMessage());
                state = BluetoothState.ENABLED;
                pairBtn.setText(state.getMsg());
            }
            blueSocket = tmpSocket;
        }

        @Override
        public void run() {
            OutputStream out = null;
            try{
                blueSocket.connect();
                out = blueSocket.getOutputStream();
            }catch (IOException e){
                Log.e(APP_TAG, e.getMessage());
                try {
                    blueSocket.close();
                    return;
                } catch (IOException ex) {
                    Log.e(APP_TAG, ex.getMessage());
                }
            }catch (SecurityException e) {
                Log.e(APP_TAG, e.getMessage());
            }
            service.setCanProcess(true);
            service.setSocket(blueSocket);
            service.setOutStream(out);
        }

        public void cancel(){
            try {
                blueSocket.close();
                state = BluetoothState.ENABLED;
                pairBtn.setText(state.getMsg());
                service.setCanProcess(false);
            } catch (IOException e) {
                Log.e(APP_TAG, e.getMessage());
            }
        }
    }
}
package com.carrinho.controle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.security.Permissions;
import java.security.acl.Permission;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Button pairBtn;
    private TextView statusBluethootTv;
    private ImageView bluethootIv;
    private boolean isConnected;
    private BluetoothAdapter bluetoothAdapter;
    private ActivityResultLauncher<Intent> bluetoothActivityResult;

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
        if(bluetoothAdapter == null) {
            statusBluethootTv.setText("Bluetooth disponível");
        }else{
            statusBluethootTv.setText("Bluetooth indisponível");
        }

        pairBtn.setOnClickListener((l) -> {
            if(!bluetoothAdapter.isEnabled()){
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}
package com.carrinho.controle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private ConnectedThread thread;
    private Mat imgMat;

    public void setImgMat(Mat imgMat){
        this.imgMat = imgMat;
    }

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

    private static final String APP_TAG = "OPEN_CVV";
    private Handler handler;

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket blueSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private byte[] buffer;

        public ConnectedThread(BluetoothSocket socket){
            blueSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try{
                tmpIn = socket.getInputStream();
            }catch (Exception e){
                Log.e(APP_TAG, e.getMessage());
            }

            try{
                tmpOut = socket.getOutputStream();
            }catch (Exception e){
                Log.e(APP_TAG, e.getMessage());
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run(){
            buffer = new byte[1024];
            int numBytes;
            while(true){
                try {
                    numBytes = inputStream.read(buffer);
                    Mat imgMat = new Mat(32, 32, CvType.CV_8UC1);
                    imgMat.put(0, 0, buffer);
                    setImgMat(imgMat);
                } catch (IOException e) {
                    Log.d(APP_TAG,"Input stream foi desconectada", e);
                }
            }
        }

        public void write(byte[] buffer){
            try {
                outputStream.write(buffer);
            } catch (IOException e) {
                Log.d(APP_TAG, "Output stream foi desconectada", e);
            }
        }

        public void cancel(){
            try{
                blueSocket.close();
            }catch(IOException e){
                Log.e(APP_TAG, "Nao foi fechado o socket");
            }
        }
    }
}
package com.carrinho.controle;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

public class BluethootService {
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

            }
        }
    }
}

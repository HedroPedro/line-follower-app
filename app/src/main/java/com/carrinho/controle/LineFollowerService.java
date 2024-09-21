package com.carrinho.controle;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LineFollowerService extends Thread{
    private OutputStream outStream;
    private final ImageCapture imageCapture;
    private final PreviewView view;
    private final Context context;

    public LineFollowerService(Context context, PreviewView view){
        imageCapture = new ImageCapture.Builder().build();
        this.view = view;
        this.context = context;
    }

    public void setOutStream(OutputStream outStream) {
        this.outStream = outStream;
    }

    public void startCamera(MainActivity activity){
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(activity);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(view.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, preview);
            } catch (ExecutionException e) {
                Log.e(MainActivity.APP_TAG, e.getMessage());
            } catch (InterruptedException e) {
                Log.e(MainActivity.APP_TAG, e.getMessage());
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    @Override
    public void run() {
        write("Estou Fora".getBytes());
        ImageCapture capture = new ImageCapture.Builder().build();
        imageCapture.takePicture(ContextCompat.getMainExecutor(context), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(MainActivity.APP_TAG, "Erros");
            }
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                System.out.println("Ola");
                try {
                    outStream.write("a".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Log.i(MainActivity.APP_TAG, "Estou aqui dentro");
                /*Mat imgMat = convertImageProxyToMat(image);
                Mat hierarchy = new Mat();
                Mat mask = new Mat();
                Moments moments;
                List<MatOfPoint> contours = new ArrayList<>();
                int maxValIndx = 0;
                double maxVal = 0.0f;
                Core.inRange(imgMat, new Scalar(0.d), new Scalar(0.5d), mask);
                Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                if(contours.size() > 0) {
                    maxVal = Imgproc.contourArea(contours.get(0));
                    for (int contourIdx = 1; contourIdx < contours.size(); contourIdx++) {
                        double valToCmp = Imgproc.contourArea(contours.get(contourIdx));
                        if (valToCmp > maxVal) {
                            maxVal = valToCmp;
                            maxValIndx = contourIdx;
                        }
                    }
                    moments = Imgproc.moments(contours.get(maxValIndx));
                    if (moments.get_m00() != 0.d) {
                        int cx;
                        cx = (int) (moments.get_m10() / moments.get_m00());
                        if (cx > 119) {
                            write("1001".getBytes());
                            image.close();
                            return;
                        }
                        if (cx < 120 && cx > 40) {
                            write("1010".getBytes());
                            image.close();
                            return;
                        }
                        write("0110".getBytes());
                        image.close();
                        return;
                    }
                }
                write("0000".getBytes());
                image.close();*/
            }
        });
    }

    public void write(byte buffer[]){
        try {
            outStream.write(buffer);
        } catch (IOException e) {
            Log.e(MainActivity.APP_TAG, "Não foi possível enviar para o sockete");
        }
    }

    //https://stackoverflow.com/questions/58102717/android-camerax-analyzer-image-with-format-yuv-420-888-to-opencv-mat
    private static Mat convertImageProxyToMat(@NonNull ImageProxy img){
        ByteBuffer bb = img.getPlanes()[0].getBuffer();
        byte[] buffer = new byte[bb.remaining()];
        return Imgcodecs.imdecode(new MatOfByte(buffer), Imgcodecs.IMREAD_UNCHANGED);
    }

    public byte[] giveCarDecision(ImageProxy img){
        return new byte[]{0};
    }
}

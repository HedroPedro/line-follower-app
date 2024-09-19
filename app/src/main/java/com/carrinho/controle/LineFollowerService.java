package com.carrinho.controle;

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class LineFollowerService extends Thread{
    private final OutputStream outStream;
    private final ImageCapture imageCapture;

    public LineFollowerService(View view, BluetoothSocket socket){
        OutputStream tmpOut = null;
        imageCapture = new ImageCapture.Builder().setTargetRotation(view.getDisplay().getRotation())
                .build();

        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(MainActivity.APP_TAG, "Erro aconteceu ao criar");
        }
        outStream = tmpOut;
    }

    @Override
    public void run() {
        Executor excecutor = new Executor() {
            @Override
            public void execute(Runnable runnable) {
                runnable.run();
            }
        };

        while(true){
            imageCapture.takePicture(excecutor, new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    super.onError(exception);
                }

                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    Mat imgMat = convertImageProxyToMat(image);
                    Mat hierarchy = new Mat();
                    Moments moments;
                    List<MatOfPoint> contours = new ArrayList<>();
                    int maxValIndx = 0;
                    double maxVal = Imgproc.contourArea(contours.get(0));

                    Imgproc.findContours(imgMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                    if(contours.size() > 0){
                        for(int contourIdx = 1; contourIdx < contours.size(); contourIdx++){
                            double valToCmp = Imgproc.contourArea(contours.get(contourIdx));
                            if(valToCmp > maxVal){
                                maxVal = valToCmp;
                                maxValIndx = contourIdx;
                            }
                        }
                    }
                    moments = Imgproc.moments(contours.get(maxValIndx));
                    if(moments.get_m00() != 0.d){
                        int cx;
                        cx = (int) (moments.get_m10()/moments.get_m00());
                        if(cx > 119){
                            write("1001".getBytes());
                            return;
                        }
                        if(cx < 120 && cx > 40){
                            write("1010".getBytes());
                            return;
                        }

                        write("0110".getBytes());
                        return;
                    }
                    write("0000".getBytes());
                }
            });
        }
    }

    public void write(byte buffer[]){
        try {
            outStream.write(buffer);
        } catch (IOException e) {
            Log.e(MainActivity.APP_TAG, "Não foi possível enviar para o sockete");
        }
    }

    //https://stackoverflow.com/questions/58102717/android-camerax-analyzer-image-with-format-yuv-420-888-to-opencv-mat
    private Mat convertImageProxyToMat(@NonNull ImageProxy img){
        byte tmpImageBuffer[];
        ByteBuffer yBuffer = img.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = img.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = img.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        Mat grayscaleMat = new Mat(img.getHeight() + (img.getHeight()>>1), img.getWidth(), CvType.CV_8UC1);
        tmpImageBuffer = new byte[ySize + uSize + vSize];

        yBuffer.get(tmpImageBuffer, 0, ySize);
        vBuffer.get(tmpImageBuffer, ySize, vSize);
        uBuffer.get(tmpImageBuffer, ySize + vSize, uSize);
        grayscaleMat.put(0, 0, tmpImageBuffer);
        return grayscaleMat;
    }
}

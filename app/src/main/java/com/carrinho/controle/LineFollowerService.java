package com.carrinho.controle;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class LineFollowerService extends Thread{
    private final ImageAnalysis imageAnalysis;
    private final PreviewView view;
    private final Context context;
    private OutputStream outStream;
    private BluetoothSocket socket;
    private Boolean canProcess;

    public LineFollowerService(Context context, PreviewView view, Boolean canProcess) {
        imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(1820, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        this.canProcess = canProcess;
        this.view = view;
        this.context = context;
    }

    //https://stackoverflow.com/questions/58102717/android-camerax-analyzer-image-with-format-yuv-420-888-to-opencv-mat
    private static Mat convertImageProxyToMat(@NonNull ImageProxy img) {
        byte[] nv21;

        ByteBuffer yBuffer = img.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = img.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = img.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        Mat yuv = new Mat(img.getHeight() + img.getHeight() / 2, img.getWidth(), CvType.CV_8UC1);
        yuv.put(0, 0, nv21);
        Mat rgb = new Mat();
        Imgproc.cvtColor(yuv, rgb, Imgproc.COLOR_YUV2RGB_NV21, 3);
        Core.rotate(rgb, rgb, Core.ROTATE_90_CLOCKWISE);
        return rgb;
    }

    public void setSocket(BluetoothSocket socket) {
        this.socket = socket;
    }

    public void setOutStream(OutputStream outStream) {
        this.outStream = outStream;
    }

    public void setCanProcess(Boolean canProcess) {
        this.canProcess = canProcess;
    }

    public void startCamera(MainActivity activity) {
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                if (!canProcess) {
                    image.close();
                    return;
                }
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(MainActivity.APP_TAG, e.getMessage());
                }
                Mat imgMat = convertImageProxyToMat(image);
                Mat hierarchy = new Mat();
                Mat mask = new Mat();
                Moments moments;
                List<MatOfPoint> contours = new ArrayList<>();
                int maxValIndx = 0;
                double maxVal = 0.0f;
                image.close();
                Core.inRange(imgMat, new Scalar(0.d, 0.d, 0.d), new Scalar(0.019d, 0.019d, 0.019d), mask);
                Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
                if (contours.size() > 0) {
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
                            write("1001".getBytes());//Esquerda
                            return;
                        }
                        if (cx < 120 && cx > 40) {
                            write("1010".getBytes());//Continua reto
                            return;
                        }
                        write("0110".getBytes());//Direita
                        return;
                    }
                }
                write("0000".getBytes());//Pare, não tem linha
            }
        });

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(activity);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(view.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
            } catch (ExecutionException e) {
                Log.e(MainActivity.APP_TAG, e.getMessage());
            } catch (InterruptedException e) {
                Log.e(MainActivity.APP_TAG, e.getMessage());
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    public void write(byte buffer[]) {
        try {
            outStream.write(buffer);
        } catch (IOException e) {
            Log.e(MainActivity.APP_TAG, "Não foi possível enviar para o sockete: " + e.getMessage());
        }
    }
}

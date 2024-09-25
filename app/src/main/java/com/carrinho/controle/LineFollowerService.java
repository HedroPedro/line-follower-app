package com.carrinho.controle;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
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
    private final double maxThreshScalarValue = 10.d;
    private final double thresholdValue = maxThreshScalarValue/255;

    public LineFollowerService(Context context, PreviewView view, Boolean canProcess) {
        imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(1820, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        this.canProcess = canProcess;
        this.view = view;
        this.context = context;
    }

    //https://stackoverflow.com/questions/58102717/android-camerax-analyzer-image-with-format-yuv-420-888-to-opencv-mat
    @NonNull
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
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), image -> {
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
            image.close();
            List<MatOfPoint> contours = new ArrayList<>();
            Mat edges = new Mat();
            Mat hierarchy = new Mat();
            Mat range = new Mat();
            Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_RGB2GRAY);
//            Imgproc.Canny(imgMat, edges, 50, 150);
            Core.inRange(imgMat, new Scalar(0.d, 0.d, 0.d), new Scalar(5d, 5d, 5d), range);
            Imgproc.findContours(range, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
            if (!contours.isEmpty()) {
                int maxValIndx = 0;
                double maxVal = Imgproc.contourArea(contours.get(0));
                for (int contourIdx = 1; contourIdx < contours.size(); contourIdx++) {
                    double valToCmp = Imgproc.contourArea(contours.get(contourIdx));
                    if (valToCmp > maxVal) {
                        maxVal = valToCmp;
                        maxValIndx = contourIdx;
                    }
                }
                Log.i(MainActivity.APP_TAG, "Index maior " + maxValIndx);
                Moments moments = Imgproc.moments(contours.get(maxValIndx));
                if (moments.get_m00() != 0.d) {
                    int cx = (int) (moments.get_m10() / (moments.get_m00()*10));
                    Log.i(MainActivity.APP_TAG, "cx: " + cx);
                    if (cx > 139) {
                        write("L".getBytes());//Esquerda
                        return;
                    }
                    if (cx < 140 && cx > 40) {
                        write("U".getBytes());//Continua reto
                        return;
                    }
                    write("R".getBytes());//Direita
                    return;
                }
            }
            write("S".getBytes());//Pare, não tem linha
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

package com.ece420.lab6;

import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
// import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;
import java.lang.Math;
// import java.util.List;
import java.util.Collections;
import java.util.Arrays;




public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    // UI Variable
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private SurfaceView surfaceView2;
    private SurfaceHolder surfaceHolder2;
    private TextView textHelper;
    // Camera Variable
    private Camera camera;
    boolean previewing = false;
    private int width = 640;
    private int height = 480;
    // Kernels
    private double[][] kernelS = new double[][] {{-1,-1,-1},{-1,9,-1},{-1,-1,-1}};
    private double[][] kernelX = new double[][] {{1,0,-1},{1,0,-1},{1,0,-1}};
    private double[][] kernelY = new double[][] {{1,1,1},{0,0,0},{-1,-1,-1}};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.UNKNOWN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        super.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Modify UI Text
        textHelper = (TextView) findViewById(R.id.Helper);
        if(MainActivity.appFlag == 1) textHelper.setText("Histogram Equalized Image");
        else if(MainActivity.appFlag == 2) textHelper.setText("Sharpened Image");
        else if(MainActivity.appFlag == 3) textHelper.setText("Edge Detected Image");

        // Setup Surface View handler
        surfaceView = (SurfaceView)findViewById(R.id.ViewOrigin);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView2 = (SurfaceView)findViewById(R.id.ViewHisteq);
        surfaceHolder2 = surfaceView2.getHolder();
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Must have to override native method
        return;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(!previewing) {
            camera = Camera.open();
            if (camera != null) {
                try {
                    // Modify Camera Settings
                    Camera.Parameters parameters = camera.getParameters();
                    parameters.setPreviewSize(width, height);
                    // Following lines could log possible camera resolutions, including
                    // 2592x1944;1920x1080;1440x1080;1280x720;640x480;352x288;320x240;176x144;
                    // List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                    // for(int i=0; i<sizes.size(); i++) {
                    //     int height = sizes.get(i).height;
                    //     int width = sizes.get(i).width;
                    //     Log.d("size: ", Integer.toString(width) + ";" + Integer.toString(height));
                    // }
                    camera.setParameters(parameters);
                    camera.setDisplayOrientation(90);
                    camera.setPreviewDisplay(surfaceHolder);
                    camera.setPreviewCallback(new PreviewCallback() {
                        public void onPreviewFrame(byte[] data, Camera camera)
                        {
                            // Lock canvas
                            Canvas canvas = surfaceHolder2.lockCanvas(null);
                            // Where Callback Happens, camera preview frame ready
                            onCameraFrame(canvas,data);
                            // Unlock canvas
                            surfaceHolder2.unlockCanvasAndPost(canvas);
                        }
                    });
                    camera.startPreview();
                    previewing = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Cleaning Up
        if (camera != null && previewing) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
            previewing = false;
        }
    }

    // Camera Preview Frame Callback Function
    protected void onCameraFrame(Canvas canvas, byte[] data) {

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        int retData[] = new int[width * height];

        // Apply different processing methods
        if(MainActivity.appFlag == 1){
            byte[] histeqData = histEq(data, width, height);
            retData = yuv2rgb(histeqData);
        }
        else if (MainActivity.appFlag == 2){

            int[] sharpData = conv2(data, width, height, kernelS);
            retData = merge(sharpData, sharpData);
        }
        else if (MainActivity.appFlag == 3){
            int[] xData = conv2(data, width, height, kernelX);
            int[] yData = conv2(data, width, height, kernelY);
            retData = merge(xData, yData);
        }

        // Create ARGB Image, rotate and draw
        Bitmap bmp = Bitmap.createBitmap(retData, width, height, Bitmap.Config.ARGB_8888);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        canvas.drawBitmap(bmp, new Rect(0,0, height, width), new Rect(0,0, canvas.getWidth(), canvas.getHeight()),null);
    }

    // Helper function to convert YUV to RGB
    public int[] yuv2rgb(byte[] data){
        final int frameSize = width * height;
        int[] rgb = new int[frameSize];

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) data[yp])) - 16;
                y = y<0? 0:y;

                if ((i & 1) == 0) {
                    v = (0xff & data[uvp++]) - 128;
                    u = (0xff & data[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                r = r<0? 0:r;
                r = r>262143? 262143:r;
                g = g<0? 0:g;
                g = g>262143? 262143:g;
                b = b<0? 0:b;
                b = b>262143? 262143:b;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
        return rgb;
    }

    // Helper function to merge the results and convert GrayScale to RGB
    public int[] merge(int[] xdata,int[] ydata){
        int size = height * width;
        int[] mergeData = new int[size];
        for(int i=0; i<size; i++)
        {
            int p = (int)Math.sqrt((xdata[i] * xdata[i] + ydata[i] * ydata[i]) / 2);
            mergeData[i] = 0xff000000 | p<<16 | p<<8 | p;
        }
        return mergeData;
    }

    // Function for Histogram Equalization
    public byte[] histEq(byte[] data, int width, int height){
        byte[] histeqData = new byte[data.length];
        int size = height * width;

        // Perform Histogram Equalization
        // Note that you only need to manipulate data[0:size] that corresponds to luminance
        // The rest data[size:data.length] is for colorness that we handle for you
        // *********************** START YOUR CODE HERE  **************************** //
        // find histogram size
        int max_val = data[0] & 0xff;
        for (int i = 0; i < size; i++) {

            int v = 0xff & data[i];
            max_val = (v > max_val) ? v:max_val;
        }

        // initialize histogram
        int [] hist = new int[max_val+1];

        // compute histogram
        for (int i = 0; i < size; i++) {

            int pixel_val = 0xff & data[i];        // pixel value becomes index for histogram array
            hist[pixel_val] += 1;

        }

        // apply new pixel value with normalized CDF

        for (int i = 0; i < size; i++) {

            int val = data[i] & 0xff;

            //              normalize
            // =============================================
            // find cdfmin

            int cdfmin = 0;

            // find first non zero value (cdfmin)
            for (int n = 0; n < hist.length; n++) {

                if (hist[n] == 0)
                    continue;

                else {

                    cdfmin = hist[n];
                    break;
                }
            }

            //          create CDF function
            // =====================================

            // number of pixels whose value is less than or equal than
            int cdf_out = 0;

            for (int k = 0; k < val+1; k++) {

                cdf_out += hist[k];
            }

            // =====================================

            // determined normalized value
            int L = 256;
            int M = width;
            int N = height;

            int h = Math.round((L-1) * (cdf_out - cdfmin) / (M*N-1));

            // =============================================

            histeqData[i] = (byte)(h & 0xff);
        }



        // *********************** End YOUR CODE HERE  **************************** //
        // We copy the colorness part for you, do not modify if you want rgb images
        for(int i=size; i<data.length; i++){
            histeqData[i] = data[i];
        }
        return histeqData;
    }

    public int[] conv2(byte[] data, int width, int height, double kernel[][]){
        // 0 is black and 255 is white.
        int size = height * width;
        int[] convData = new int[size];

        // Perform single channel 2D Convolution
        // Note that you only need to manipulate data[0:size] that corresponds to luminance
        // The rest data[size:data.length] is ignored since we only want grayscale output
        // *********************** START YOUR CODE HERE  **************************** //

        // reverse kernel (rotate 180)
        // ===============================================

        // 1. transpose matrix
        for (int i = 0; i < kernel.length; i++) {
            for (int j = 0; j < i; j++) {
                double temp = kernel[i][j];
                kernel[i][j] = kernel[j][i];
                kernel[j][i] = temp;
            }
        }

        // 2. reverse rows matrix
        for (int i = 0; i < kernel.length; i++) {

            Collections.reverse(Arrays.asList(kernel[i]));
        }

        // 3. transpose matrix
        for (int i = 0; i < kernel.length; i++) {
            for (int j = 0; j < i; j++) {
                double temp = kernel[i][j];
                kernel[i][j] = kernel[j][i];
                kernel[j][i] = temp;
            }
        }

        // 4. reverse rows matrix
        for (int i = 0; i < kernel.length; i++) {

            Collections.reverse(Arrays.asList(kernel[i]));
        }

        // perform convolution
        for (int i = 0; i < height; i++) {

            for (int j = 0; j < width; j++) {

                double val = 0;

                for (int m = -1; m < 2; m++) {

                    for (int n = -1; n < 2; n++) {

                        int data_idx = (i+m)*width+(j+n);
                        int data_val = ((data_idx >= 0) && (data_idx < size)) ? (0xff&data[data_idx]):0;
                        double kernel_val = kernel[m+1][n+1];

                        val += data_val * kernel_val;
                    }
                }

                convData[i*width+j] = (int)val;
            }
        }


        // *********************** End YOUR CODE HERE  **************************** //
        return convData;
    }

}

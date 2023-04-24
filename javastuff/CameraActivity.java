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

    int[][] QuantTableY = {{16, 11, 10, 16, 24, 40, 51, 61},
            {12, 12, 14, 19, 26, 58, 60, 55},
            {14, 13, 16, 24, 40, 57, 69, 56},
            {14, 17, 22, 29, 51, 87, 80, 62},
            {18, 22, 37, 56, 68, 109, 103, 77},
            {24, 35, 55, 64, 81, 104, 113, 92},
            {49, 64, 78, 87, 103, 121, 120, 101},
            {72, 92, 95, 98, 112, 100, 103, 99}};


    int[][] QuantTableC = {{17, 18, 24, 47, 99, 99, 99, 99},
            {18, 21, 26, 66, 99, 99, 99, 99},
            {24, 26, 56, 99, 99, 99, 99, 99},
            {47, 66, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99}};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.UNKNOWN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        super.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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

        int[][][] pic = bytes2pic(data);
        pic = process_img(pic, QuantTableY, QuantTableC, 50);
        byte[] newdata = pic2bytes(pic);
        int[] rgb = yuv2rgb(newdata);

        // Create ARGB Image, rotate and draw
        Bitmap bmp = Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        canvas.drawBitmap(bmp, new Rect(0,0, height, width), new Rect(0,0, canvas.getWidth(), canvas.getHeight()),null);
    }

    public int[][][] bytes2pic(byte[] data) {
        int[][][] pic = new int[3][height][width];
        for (int y = 0; y < height; ++y) for (int x = 0; x < width; ++x) {
            pic[0][y][x] = data[width*y+x];
        }
        for (int y = 0; y < height/2; ++y) for (int x = 0; x < width/2; ++x) {
            int i = 2*x;
            int j = 2*y;
            pic[1][j][i] = data[width*height+width/2*y+x];
            pic[1][j+1][i] = data[width*height+width/2*y+x];
            pic[1][j][i+1] = data[width*height+width/2*y+x];
            pic[1][j+1][i+1] = data[width*height+width/2*y+x];
            pic[2][j][i] = data[width*height*5/4+width/2*y+x];
            pic[2][j+1][i] = data[width*height*5/4+width/2*y+x];
            pic[2][j][i+1] = data[width*height*5/4+width/2*y+x];
            pic[2][j+1][i+1] = data[width*height*5/4+width/2*y+x];
        }
        return pic;
    }

    public byte[] pic2bytes(int[][][] pic) {
        byte[] data = new byte[width*height*3/2];
        for (int y = 0; y < height; ++y) for (int x = 0; x < width; ++x) {
            data[width*y+x] = (byte)pic[0][y][x];
        }
        for (int y = 0; y < height/2; ++y) for (int x = 0; x < width/2; ++x) {
            int i = 2*x;
            int j = 2*y;
            byte avg1 = (byte)((pic[1][j][i] + pic[1][j+1][i] + pic[1][j][i+1] + pic[1][j+1][i+1])/4);
            data[width*height+width/2*y+x] = avg1;
            byte avg2 = (byte)((pic[2][j][i] + pic[2][j+1][i] + pic[2][j][i+1] + pic[2][j+1][i+1])/4);
            data[width*height*5/4+width/2*y+x] = avg2;
        }
        return data;
    }

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

    public static double coeff(int x) {

        if (x == 0)
            return 1/Math.sqrt(2);

        else if (x > 0)
            return 1;

        else
            return -1;
    }

    public static double[][] DCT(int [][] x) {

        if (x.length != x[0].length)
            return null;

        int N = x.length;

        // init dct array
        double [][] dct = new double[N][N];


        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {

                double val = 0;

                for (int m = 0; m < N; m++) {
                    for (int n = 0; n < N; n++) {

                        double curr_pixel = x[m][n];

                        double cos1 = Math.cos(((2*m+1)*Math.PI*i) / (2*N));
                        double cos2 = Math.cos(((2*n+1)*Math.PI*j) / (2*N));

                        val += curr_pixel * cos1 * cos2;

                    }
                }

                val *= 1/Math.sqrt(2*N) * coeff(i) * coeff(j);

                dct[i][j] = val;
            }
        }

        return dct;
    }

    public static int[][] idct(double [][] x) {

        if (x.length != x[0].length)
            return null;

        int N = x.length;

        // init dct array
        int [][] idct = new int[N][N];


        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {

                double val = 0;

                for (int m = 0; m < N; m++) {
                    for (int n = 0; n < N; n++) {

                        double curr_pixel = x[m][n];

                        double cos1 = Math.cos(((2*j+1)*Math.PI*m) / (2*N));
                        double cos2 = Math.cos(((2*i+1)*Math.PI*n) / (2*N));

                        val += curr_pixel * cos1 * cos2 * coeff(m) * coeff(n);

                    }
                }

                val *= 1/Math.sqrt(2*N);

                idct[i][j] = (int)val;
            }
        }

        return idct;
    }

    public static int [][] scaleQuantTable (int [][] qt, int qf) {

        int N = qt.length;

        double s = (qf < 50) ? 5000/qf:(200 - 2*qf);

        int [][] t = new int [qt.length][qt[0].length];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {

                t[j][i] = (int)Math.floor((s * qt[j][i] + 50) / 100);
            }
        }

        return t;

    }

    public static int [][] quantize (double [][] x, int [][] q) {

        // input must be 8x8
        if (x.length != x[0].length)
            return null;

        int N = x.length;

        int [][] B = new int [N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {

                B[i][j] = (int)Math.round(x[i][j] / q[i][j]);
            }
        }

        return B;

    }

    public static double [][] unquantize (int [][] x, int [][] q) {

        // input must be 8x8
        if (x.length != x[0].length)
            return null;

        int N = x.length;

        double [][] B = new double [N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {

                B[i][j] = x[i][j] * q[i][j];
            }
        }

        return B;

    }

    public static int [][] block_split (int [][] img, int row, int col) {

        int img_row_start = 8*row;
        int img_col_start = 8*col;

        int height = img.length;
        int width = img[0].length;

        int [][] block = new int [8][8];

        int block_row_end = Math.min(8, height - img_row_start);
        int block_col_end = Math.min(8, width - img_col_start);


        for (int i = 0; i < block_row_end; i++) {
            for (int j = 0; j < block_col_end; j++) {

                block[i][j] = img[img_row_start + i][img_col_start + j];
            }
        }

        return block;
    }

    public static int[][] block_combine (int[][] new_img, int row, int col, int[][] block) {

        int img_row_start = 8*row;
        int img_col_start = 8*col;

        int height = new_img.length;
        int width = new_img[0].length;

        int block_row_end = Math.min(8, height - img_row_start);
        int block_col_end = Math.min(8, width - img_col_start);

        for (int i = 0; i < block_row_end; i++) {
            for (int j = 0; j < block_col_end; j++) {

                new_img[img_row_start + i][img_col_start + j] = block[i][j];
            }
        }

        return new_img;
    }

    public static void print_mat_int (int [][] x) {

        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[0].length; j++) {

                System.out.print(x[i][j]);
                System.out.print(" ");
            }

            System.out.print("\n");
        }

        System.out.print("\n");
    }

    public static void print_mat_double (double [][] x) {

        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[0].length; j++) {

                System.out.print(x[i][j]);
                System.out.print(" ");
            }

            System.out.print("\n");
        }

        System.out.print("\n");
    }
    public static int[][][] process_img (int [][][] img, int [][] qty, int [][] qtc, int qf) {

        int channel = img.length;
        int height = img[0].length;
        int width = img[0][0].length;

        int [][][] new_img = new int [channel][height][width];

        int [][] quanty = scaleQuantTable(qty, qf);
        int [][] quantc = scaleQuantTable(qtc, qf);

        for (int c = 0; c < channel; c++) {
            for (int i = 0; i < Math.ceil(width/8); i++) {
                for (int j = 0; j < Math.ceil(height/8); j++) {

                    int [][] block = block_split(img[c], j, i);
                    /*
                    double [][] dct = DCT(block);

                    int[][] Q;
                    double [][] uQ;

                    if (c == 0) {
                        Q = quantize(dct, quanty);
                        uQ = unquantize(Q, quanty);
                    }
                    else {
                        Q = quantize(dct, quantc);
                        uQ = unquantize(Q, quantc);
                    }
                    int[][] idct = idct(uQ);
                    */
                    new_img[c] = block_combine(new_img[c], j, i, block);

                }
            }
        }
        return new_img;
    }

}
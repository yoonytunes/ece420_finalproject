package com.ece420.lab6;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.BitmapFactory;

import org.w3c.dom.Text;

import java.io.IOException;
// import java.util.List;


public class ShowActivity extends AppCompatActivity {

    // UI Variable
    private ImageView originalView;
    public static ImageView compressedView;
    private TextView qfText;
    private TextView ratioText;

    public int qf = MainActivity.qf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.UNKNOWN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_show);
        super.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Setup Text Views
        qfText = (TextView) findViewById(R.id.qfText);
        qfText.setText(String.valueOf(qf));

        ratioText = (TextView) findViewById(R.id.ratioText);
        ratioText.setText("N/A");

        // Setup Image Views
        originalView = (ImageView) findViewById(R.id.ViewOrigin);
        compressedView = (ImageView) findViewById(R.id.ViewCompressed);

        // Read image into image view
        String path = android.os.Environment.getExternalStorageDirectory() + "/Download/kitten.png";
        java.io.File imgFile = new java.io.File(path);
        final Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        originalView.setImageBitmap(bitmap);

        // Compress and put into image view
        new Compress().execute(bitmap);
    }

    public class Compress extends AsyncTask<Bitmap, Integer, Bitmap> {
        protected Bitmap doInBackground(Bitmap... bitmap) {
            double[][][] img = image.bitmap2img(bitmap[0]);
            double[][][] newimg = image.process_img(img, qf);
            return image.img2bitmap(newimg);
        }

        protected void onPostExecute(Bitmap bitmap) {
            ShowActivity.compressedView.setImageBitmap(bitmap);
        }
    }
}

package com.ece420.lab6;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.BitmapFactory;
// import java.util.List;


public class ShowActivity extends AppCompatActivity {

    // UI Variable
    private ImageView originalView;
    public static ImageView compressedView;
    private TextView qfText;
    private TextView ratioText;
    private String osPath = android.os.Environment.getExternalStorageDirectory() + "";
    public String ogPath;
    public String compressedPath;

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
        Bitmap bitmap;
        if (MainActivity.app == 1) {
            ogPath = osPath + "/Download/kitten.png";
            compressedPath = osPath + "/Download/kitten_compressed.bin";
            java.io.File imgFile = new java.io.File(ogPath);
            bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        }
        else {
            bitmap = CameraActivity.pic;
            ogPath = osPath + "/Download/your_picture.png";
            compressedPath = osPath + "/Download/your_picture_compressed.bin";
            new saveImage().execute(bitmap);
        }
        originalView.setImageBitmap(bitmap);

        // Compress and put into image view
        new Compress().execute(bitmap);
    }

    public class saveImage extends AsyncTask<Bitmap, Integer, Integer> {
        protected Integer doInBackground(Bitmap... bitmap) {
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(ogPath)) {
                bitmap[0].compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (java.io.IOException e) {e.printStackTrace();}
            return 1;
        }
        protected void onPostExecute(Bitmap bitmap) {}
    }

    public class Compress extends AsyncTask<Bitmap, Integer, Bitmap> {
        protected Bitmap doInBackground(Bitmap... bitmap) {
            double[][][] img = image.bitmap2img(bitmap[0]);
            image.compress_img(img, qf, compressedPath);
            img = image.decompress_img(compressedPath);
            return image.img2bitmap(img);
        }

        protected void onPostExecute(Bitmap bitmap) {
            ShowActivity.compressedView.setImageBitmap(bitmap);
            java.io.File original_file = new java.io.File(ogPath);
            java.io.File compressed_file = new java.io.File(compressedPath);
            ratioText.setText(String.valueOf(Math.floor(((double)original_file.length()/compressed_file.length())*100)/100));
        }
    }
}

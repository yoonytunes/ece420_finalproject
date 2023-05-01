package com.ece420.lab6;

import android.graphics.Bitmap;

public class image {
    public static int[][] QuantTableY = {{16, 11, 10, 16, 24, 40, 51, 61},
            {12, 12, 14, 19, 26, 58, 60, 55},
            {14, 13, 16, 24, 40, 57, 69, 56},
            {14, 17, 22, 29, 51, 87, 80, 62},
            {18, 22, 37, 56, 68, 109, 103, 77},
            {24, 35, 55, 64, 81, 104, 113, 92},
            {49, 64, 78, 87, 103, 121, 120, 101},
            {72, 92, 95, 98, 112, 100, 103, 99}};


    public static int[][] QuantTableC = {{17, 18, 24, 47, 99, 99, 99, 99},
            {18, 21, 26, 66, 99, 99, 99, 99},
            {24, 26, 56, 99, 99, 99, 99, 99},
            {47, 66, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99},
            {99, 99, 99, 99, 99, 99, 99, 99}};

    public static double[] RGBbyte2YCbCr (int in) {
        double R = (in >> 16) & 0xFF;
        double G = (in >> 8) & 0xFF;
        double B = in & 0xFF;

        double Y = R/4 + G/2 + B/4;
        double Cb = -R/6 - G/3 + B/2;
        double Cr = R/2 - G/3 - B/6;

        return new double[]{Y, Cb, Cr};
    }

    public static int YCbCr2RGBbyte (double[] in) {
        int R = Math.min(Math.max((int)Math.round(in[0] + 1.5*in[2]), 0), 255);
        int G = Math.min(Math.max((int)Math.round(in[0] - 0.75*(in[1] + in[2])), 0), 255);
        int B = Math.min(Math.max((int)Math.round(in[0] + 1.5*in[1]), 0), 255);

        return (0xFF << 24) | (R << 16) | (G << 8) | B;
    }

    public static double[][][] bitmap2img (Bitmap bitmap) {
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();

        double[][][] img = new double[3][h][w];

        for (int i = 0; i < w; ++i) for (int j = 0; j < h; ++j) {
            double[] color = RGBbyte2YCbCr(bitmap.getPixel(i, j));
            img[0][j][i] = color[0];
            img[1][j][i] = color[1];
            img[2][j][i] = color[2];
        }
        return img;
    }

    public static Bitmap img2bitmap (double[][][] img) {
        int h = img[0].length;
        int w = img[0][0].length;

        int[] data = new int[h*w];

        for (int i = 0; i < w; ++i) for (int j = 0; j < h; ++j) {
            double[] color = {img[0][j][i], img[1][j][i], img[2][j][i]};
            data[w * j + i] = YCbCr2RGBbyte(color);
        }

        return Bitmap.createBitmap(data, w, h, Bitmap.Config.ARGB_8888);
    }

    public static double coeff(int x) {

        if (x == 0)return 1/Math.sqrt(2);

        else if (x > 0)
            return 1;

        else
            return -1;
    }

    public static double[][] DCT(double [][] x) {
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
                        double curr_pixel = x[n][m];

                        double cos1 = Math.cos(((2*m+1)*Math.PI*i) / (2*N));
                        double cos2 = Math.cos(((2*n+1)*Math.PI*j) / (2*N));

                        val += curr_pixel * cos1 * cos2;
                    }
                }

                val *= 1/Math.sqrt(2*N) * coeff(i) * coeff(j);

                dct[j][i] = val;
            }
        }

        return dct;
    }

    public static double[][] idct(double [][] x) {
        if (x.length != x[0].length)
            return null;

        int N = x.length;

        // init dct array
        double [][] idct = new double[N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                double val = 0;

                for (int m = 0; m < N; m++) {
                    for (int n = 0; n < N; n++) {
                        double curr_pixel = x[n][m];

                        double cos1 = Math.cos(((2*i+1)*Math.PI*m) / (2*N));
                        double cos2 = Math.cos(((2*j+1)*Math.PI*n) / (2*N));

                        val += curr_pixel * cos1 * cos2 * coeff(m) * coeff(n);
                    }
                }

                val *= 1/Math.sqrt(2*N);

                idct[j][i] = val;
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

    public static double[][] quantize (double [][] x, int [][] q) {
        // input must be 8x8
        if (x.length != x[0].length)
            return null;

        int N = x.length;

        double[][] B = new double[N][N];

        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++)
            B[i][j] = Math.round(x[i][j] / q[i][j]);

        return B;
    }

    public static double[][] unquantize (double [][] x, int [][] q) {
        // input must be 8x8
        if (x.length != x[0].length)
            return null;

        int N = x.length;

        double[][] B = new double[N][N];

        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++)
            B[i][j] = x[i][j] * q[i][j];

        return B;
    }

    public static double [][] block_split (double [][] img, int row, int col) {

        int img_row_start = 8*row;
        int img_col_start = 8*col;

        int height = img.length;
        int width = img[0].length;

        double [][] block = new double [8][8];

        int block_row_end = Math.min(8, height - img_row_start);
        int block_col_end = Math.min(8, width - img_col_start);


        for (int i = 0; i < block_row_end; i++) {
            for (int j = 0; j < block_col_end; j++) {
                block[i][j] = img[img_row_start + i][img_col_start + j];
            }
        }

        return block;
    }

    public static double[][] block_combine (double [][] img, int row, int col, double[][] block) {

        int img_row_start = 8*row;
        int img_col_start = 8*col;

        int height = img.length;
        int width = img[0].length;

        int block_row_end = Math.min(8, height - img_row_start);
        int block_col_end = Math.min(8, width - img_col_start);

        for (int i = 0; i < block_row_end; i++) {
            for (int j = 0; j < block_col_end; j++) {
                img[img_row_start + i][img_col_start + j] = block[i][j];
            }
        }

        return img;
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
    public static double[][][] process_img (double [][][] img, int qf) {
        int h = img[0].length;
        int w = img[0][0].length;

        int [][] quanty = scaleQuantTable(QuantTableY, qf);
        int [][] quantc = scaleQuantTable(QuantTableC, qf);

        double[][][] newimg = new double[3][h][w];

        for (int c = 0; c < 3; c++) {
            for (int i = 0; i < Math.ceil(w/8); i++) {
                for (int j = 0; j < Math.ceil(h/8); j++) {
                    double[][] block = block_split(img[c], j, i);

                    block = DCT(block);

                    if (c == 0) {
                        block = quantize(block, quanty);
                        block = unquantize(block, quanty);
                    }
                    else {
                        block = quantize(block, quantc);
                        block = unquantize(block, quantc);
                    }

                    block = idct(block);

                    newimg[c] = block_combine(img[c], j, i, block);
                }
            }
        }

        return img;
    }

    public static int [] zigzag (int [][] x) {

        int N = 8;
        int [] s = new int[N*N];


        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {

                int k = i + j;
                float m,l;

                if (k < 8) {

                    l = k*(k+1) / 2;

                    if (k%2 == 0)
                        m = l + i;
                    
                    else
                        m = l + j;
                        
                }

                else if (k == 8)
                    m = 35 + i;

                else {
                    
                    k = 15 - k;
                    l = 71 - (k*(k+1))/2;

                    if (k%2 == 0) 
                        m = l - i;
                    
                    else
                        m = l - j;

                }

                s[(int)m] = x[j][i];
            }
        }

        return s;
    }

    public static int [][] runLengthEncode (int [] x) {

        int [][] s = new int[64][2];

        int rl = 0;

        for (int i = 0; i < 64; i++) {

            int amplitude = x[i];

            if (amplitude == 0) {
                rl +=1;
                continue;
            }

            s[i][0] = rl;
            s[i][1] = amplitude;
            rl = 0;
        }

        return s;
    }

}

package com.ece420.lab6;

import android.graphics.Bitmap;

import java.io.ByteArrayInputStream;

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

    public static void quantize (double [][] x, int [][] q) {
        // input must be 8x8
        if (x.length != x[0].length)
            return;

        int N = x.length;

        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++)
            x[i][j] = Math.round(x[i][j] / q[i][j]);
    }

    public static void unquantize (double [][] x, int [][] q) {
        // input must be 8x8
        if (x.length != x[0].length)
            return;

        int N = x.length;

        double[][] B = new double[N][N];

        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++)
            x[i][j] = x[i][j] * q[i][j];
    }

    public static void block_split (double[][] img, int row, int col, double[][] block) {
        int img_row_start = 8*row;
        int img_col_start = 8*col;

        int height = img.length;
        int width = img[0].length;

        int block_row_end = Math.min(8, height - img_row_start);
        int block_col_end = Math.min(8, width - img_col_start);

        for (int i = 0; i < block_row_end; i++) {
            for (int j = 0; j < block_col_end; j++) {
                block[i][j] = img[img_row_start + i][img_col_start + j];
            }
        }
    }

    public static void block_combine (double [][] img, int row, int col, double[][] block) {
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
    }

    public static void zigzag (double [][] x, double[] s) {
        int N = 8;

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int k = i + j;
                int m,l;

                if (k < 8) {
                    l = k*(k+1) / 2;
                    if (k%2 == 0) m = l + i;
                    else m = l + j;
                }
                else if (k == 8)
                    m = 35 + i;
                else {
                    k = 15 - k;
                    l = 71 - (k*(k+1))/2;
                    if (k%2 == 0) m = l - i;
                    else m = l - j;
                }
                s[m] = x[j][i];
            }
        }
    }

    public static void unzigzag (double [] x, double[][] s) {
        int N = 8;

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                int k = i + j;
                int m,l;

                if (k < 8) {
                    l = k*(k+1) / 2;
                    if (k%2 == 0) m = l + i;
                    else m = l + j;
                }
                else if (k == 8)
                    m = 35 + i;
                else {
                    k = 15 - k;
                    l = 71 - (k*(k+1))/2;
                    if (k%2 == 0) m = l - i;
                    else m = l - j;
                }
                s[j][i] = x[m];
            }
        }
    }

    public static double[][] runLengthEncode (double [] x) {
        double[][] s = new double[64][2];
        int rl = 0;
        int idx = 0;

        for (int i = 0; i < 64; i++) {
            double amplitude = x[i];
            if (amplitude == 0) {
                rl +=1;
                continue;
            }
            s[idx][0] = rl;
            s[idx][1] = amplitude;
            rl = 0;
            idx++;
        }

        return s;
    }

    public static double[] runlengthdecode(double[][] x) {
        double[] s = new double[64];

        int idx = 0;
        for (int i = 0; i < 64; ++i) {
            idx += (int)(x[i][0]);
            s[idx] = x[i][1];
            idx++;
            if (idx >= 64) break;
        }

        return s;
    }

    public static int log2(int x) {
        return (int) (Math.log(x) / Math.log(2));
    }

    public static byte[] runlength_to_bitstream(double[][] x) {
        java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < x.length; ++i) {
            int value = (int)x[i][1];
            if (value == 0) break;
            int size = (int)Math.ceil((Math.floor(log2(Math.abs(value))) + 2)/8);
            int zeros = (int)x[i][0];
            while (zeros >= 15) {
                bs.write(240);
                zeros -= 15;
            }
            bs.write((16*zeros+size));
            bs.write(value);
        }
        return bs.toByteArray();
    }

    public static double[][] bitstream_to_runlength(byte[] x) {
        double[][] s = new double[64][2];
        int idx = 0;
        int zeros = 0;
        int size = 0;
        java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < x.length; ++i) {
            if (size == 0) {
                int b = x[i] & 0xFF;
                zeros += b >> 4;
                size = (int)Math.ceil(b & 0xF);
                bs.reset();
            }
            else {
                bs.write(x[i]);
                size--;
                if (size == 0) {
                    s[idx] = new double[]{zeros, new java.math.BigInteger(bs.toByteArray()).intValue()};
                    zeros = 0;
                    idx++;
                }
            }
        }
        return s;
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

    public static void print_byte_array (byte[] x) {
    for (int i = 0; i < x.length; i++) {
            System.out.print(x[i]);
            System.out.print(" ");
        }
        System.out.print("\n");
    }

    public static void compress_img (double[][][] img, int qf, String filename) {
        java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
        bs.write(qf);

        int h = img[0].length;
        bs.write((int)Math.ceil((Math.floor(log2(h)) + 2)/8));
        byte[] hbytes = java.math.BigInteger.valueOf(h).toByteArray();
        bs.write(hbytes, 0, hbytes.length);
        int w = img[0][0].length;
        bs.write((int)Math.ceil((Math.floor(log2(w)) + 2)/8));
        byte[] wbytes = java.math.BigInteger.valueOf(w).toByteArray();
        bs.write(wbytes, 0, wbytes.length);

        int [][] quanty = scaleQuantTable(QuantTableY, qf);
        int [][] quantc = scaleQuantTable(QuantTableC, qf);

        double[][] block = new double[8][8];
        double[] array = new double[64];

        for (int c = 0; c < 3; c++) {
            for (int i = 0; i < Math.ceil(w / 8); i++) {
                for (int j = 0; j < Math.ceil(h / 8); j++) {
                    block_split(img[c], j, i, block);
                    block = DCT(block);

                    if (c == 0) quantize(block, quanty);
                    else quantize(block, quantc);

                    zigzag(block, array);
                    double[][] data = runLengthEncode(array);
                    byte[] bytes = runlength_to_bitstream(data);
                    bs.write(bytes.length);
                    bs.write(bytes, 0, bytes.length);
                }
            }
        }

        java.io.File file = new java.io.File(filename);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
            fos.write(bs.toByteArray());
            System.out.println("Successfully written data to the file");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public static double[][][] decompress_img (String filename) {
        java.io.File file = new java.io.File(filename);
        double[][][] img = null;
        byte[] bytes;
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            int qf = fis.read();
            int hsize = fis.read();
            byte[] hbytes = new byte[hsize];
            fis.read(hbytes, 0, hsize);
            int h = new java.math.BigInteger(hbytes).intValue();
            int wsize = fis.read();
            fis.read(bytes = new byte[wsize]);
            int w = new java.math.BigInteger(bytes).intValue();

            img = new double[3][h][w];

            int [][] quanty = scaleQuantTable(QuantTableY, qf);
            int [][] quantc = scaleQuantTable(QuantTableC, qf);

            double[][] block = new double[8][8];
            double[] array = new double[64];

            for (int c = 0; c < 3; c++) {
                for (int i = 0; i < Math.ceil(w / 8); i++) {
                    for (int j = 0; j < Math.ceil(h / 8); j++) {
                        int num = fis.read();
                        fis.read(bytes = new byte[num]);
                        double[][] data  = bitstream_to_runlength(bytes);
                        array = runlengthdecode(data);
                        unzigzag(array, block);

                        if (c == 0) unquantize(block, quanty);
                        else unquantize(block, quantc);

                        block_combine(img[c], j, i, idct(block));
                    }
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return img;
    }

    public static void process_img (double[][][] img, int qf) {
        int h = img[0].length;
        int w = img[0][0].length;

        int [][] quanty = scaleQuantTable(QuantTableY, qf);
        int [][] quantc = scaleQuantTable(QuantTableC, qf);

        double[][] block = new double[8][8];
        double[] array = new double[64];

        for (int c = 0; c < 3; c++) {
            for (int i = 0; i < Math.ceil(w / 8); i++) {
                for (int j = 0; j < Math.ceil(h / 8); j++) {
                    block_split(img[c], j, i, block);
                    block = DCT(block);

                    if (c == 0) quantize(block, quanty);
                    else quantize(block, quantc);

                    zigzag(block, array);
                    double[][] data = runLengthEncode(array);
                    byte[] bytes = runlength_to_bitstream(data);
                    data = bitstream_to_runlength(bytes);
                    array = runlengthdecode(data);
                    unzigzag(array, block);

                    if (c == 0) unquantize(block, quanty);
                    else unquantize(block, quantc);

                    block_combine(img[c], j, i, idct(block));
                }
            }
        }
    }
}

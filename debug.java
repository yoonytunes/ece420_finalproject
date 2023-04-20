/******************************************************************************

                            Online Java Compiler.
                Code, Compile, Run and Debug java program online.
Write your code in this editor and press "Run" button to execute it.

*******************************************************************************/

public class Main
{
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
    
    public static int [][] generate_2d (int row, int col) {
        
        int [][] x = new int [row][col];
        
        for (int i = 0; i < row; i++) {
            for(int j = 0; j < col; j++) {
                
                x[i][j] = i*col+j;
            }
        }
        
        return x;
    }
    
    public static int [][][] generate_test_3d (int [][] t) {
        
        int row = t.length;
        int col = t[0].length;
        int channel = 3;
        
        int [][][] x = new int [channel][row][col];
        
        for (int i = 0; i < channel; i++) {
            
            x[i] = t;
        }
        
        return x;
    }
    
    public static int [][][] generate_3d (int row, int col, int channel) {
        
        int [][][] x = new int [channel][row][col];
        
        for (int i = 0; i < channel; i++) {
            
            x[i] = generate_2d(8,8);
        }
        
        return x;
    }
    
    public static void process_img (int [][][] img, int [][] qty, int [][] qtc, int qf) {
        
        int channel = img.length;
        int height = img[0].length;
        int width = img[0][0].length;
        
        
        int [][] quanty = scaleQuantTable(qty, qf);
        int [][] quantc =scaleQuantTable(qtc, qf);
        
        for (int c = 0; c < channel; c++) {
            for (int i = 0; i < Math.ceil(width/8); i++) {
                for (int j = 0; j < Math.ceil(height/8); j++) {
                    
                    int [][] block = block_split(img[c], j, i);
                    
                    double [][] dct = DCT(block);
                    
                    int[][] Q;
                    
                    if (c == 0)
                        Q = quantize(dct, quanty);
                        
                    else
                        Q = quantize(dct, quantc);
                        
                    print_mat_int(Q);
                    
                }
            }
        }
    }
	public static void main(String[] args) {
		
		int[][] test = {{-76, -73, -67, -62, -58, -67, -64, -55},
                        {-65, -69, -73, -38, -19, -43, -59, -56},
                        {-66, -69, -60, -15,  16, -24, -62, -55},
                        {-65, -70, -57,  -6,  26, -22, -58, -59},
                        {-61, -67, -60, -24,  -2, -40, -60, -58},
                        {-49, -63, -68, -58, -51, -60, -70, -53},
                        {-43, -57, -64, -69, -73, -67, -63, -45},
                        {-41, -49, -59, -60, -63, -52, -50, -34}};
                        
        
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


                        
        int [][][] test_img = generate_test_3d(test);
        
        int q_factor = 50;
        
        process_img (test_img, QuantTableY, QuantTableC, q_factor);
               
               
               
        // int [][] block_test = generate_2d(20,20);
        
        
        // double [][] dct = DCT(test);
        
        // print_mat_double(dct);
        
        
        // int [][] output = quantize(dct, scaleQuantTable(QuantTableY,50));
        
        
        // print_mat_int(output);
        
    
        // print_mat_int(block_test);
        
        // int [][] tile = block_split(block_test, 2, 2);
        
        // print_mat_int(tile);
               
               
               
               
               
               
               
               
               
               

	}
}

package com.thisisnsh.gpu;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.renderscript.Allocation;
import androidx.renderscript.Element;
import androidx.renderscript.RenderScript;

import java.util.Random;

public class MainActivity extends AppCompatActivity {


    private ImageView im1, im2, imcpu, imgpu, imd, im1bw, im2bw;
    private TextView stats, statsd, dim1, dim2;
    private EditText one_row, one_col, two_col;
    private Button button;
    private CardView calc;

    private RenderScript mRS;

    private Allocation allocationA; // allocation of A matrix
    private Allocation allocationB; // allocation of B matrix
    private Allocation allocationC; // allocation of C matrix
    private Allocation allocationKSize; // allocation of Row Size of Matrix A;
    private Allocation allocationNSize; // allocation of Col Size of Matrix B;
    private Allocation allocationPosRow;

    private ScriptC_multiply mScript;

    private float[] matA;
    private float[] matB;
    private float[] outMatrix;

    private int[] pos_row;
    private int[] mSize;
    private int[] nSize;
    private int[] kSize;

    private Random rand = new Random();

    private int[][] cpuMatA;
    private int[][] cpuMatB;
    private int[][] cpuMatC;

    protected int A_row = 0;
    protected int A_col = 0;
    protected int B_col = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        im1 = findViewById(R.id.im1);
        im2 = findViewById(R.id.im2);
        im1bw = findViewById(R.id.im1bw);
        im2bw = findViewById(R.id.im2bw);
        imcpu = findViewById(R.id.imcpu);
        imgpu = findViewById(R.id.imgpu);
        imd = findViewById(R.id.imd);
        dim1 = findViewById(R.id.dim1);
        dim2 = findViewById(R.id.dim2);
        stats = findViewById(R.id.stats);
        statsd = findViewById(R.id.statsd);

        one_row = findViewById(R.id.one_row);
        one_col = findViewById(R.id.one_col);
        two_col = findViewById(R.id.two_col);

        button = findViewById(R.id.button);
        calc = findViewById(R.id.calc);

        final Bitmap src1 = BitmapFactory.decodeResource(this.getResources(), R.drawable.one);
        dim1.setText("Dim: " + src1.getHeight() + " X " + src1.getWidth());

//        for (int x = 0; x < src1.getWidth(); x+=1) {
//            for (int y = 0; y < src1.getHeight(); y+=1) {
//                int pixel = src1.getPixel(x, y);
//
//                int A = Color.alpha(pixel);
//
//                int R = Color.red(pixel);
//                int G = Color.green(pixel);
//                int B = Color.blue(pixel);
//
//                R = G = B = (int) (0.299 * R + 0.587 * G + 0.114 * B);
//
//                bitmap1.setPixel(x, y, Color.argb(A, R, G, B));
//            }
//        }

//        im1bw.setImageBitmap(bitmap1);

        final Bitmap src = BitmapFactory.decodeResource(this.getResources(), R.drawable.two2);
//        final Bitmap bitmap2 = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        dim2.setText("Dim: " + src.getHeight() + " X " + src.getWidth());


//        for (int x = 0; x < src.getWidth(); x++) {
//            for (int y = 0; y < src.getHeight(); y++) {
//                int pixel = src.getPixel(x, y);
//
//                int A = Color.alpha(pixel);
//                int R = Color.red(pixel);
//                int G = Color.green(pixel);
//                int B = Color.blue(pixel);
//
//                R = G = B = (int) (0.299 * R + 0.587 * G + 0.114 * B);
//
//                bitmap2.setPixel(x, y, Color.argb(A, R, G, B));
//            }
//        }

//        im2bw.setImageBitmap(bitmap2);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!String.valueOf(one_row.getText()).equals("") && !String.valueOf(one_row.getText()).equals("null") && !String.valueOf(two_col.getText()).equals("") && !String.valueOf(two_col.getText()).equals("null") && !String.valueOf(one_col.getText()).equals("null") && !String.valueOf(one_col.getText()).equals("")) {
                    calc.setVisibility(View.VISIBLE);
                    A_row = Integer.parseInt(String.valueOf(one_row.getText()));
                    A_col = Integer.parseInt(String.valueOf(one_col.getText()));
                    B_col = Integer.parseInt(String.valueOf(two_col.getText()));
                    initMatrix(src1, src);
                } else {
                    Toast.makeText(MainActivity.this, "Invalid Entry", Toast.LENGTH_SHORT).show();
                    calc.setVisibility(View.GONE);
                    A_row = 0;
                    A_col = 0;
                    B_col = 0;
                }

            }
        });
    }

    public float[] one_matrix(Bitmap bitmap) {

        float[] coverImageIntArray1D = new float[A_row * A_col];
        int index = 0;
        for (int y = 0; y < A_row; ++y) {
            for (int x = 0; x < A_col; ++x) {
                coverImageIntArray1D[index++] = bitmap.getPixel(x, y);
            }
        }

        return coverImageIntArray1D;
    }

    public float[] two_matrix(Bitmap bitmap) {
        float[] coverImageIntArray1D = new float[A_col * B_col];
        int index = 0;
        for (int y = 0; y < A_col; ++y) {
            for (int x = 0; x < B_col; ++x) {
                coverImageIntArray1D[index++] = bitmap.getPixel(x, y);
            }
        }
        return coverImageIntArray1D;
    }

    public int[] generate_row_idx(int m) {
        int[] r_idx = new int[m];
        for (int i = 0; i < m; i++) {
            r_idx[i] = i;
        }
        return r_idx;
    }

    public float[] CPU_matrix_multiplication() {
        float[] matC = new float[A_row * B_col];
        for (int i = 0; i < A_row; i++) {
            for (int j = 0; j < B_col; j++) {
                for (int k = 0; k < A_col; k++) {
                    matC[(i * B_col) + j] += (float) matA[(i * A_col) + k] * (float) matB[(B_col * k) + j];
                }
            }
        }
        return matC;
    }

    void initMatrix(Bitmap bitmap1, Bitmap bitmap2) {
        matA = one_matrix(bitmap1);

//        int[] intMatA = new int[A_row * A_col / 100];
//        for (int y = 0; y < matA.length; ++y) {
//                intMatA[y] = (int) matA[y];
//            }
//
//        Bitmap bm2 = Bitmap.createBitmap(A_col, A_row, Bitmap.Config.ARGB_8888);
//        bm2.setPixels(intMatA, 0, A_col, 0, 0, A_col, A_row);
//        im1.setImageBitmap(bm2);

        matB = two_matrix(bitmap2);

//        int[] intMatB = new int[A_col * B_col / 100];
//            for (int y = 0; y < matB.length; ++y) {
//            intMatB[y] = (int) matB[y];
//        }
//        Bitmap bm3 = Bitmap.createBitmap(B_col, A_col, Bitmap.Config.ARGB_8888);
//        bm3.setPixels(intMatB, 0, B_col, 0, 0, B_col, A_col);
//        im2.setImageBitmap(bm3);

        outMatrix = new float[A_row * B_col];
        pos_row = generate_row_idx(A_row);

        kSize = new int[1];
        nSize = new int[1];
        mSize = new int[1];

        kSize[0] = A_col;
        mSize[0] = A_row;
        nSize[0] = B_col;

        createScript();
    }

    private void createScript() {

        Random rand = new Random();
        for (int i = 0; i < matA.length; i++) {
            matA[i] = Color.red((int)matA[i]) + rand.nextFloat();
        }

        for (int i = 0; i < matB.length; i++) {
            matB[i] = Color.red((int)matB[i]) + rand.nextFloat();
        }

        mRS = RenderScript.create(this);

        int sizeA = A_row * A_col;
        int sizeB = A_col * B_col;
        int sizeC = A_row * B_col;

        // memory allocation part
        allocationA = Allocation.createSized(mRS, Element.F32(mRS), sizeA);
        allocationB = Allocation.createSized(mRS, Element.F32(mRS), sizeB);
        allocationC = Allocation.createSized(mRS, Element.F32(mRS), sizeC);

        allocationNSize = Allocation.createSized(mRS, Element.I32(mRS), 1);
        allocationKSize = Allocation.createSized(mRS, Element.I32(mRS), 1);
        allocationPosRow = Allocation.createSized(mRS, Element.I32(mRS), A_row);

        allocationA.copyFrom(matA);
        allocationB.copyFrom(matB);
        allocationPosRow.copyFrom(pos_row);
        allocationNSize.copyFrom(nSize);
        allocationKSize.copyFrom(kSize);

        //GPU Start Time
        long startTime = System.currentTimeMillis();
        mScript = new ScriptC_multiply(mRS);
        mScript.bind_matA(allocationA);
        mScript.bind_matB(allocationB);
        mScript.bind_outMatrix(allocationC);
        mScript.bind_kSize(allocationKSize);
        mScript.bind_nSize(allocationNSize);
        mScript.forEach_root(allocationPosRow);
        allocationC.copyTo(outMatrix);

        long endTime = System.currentTimeMillis();
        float gput = ((endTime - startTime) / 1f);
        //GPU End Time

        //CPU Start Time
        startTime = System.currentTimeMillis();
        float[] CPUMatC = CPU_matrix_multiplication();
        endTime = System.currentTimeMillis();
        //CPU End Time

        // Show SpeedUp Time
        float swt = ((endTime - startTime) / 1f);
        String stat = "CPU time: " + swt + "ms\nGPU time: " + gput + "ms\nThe speed up: " + swt / gput;
        stats.setText(stat);

        //GPU Mapping Start
        float gpu_min = Float.MAX_VALUE;
        float gpu_max = Float.MIN_VALUE;

//        for (int i = 0; i < outMatrix.length; i+=100) {
//            gpu_min = Math.min(gpu_min, outMatrix[i]);
//            gpu_max = Math.max(gpu_max, outMatrix[i]);
//        }

        float tempGPU = (gpu_max - gpu_min);
        if (tempGPU!=0)
            System.out.println(tempGPU);
        else
            Log.e("GPU System Error","Max - Min = 0");

//        int[] outMatrix1 = new int[A_row * B_col / 100];
//        for (int i = 0; i < outMatrix.length; i+=100) {
//            outMatrix1[i] = (int) (255 * ((outMatrix[i] - gpu_min) / tempGPU));
//            outMatrix1[i] = Color.argb(255, outMatrix1[i], outMatrix1[i], outMatrix1[i]);
//        }

        //GPU Mapping End

        //GPU Image Display
//        Bitmap bm1 = Bitmap.createBitmap(B_col/10, A_row/10, Bitmap.Config.ARGB_8888);
//        bm1.setPixels(outMatrix1, 0, B_col/10, 0, 0, B_col/10, A_row/10);
//        imgpu.setImageBitmap(bm1);

        //CPU Mapping Start
//        float cpu_min = Float.MAX_VALUE;
//        float cpu_max = Float.MIN_VALUE;
//
//        for (int i = 0; i < A_row * B_col; i+=100) {
//            cpu_min = Math.min(cpu_min, CPUMatC[i]);
//            cpu_max = Math.max(cpu_max, CPUMatC[i]);
//        }
//        float temp = (cpu_max - cpu_min);
//
//        if (temp!=0)
//            System.out.println(temp);
//        else
//            Log.e("CPU System Error","Max - Min = 0");
//
//        int[] resMatrix1 = new int[A_row * B_col / 100];
//        for (int i = 0; i < A_row * B_col; i+=100) {
//            int val = (int) (255 * ((float) (CPUMatC[i] - cpu_min) / temp));
//            resMatrix1[i] = Color.argb(255, val, val, val);
//        }
        //CPU Mapping End

        //CPU Display Image
//        Bitmap bm = Bitmap.createBitmap(B_col/10, A_row/10, Bitmap.Config.ARGB_8888);
//        bm.setPixels(resMatrix1, 0, B_col/10, 0, 0, B_col/10, A_row/10);
//        imcpu.setImageBitmap(bm);

        //Difference calculation between CPU & GPU
        float[] d = new float[A_row * B_col];

        double outSum = 0;
        double dSum = 0;

        for (int i = 0; i < d.length; i++) {
//            d[i] = Math.abs(CPUMatC[i] - outMatrix[i]);
            dSum += Math.abs(CPUMatC[i] - outMatrix[i]);
            outSum += (CPUMatC[i]);
//            d[i] = Color.argb(255, d[i], d[i], d[i]);
        }
//
//        //Difference image shown
//        Bitmap bm5 = Bitmap.createBitmap(B_col, A_row, Bitmap.Config.ARGB_8888);
//        bm5.setPixels(d, 0, B_col, 0, 0, B_col, A_row);
//        imd.setImageBitmap(bm5);

        //Relative Error Calculated
        stat = "Relative Error: " + String.format("%.12f", ((float) (dSum) / outSum));
        statsd.setText(stat);
    }

}

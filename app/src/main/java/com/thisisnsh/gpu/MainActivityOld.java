package com.thisisnsh.gpu;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.renderscript.Allocation;
import androidx.renderscript.Element;
import androidx.renderscript.RenderScript;

import java.util.Random;

public class MainActivityOld extends AppCompatActivity {


    private ImageView res;
    private TextView data,data1;

    private RenderScript mRS;

    private Allocation allocationA; // allocation of A matrix
    private Allocation allocationB; // allocation of B matrix
    private Allocation allocationC; // allocation of C matrix
    private Allocation allocationKSize; // allocation of Row Size of Matrix A;
    private Allocation allocationNSize; // allocation of Col Size of Matrix B;
    private Allocation allocationPosRow;

    private ScriptC_multiply mScript;

    private int[] matA;
    private int[] matB;
    private int[] outMatrix;

    private int[] pos_row;
    private int[] nSize;
    private int[] kSize;

    private Random rand = new Random();

    private int[][] cpuMatA;
    private int[][] cpuMatB;
    private int[][] cpuMatC;

    protected int A_row = 571;
    protected int A_col = 1000;
    protected int B_col = 1;

    public static Bitmap drawableToGrayscaleBitmap(Drawable drawable) {
        drawable.getBounds();/*www . j  a va2  s .c  om*/
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        drawable.setColorFilter(f);
        drawable.draw(canvas);
        return bitmap;
    }

    public int[] one_matrix(int m, int n) {

        int[] coverImageIntArray1D = new int[571 * 1000];
        drawableToGrayscaleBitmap(getResources().getDrawable(R.drawable.one)).getPixels(coverImageIntArray1D, 0, 1000,
                0, 0, 1000, 571);

        return coverImageIntArray1D;
    }

    public int[] two_matrix(int m, int n) {

        int[] coverImageIntArray1D = new int[1000 * 1];
        drawableToGrayscaleBitmap(getResources().getDrawable(R.drawable.two)).getPixels(coverImageIntArray1D, 0, 1,
                0, 0, 1, 1000);

        return coverImageIntArray1D;
    }

    public int[] three_matrix(int m, int n) {

        int[] coverImageIntArray1D = new int[1000 * 20];
        drawableToGrayscaleBitmap(getResources().getDrawable(R.drawable.two1)).getPixels(coverImageIntArray1D, 0, 20,
                0, 0, 20, 1000);

        return coverImageIntArray1D;
    }

    public int[] init_matrix(int m, int n) {
        int size = m * n;
        int[] r_matrix = new int[size];
        return r_matrix;
    }

    public int[] generate_row_idx(int m) {
        int[] r_idx = new int[m];
        for (int i = 0; i < m; i++) {
            r_idx[i] = i;
        }
        return r_idx;
    }

    public int[][] copyMatrix(int[] mat, int m, int n) {
        int[][] r_mat = new int[m][n];
        for (int i = 0; i < m * n; i++) {
            int row_num = i / n;
            int col_num = i - n * row_num;
            r_mat[row_num][col_num] = mat[i];
        }
        return r_mat;
    }

    public int[][] CPU_matrix_multiplication() {
        int[][] matC = new int[A_row][B_col];
        cpuMatA = copyMatrix(matA, A_row, A_col);
        cpuMatB = copyMatrix(matB, A_col, B_col);
        for (int i = 0; i < A_row; i++) {
            for (int j = 0; j < B_col; j++) {
                for (int k = 0; k < A_col; k++) {
                    matC[i][j] += cpuMatA[i][k] * cpuMatB[k][j];
                }
//                matC[i][j]/=A_col;
            }
        }
        return matC;
    }

    void initMatrix() {
        A_row = 571;
        A_col = 1000;
        B_col = 1;

        matA = one_matrix(A_row, A_col);
        matB = two_matrix(A_col, B_col);
        outMatrix = init_matrix(A_row, B_col);
        pos_row = generate_row_idx(A_row);

        kSize = new int[1];
        nSize = new int[1];

        kSize[0] = A_col;
        nSize[0] = B_col;
    }

    void initMatrix1() {
        A_row = 571;
        A_col = 1000;
        B_col = 20;

        matA = one_matrix(A_row, A_col);
        matB = three_matrix(A_col, B_col);
        outMatrix = init_matrix(A_row, B_col);
        pos_row = generate_row_idx(A_row);

        kSize = new int[1];
        nSize = new int[1];

        kSize[0] = A_col;
        nSize[0] = B_col;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        res = findViewById(R.id.res);
        data = findViewById(R.id.data);
        data1 = findViewById(R.id.data1);

        initMatrix();
        createScript();

        initMatrix1();
        createScript1();

    }

    private void createScript() {
        long startTime = System.currentTimeMillis();
        mRS = RenderScript.create(this);

        int sizeA = A_row * A_col;
        int sizeB = A_col * B_col;
        int sizeC = A_row * B_col;

        // memory allocation part
        allocationA = Allocation.createSized(mRS, Element.I32(mRS), sizeA);
        allocationB = Allocation.createSized(mRS, Element.I32(mRS), sizeB);
        allocationC = Allocation.createSized(mRS, Element.I32(mRS), sizeC);
        allocationNSize = Allocation.createSized(mRS, Element.I32(mRS), 1);
        allocationKSize = Allocation.createSized(mRS, Element.I32(mRS), 1);
        allocationPosRow = Allocation.createSized(mRS, Element.I32(mRS), A_row);

        allocationA.copyFrom(matA);
        allocationB.copyFrom(matB);

        allocationPosRow.copyFrom(pos_row);
        allocationNSize.copyFrom(nSize);
        allocationKSize.copyFrom(kSize);

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

        startTime = System.currentTimeMillis();
        int[][] CPUMatC = CPU_matrix_multiplication();
        endTime = System.currentTimeMillis();

        float swt = ((endTime - startTime) / 1f);

        String stats = "Software time: "+swt+"ms\nGPU time: "+gput+"ms\nThe speed up: "+swt/gput;
        data.setText(stats);

    }

    private void createScript1() {
        long startTime = System.currentTimeMillis();
        mRS = RenderScript.create(this);

        int sizeA = A_row * A_col;
        int sizeB = A_col * B_col;
        int sizeC = A_row * B_col;

        // memory allocation part
        allocationA = Allocation.createSized(mRS, Element.I32(mRS), sizeA);
        allocationB = Allocation.createSized(mRS, Element.I32(mRS), sizeB);
        allocationC = Allocation.createSized(mRS, Element.I32(mRS), sizeC);
        allocationNSize = Allocation.createSized(mRS, Element.I32(mRS), 1);
        allocationKSize = Allocation.createSized(mRS, Element.I32(mRS), 1);
        allocationPosRow = Allocation.createSized(mRS, Element.I32(mRS), A_row);

        allocationA.copyFrom(matA);
        allocationB.copyFrom(matB);

        allocationPosRow.copyFrom(pos_row);
        allocationNSize.copyFrom(nSize);
        allocationKSize.copyFrom(kSize);

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

        startTime = System.currentTimeMillis();
        int[][] CPUMatC = CPU_matrix_multiplication();
        endTime = System.currentTimeMillis();

        float swt = ((endTime - startTime) / 1f);

        String stats = "Software time: "+swt+"ms\nGPU time: "+gput+"ms\nThe speed up: "+swt/gput;
        data1.setText(stats);

        int[] resMatrix = new int[571*20];
        for (int i = 0; i < 571*20; i++) {
            int row_num = i / 20;
            int col_num = i - 20 * row_num;
            resMatrix[i] = CPUMatC[row_num][col_num];
        }

        Bitmap bm = Bitmap.createBitmap(20, 571, Bitmap.Config.ARGB_8888);
        bm.setPixels(resMatrix, 0, 20, 0, 0, 20, 571);

        res.setImageBitmap(bm);

    }

}

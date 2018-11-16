package utils;


// Matriz2D.java
// Sajan Joseph, sajanjoseph@gmail.com
// Modified by Andrew Davison, April 2011, ad@fivedots.coe.psu.ac.th
// Adaptado por Thiago C L da Silva, cls.thiago@gmail.com

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;


public class Matriz2D extends DenseDoubleMatrix2D {


    public Matriz2D(double[][] data) {
        super(data);
    }

    public Matriz2D(DoubleMatrix2D dmat) {
        super(dmat.toArray());
    }

    public Matriz2D(int rows, int cols) {
        super(rows, cols);
    }


    public Matriz2D(double[] data, int rows) {
        super(rows, ((rows != 0) ? data.length / rows : 0));
        int columns = (rows != 0) ? data.length / rows : 0;
        if ((rows * columns) != data.length)
            throw new IllegalArgumentException("Array length must be a multiple of " + rows);

        double[][] vals = new double[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++)
                vals[i][j] = data[i + (j * rows)];
        }
        assign(vals);
    }


    public Matriz2D getSubMatrix(int rows) {
        return new Matriz2D(viewPart(0, 0, rows, super.columns()).copy());
    }



    public void subtrairMedia(){
        this.subtracaoPorColuna( this.calcMedia_cols() );
    }


    public double[] calcMedia_cols(){
        double[][] data = this.toArray();
        double total;
        double[] avgValues = new double[this.columns];

        for (int col = 0; col < this.columns; col++) {
            total = 0.0;
            for (int row = 0; row < this.rows; row++)
                total += data[row][col];
            avgValues[col] = total / this.rows;
        }
        return avgValues;
    }


    public void replaceRows_array(double[] data) {
        if (this.columns != data.length)
            throw new RuntimeException(
                    "matrix columns not matching number of input array elements");

        for (int lin = 0; lin < this.rows; lin++) {
            for (int col = 0; col < this.columns; col++)
                set(lin, col, data[col]);
        }
    }


    public void normalise() {
        double[][] temp = this.toArray();
        double[] mvals = new double[temp.length];

        for (int i = 0; i < temp.length; i++)
            mvals[i] = max(temp[i]);

        for (int i = 0; i < temp.length; i++) {
            for (int j = 0; j < temp[0].length; j++)
                temp[i][j] /= mvals[i];
        }
        assign(temp);
    }


    private static double max(double[] arr) {
        double max = Double.MIN_VALUE;
        for (int i = 0; i < arr.length; i++)
            max = Math.max(max, arr[i]);
        return max;
    }


    public void subtract(Matriz2D mat) {
        assign(mat, Functions.functions.minus);
    }


    public void add(Matriz2D mat) {
        assign(mat, Functions.functions.plus);
    }


    public void subtracaoPorColuna(double[] oneDArray) {
        double[][] denseArr = this.toArray();
        for (int i = 0; i < denseArr.length; i++) {
            for (int j = 0; j < denseArr[0].length; j++)
                denseArr[i][j] -= oneDArray[j];
        }
        assign(denseArr);
    }


    public Matriz2D multiply(Matriz2D mat) {
        return new Matriz2D(this.zMult(mat, null));
    }


    public void multiplyElementWise(Matriz2D mat) {
        assign(mat, Functions.functions.mult);
    }


    public Matriz2D transpose() {
        return new Matriz2D(this.viewDice());
    }


    public double[] flatten() {
        double[] res = new double[this.rows * this.columns];
        int i = 0;
        for (int row = 0; row < this.rows; row++) {
            for (int col = 0; col < this.columns; col++)
                res[i++] = get(row, col);
        }
        return res;
    }


    public static double norm(double[] arr) {
        double val = 0.0;
        for (int i = 0; i < arr.length; i++)
            val += (arr[i] * arr[i]);
        return val;
    }



    public AutoVetor_decomp getEigenvalueDecomp() {
        return new AutoVetor_decomp(this);
    }

}

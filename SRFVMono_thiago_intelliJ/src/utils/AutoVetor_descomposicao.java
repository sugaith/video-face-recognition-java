package utils;


// AutoVetor_descomposicao.java
// Sajan Joseph, sajanjoseph@gmail.com
// Modified by Andrew Davison, April 2011, ad@fivedots.coe.psu.ac.th
// Adaptado por Thiago C L da Silva, cls.thiago@gmail.com


import cern.colt.matrix.linalg.EigenvalueDecomposition;


public class AutoVetor_descomposicao extends EigenvalueDecomposition {
    public AutoVetor_descomposicao(Matriz2D dmat) {
        super(dmat);
    }


    public double[] getEigenValues() {
        return diag(getD().toArray());
    }


    public double[][] getEigenVectors() {
        return getV().toArray();
    }


    private double[] diag(double[][] m) {
        double[] diag = new double[m.length];
        for (int i = 0; i < m.length; i++)
            diag[i] = m[i][i];
        return diag;
    }

}

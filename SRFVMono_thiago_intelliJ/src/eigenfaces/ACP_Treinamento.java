package eigenfaces;


// ACP_Treinamento.java
// Sajan Joseph, sajanjoseph@gmail.com
// Modified by Andrew Davison, April 2011, ad@fivedots.coe.psu.ac.th
// Adaptado por Thiago C L da Silva, cls.thiago@gmail.com


/* Use the training face images in trainingImages\ to create eigenvector and eigenvalue,
   information using PCA. The information is stored in eigen.cache.
   Two subdirectories, eigenfaces\ and reconstructed\, are also generated which
   contain eigenfaces and regenerated images. These are only produced to allow the
   eigenface process to be checked. 

   Only eigen.ccache is used by the recognition process, which is separated out
   into FaceRecognizer.java

   This code is a refactoring of the JavaFaces package by Sajan Joseph, available
   at http://code.google.com/p/javafaces/ The current version includes a GUI.
*/

import java.awt.image.*;
import java.util.*;

import utils.AutoVetor_decomp;
import utils.FileUtils;
import utils.Matriz2D;
import utils.Utilitarios;


public class ACP_Treinamento {

    public static void construirEspaco(int num_eigenfaces)
    // create a EigenSpace for the specified number of eigenfaces, and store it
    {
        ArrayList<String> fnms = FileUtils.getNomePath_treinamento();
        int num_imgs = fnms.size();
        if ((num_eigenfaces < 1) || (num_eigenfaces >= num_imgs)) {
            System.out.println("::ATENCAO:: Número de eigenfaces deve ser entre 1 -" + (num_imgs - 1) );
            System.out.println("Utilizando: " + (num_imgs - 1));
            num_eigenfaces = num_imgs - 1;
        } else
            System.out.println("Número de Eigenfaces: " + num_eigenfaces);

        EigenSpace espaco_eigenfaces = gerar_EigenSpace(fnms);
        FileUtils.salvarEigenSpace(espaco_eigenfaces);

        // RECONSTRUÇAO
        //reconstructIms(numEFs, espaco_multiDimensional);
    }


    /*
    CALCULO DE ACP - FASE DE TREINAMENTO
        Gera o espaço multidimensinonal onde cada auto-vetor representa uma dimensão
        - cria auto-vetores e auto-valores das imagens de treinamento
        - Gera EigenFaces espaço multidimensional

         Sajan Joseph, sajanjoseph@gmail.com
         Modified by Andrew Davison, April 2011, ad@fivedots.coe.psu.ac.th
         Modified by Thiago C L da Silva, 2018, cls.thiago@gmail.com
     */
    private static EigenSpace gerar_EigenSpace(ArrayList<String> fnms){
        //PASSO 1 :: DADOS!
        //carrega imagens da pasta de treinamento
        BufferedImage[] ims = FileUtils.carregaImagensDeTreino(fnms);
        //converte bufferedImage para matriz2D
        Matriz2D matriz_imgs = buffImage2Matriz2D(ims);

        //PASSO 2:: CALCULAR A MÉDIA DE CADA IMAGEM E APLICAR SUBTRAÇÃO COM AS MESMAS
        double[] avgImage = matriz_imgs.calcMedia_cols();
        matriz_imgs.subtrairMedia();//imagens de treino com a face média subtraída

        //PASSO 3:: CALCULAR A MATRIZ DE COVARIÂNCIA
        Matriz2D imsDataTr = matriz_imgs.transpose();
        Matriz2D covarMat = matriz_imgs.multiply(imsDataTr);

        //PASSO 4:: CALCULAR OS AUTOVETORES E AUTOVALORES DA MATRIZ DE COVARIANCIA
        AutoVetor_decomp egValDecomp = covarMat.getEigenvalueDecomp();
        double[] egVals = egValDecomp.getEigenValues();
        double[][] egVecs = egValDecomp.getEigenVectors();

        //PASSO 4.1:: ordenar o vetor de autovetores por ordem de autovalores (para futuro possivel descarte)
        ordenaEgVecs(egVals, egVecs);

        //PASSO 5:: No último passo cada imagem de treinamento é projetada no espaço face.
        // "O descritor PCA (ou engeifaces, normalizados) é calculado por uma combinação linear
        // de Auto-vetores com os vetores originais."
        Matriz2D egFaces = calcEspaco(matriz_imgs, new Matriz2D(egVecs));

        System.out.println("::: Salvando EigenFaces como imagens...");
        FileUtils.salvarEigenfaces_imgs(egFaces, ims[0].getWidth());
        System.out.println("::: EIGENFACES GERADOS :::");

//        Para cada face, apenas os coeficientes  são armazenados para futura comparação.
        return new EigenSpace(fnms, matriz_imgs.toArray(), avgImage, egFaces.toArray(),
                              egVals, ims[0].getWidth(), ims[0].getHeight()
        );
    }



    /*
    Por Thiago C L Da silva, cls.thiago@gmail.com
     */
    public static EigenSpace atualiza_EigenSpace(EigenSpace oldEigenSpace, BufferedImage[] novasFaces, ArrayList<String> nomeFaces) {
        //////logica:
        ////pega velhos nomes e concatena com os novos nomes
        ArrayList<String> listaNovosNomes = oldEigenSpace.getListaPath_imagens();
        listaNovosNomes.addAll(nomeFaces);

        ////pega novas faces, transforma em double[][]
        double[][] novasFacesDouble = buffImage2Matriz2D(novasFaces).toArray();

        ////concatena o double[][] das velhas faces com o double[][] das novas
        double[][] facesConcatenadas = Utilitarios.concatArray2d_double(oldEigenSpace.getImages(), novasFacesDouble);

        ////cria Matriz2D com o concatenado dos dois doubles arrays..
        Matriz2D imsMat = new Matriz2D(facesConcatenadas);

        ////refaz processo de criacao
        double[] avgImage = imsMat.calcMedia_cols();
        imsMat.subtrairMedia();   // subtrair média de cada face
        // cada linha contem agora apenas caracteriasticas distinguidas das imagens de treinamento

        // calcula matriz de covariancia
        Matriz2D imsDataTr = imsMat.transpose();
        Matriz2D covarMat = imsMat.multiply(imsDataTr);

        // calcula coordenadas (pesos) para autovetores e autovalores para matriz de covariancia
        AutoVetor_decomp egValDecomp = covarMat.getEigenvalueDecomp();
        //imsDataTr = covarMat = null;

        double[] egVals = egValDecomp.getEigenValues();
        double[][] egVecs = egValDecomp.getEigenVectors();

        // ordena os autovetores e valores
        ordenaEgVecs(egVals, egVecs);

        //normaliza matriz
        Matriz2D egFaces = calcEspaco(imsMat, new Matriz2D(egVecs));

        ////retorna novo face bundle
        return new EigenSpace(listaNovosNomes, imsMat.toArray(), avgImage,
                egFaces.toArray(), egVals, novasFaces[0].getWidth(), novasFaces[0].getHeight());
    }

    /*
        Converte um vetor de bufferedImage para Matriz2D
        - cada linha corresponde a uma imagem e o número de colunas representa o numero de pixels da imagem
        - O vetor é normalizado
     */
    private static Matriz2D buffImage2Matriz2D(BufferedImage[] ims){
        int imWidth = ims[0].getWidth();
        int imHeight = ims[0].getHeight();

        int numRows = ims.length;
        int numCols = imWidth * imHeight;
        double[][] data = new double[numRows][numCols];
        for (int i = 0; i < numRows; i++)
            ims[i].getData().getPixels(0, 0, imWidth, imHeight, data[i]);    // one image per row

        Matriz2D imsMat = new Matriz2D(data);
        imsMat.normalise();
        return imsMat;
    }



    private static Matriz2D calcEspaco(Matriz2D imsMat, Matriz2D egVecs){
        Matriz2D egVecsTr = egVecs.transpose();
        Matriz2D egFaces = egVecsTr.multiply(imsMat);
        double[][] egFacesData = egFaces.toArray();

        //normalizacao
        for (int row = 0; row < egFacesData.length; row++) {
            double norm = Matriz2D.norm(egFacesData[row]);   // valor normal
            for (int col = 0; col < egFacesData[row].length; col++)
                egFacesData[row][col] = egFacesData[row][col] / norm;//normaliza
        }
        return new Matriz2D(egFacesData);
    }


    // ---------------------- sort the EigenVectors --------------------------


    private static void ordenaEgVecs(double[] egVals, double[][] egVecs)
  /* sort the Eigenvalues and Eigenvectors arrays into descending order
     by eigenvalue. Add them to a table so the sorting of the values adjusts the
     corresponding vectors
  */ {
        Double[] egDvals = getEgValsAsDoubles(egVals);

        // create table whose key == eigenvalue; value == eigenvector
        Hashtable<Double, double[]> table = new Hashtable<Double, double[]>();
        for (int i = 0; i < egDvals.length; i++)
            table.put(egDvals[i], getColumn(egVecs, i));

        ArrayList<Double> sortedKeyList = sortKeysDescending(table);
        updateEgVecs(egVecs, table, egDvals, sortedKeyList);
        // use the sorted key list to update the Eigenvectors array

        // convert the sorted key list into an array
        Double[] sortedKeys = new Double[sortedKeyList.size()];
        sortedKeyList.toArray(sortedKeys);

        // use the sorted keys array to update the Eigenvalues array
        for (int i = 0; i < sortedKeys.length; i++)
            egVals[i] = sortedKeys[i].doubleValue();

    }  // end of ordenaEgVecs()


    private static Double[] getEgValsAsDoubles(double[] egVals)
    // convert double Eigenvalues to Double objects, suitable for Hashtable keys
    {
        Double[] egDvals = new Double[egVals.length];
        for (int i = 0; i < egVals.length; i++)
            egDvals[i] = new Double(egVals[i]);
        return egDvals;
    }  // end of getEgValsAsDoubles()


    private static double[] getColumn(double[][] vecs, int col)
  /* the Eigenvectors array is in column order (one vector per column);
     return the vector in column col */ {
        double[] res = new double[vecs.length];
        for (int i = 0; i < vecs.length; i++)
            res[i] = vecs[i][col];
        return res;
    }  // end of getColumn()


    private static ArrayList<Double> sortKeysDescending(
            Hashtable<Double, double[]> table)
    // sort the keylist part of the hashtable into descending order
    {
        ArrayList<Double> keyList = Collections.list(table.keys());
        Collections.sort(keyList, Collections.reverseOrder()); // largest first
        return keyList;
    }  // end of sortKeysDescending()


    private static void updateEgVecs(double[][] egVecs,
                                     Hashtable<Double, double[]> table,
                                     Double[] egDvals, ArrayList<Double> sortedKeyList)
  /* get vectors from the table in descending order of sorted key,
     and update the original vectors array */ {
        for (int col = 0; col < egDvals.length; col++) {
            double[] egVec = table.get(sortedKeyList.get(col));
            for (int row = 0; row < egVec.length; row++)
                egVecs[row][col] = egVec[row];
        }
    }  // end of updateEgVecs()


    // ---------- reconstruction of images from eigenfaces ------------------


    private static void reconstructIms(int numEFs, EigenSpace bundle) {
        System.out.println("\nReconstructing training images...");

        Matriz2D egFacesMat = new Matriz2D(bundle.getAuto_vetores());
        Matriz2D egFacesSubMat = egFacesMat.getSubMatrix(numEFs);

        Matriz2D egValsMat = new Matriz2D(bundle.getAuto_valores(), 1);
        Matriz2D egValsSubMat = egValsMat.transpose().getSubMatrix(numEFs);

        double[][] weights = bundle.calculaCoordenadas(numEFs);
        double[][] normImgs = getNormImages(weights, egFacesSubMat, egValsSubMat);
        // the mean-subtracted (normalized) training images
        double[][] origImages = addAvgImage(normImgs, bundle.getImagem_media());
        // original training images = normalized images + average image

        FileUtils.saveReconIms2(origImages, bundle.getImageWidth());
        System.out.println("Reconstruction done\n");
    }  // end of reconstructIms()


    private static double[][] getNormImages(double[][] weights,
                                            Matriz2D egFacesSubMat, Matriz2D egValsSubMat)
  /* calculate weights x eigenfaces, which generates mean-normalized traimning images;
     there is one image per row in the returned array
  */ {
        double[] egDValsSub = egValsSubMat.flatten();
        Matriz2D tempEvalsMat = new Matriz2D(weights.length, egDValsSub.length);
        tempEvalsMat.replaceRows_array(egDValsSub);

        Matriz2D tempMat = new Matriz2D(weights);
        tempMat.multiplyElementWise(tempEvalsMat);

        Matriz2D normImgsMat = tempMat.multiply(egFacesSubMat);
        return normImgsMat.toArray();
    }  // end of getNormImages()


    private static double[][] addAvgImage(double[][] normImgs, double[] avgImage)
    // add the average image to each normalized image (each row) and store in a new array;
    // the result are the original training images; one per row
    {
        double[][] origImages = new double[normImgs.length][normImgs[0].length];
        for (int i = 0; i < normImgs.length; i++) {
            for (int j = 0; j < normImgs[i].length; j++)
                origImages[i][j] = normImgs[i][j] + avgImage[j];
        }
        return origImages;
    }


    /*
    Para criaçao do espaço em arquivo .cache

     */
    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();
        ACP_Treinamento.construirEspaco(10);
        System.out.println("Tempo exec: " +
                (System.currentTimeMillis() - startTime) + " ms");
    }


}



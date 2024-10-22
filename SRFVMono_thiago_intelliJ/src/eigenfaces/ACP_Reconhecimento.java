package eigenfaces;


// ACP_Reconhecimento.java
// Sajan Joseph, sajanjoseph@gmail.com
// http://code.google.com/p/javafaces/
// Modified by Andrew Davison, April 2011, ad@fivedots.coe.psu.ac.th
// Adaptado por Thiago C L da Silva, cls.thiago@gmail.com

/* Use the eigen.cache containing eigenfaces, eigenvalues, and training
   image info to find the training image which most cloesly resembles 
   an input image.

   This code is a refactoring of the JavaFaces package by Sajan Joseph, available
   at http://code.google.com/p/javafaces/ The current version includes a GUI.
*/

import utils.FileUtils;
import utils.ImageUtils;
import utils.Matriz2D;

import java.awt.image.*;
import java.util.*;


public class ACP_Reconhecimento {
    private static final float FACES_FRAC = 0.75f;
    // default fraction of eigenfaces used in a match

    private EigenSpace eigenSpace = null;
    private double[][] coordenadas_eigenfaces = null;    // coordenadas das imagens de treinamento no espaço
    private int num_eigenfaces = 0;     // num de eigenfaces usado para o reconhecimento (maximo sendo o valor q o espaço foi iniciado)


    public ACP_Reconhecimento(int num_eigenFaces) {
        eigenSpace = FileUtils.carregaEigenSpace2RAM();
        if (eigenSpace == null) {
            System.out.println("Espaço (bundle) não encontrado.. deve-se construir o espaço e salvá-lo em arquivo.");
            System.exit(1);
        }

        //num. de eigenfaces dave ser menor ou igual au numero de eigenfaces criada inicialmente no espaço
        int numFaces = eigenSpace.getNumEigenFaces();
        System.out.println("::: Max num. Eigenfaces para Reconhecimento -> " + numFaces);

        this.num_eigenfaces = num_eigenFaces;
        if ((num_eigenfaces < 1) || (num_eigenfaces > numFaces - 1)) {
            num_eigenfaces = Math.round((numFaces - 1) * FACES_FRAC);     // set to less than max
            System.out.println("Número de eigenfaces para o reconhecimento deve ter um valor entre 1-" + (numFaces - 1) );
            System.out.println("Utilizando: " + num_eigenfaces);
        } else
            System.out.println("Utilizando: " + num_eigenfaces);

        coordenadas_eigenfaces = eigenSpace.calculaCoordenadas(num_eigenfaces);
    }


    /*
    Por Thiago C L Da silva, cls.thiago@gmail.com
     */
    public void setNovoEspaco(EigenSpace f, int numEigenFaces) {
        System.out.println("SETANDO NOVO ESPAÇO: " + num_eigenfaces);

        if (f == null)
            return;

        this.setEigenSpace(f);
        int numFaces = getEigenSpace().getNumEigenFaces();

        num_eigenfaces = numEigenFaces;
        if ((num_eigenfaces < 1) || (num_eigenfaces > numFaces - 1)) {
            num_eigenfaces = Math.round((numFaces - 1) * FACES_FRAC);     // set to less than max
            System.out.println("numero de eigenfaces para o match deve ter um valor entre (1-" +
                    (numFaces - 1) + ")" + "; utilizando: " + num_eigenfaces);
        } else
            System.out.println("numero de  eigenfaces: " + num_eigenfaces);

        coordenadas_eigenfaces = getEigenSpace().calculaCoordenadas(num_eigenfaces);
    }

    public ResultadoReconhecimento match(String imFnm)
    // match image in file against training images
    {
        if (!imFnm.endsWith(".png")) {
            System.out.println("imagem deve ser um PNG");
            return null;
        } else
            System.out.println("Matching... " + imFnm);

        BufferedImage image = FileUtils.carregaImagem(imFnm);
        if (image == null)
            return null;

        return match(image);
    }  // end of match() using filename

    public ResultadoReconhecimento match(BufferedImage im)
    // match loaded image against training images

    {
        if (getEigenSpace() == null) {
            System.out.println("Deve-se iniciar um eigenspace antes de processar o reconhecimento");
            return null;
        }

        return processaReconhecimento(im);   // no checking of image size or grayscale
    }      // end of match() using BufferedImage


    // ----------------- find matching results -----------------


    private ResultadoReconhecimento processaReconhecimento(BufferedImage im) {
        //PASSO 1:: CONVERTE IMAGEM PARA VETOR E NORMALIZA
        //converte imagem para vetor
        double[] imArr = ImageUtils.createArrFromIm(im);
        Matriz2D imMat = new Matriz2D(imArr, 1);
        // normaliza o vator
        imMat.normalise();

        //PASSO 2:: PROJETAR O VETOR DE CONSULTA NO ESPAÇO
        /// multiplicando de autovetores com o vetor DE CONSULTA com a face média já subtraida

        // subtracao da face media
        imMat.subtract( new Matriz2D( this.getEigenSpace().getImagem_media(), 1) );
        // projetar o vetor de consulta no espaço face, retornando suas coordenadas do espa'co
        // limitar o uso das eigenfaces "autovetores" por NUM_EF_recog previamente fornecida
        Matriz2D espaco = calcEspaco(num_eigenfaces, imMat);

        //PASSO 3:: calcula a distancia euclidiana entre a nova imagem e as imagens pre treinadas (eigenfaces)
        double[] dists = this.getEucDists(espaco);
        InfoDistancia distInfo = getMenorDist(dists);

        //consulta o nome da imagem
        ArrayList<String> imageFNms = this.getEigenSpace().getListaPath_imagens();
        String matchingFNm = imageFNms.get(distInfo.getIndex());

        //extrai raiz quadrada
        double minDist = Math.sqrt(distInfo.getValue());

        //salva no objeto
        return new ResultadoReconhecimento(matchingFNm, minDist);
    }

    /* mapeia a imagem para espaco limitando o numero de eigenfaces (autovetores)
     *   por numEFs retornando suas coordenadas do espaço
     */
    private Matriz2D calcEspaco(int numEFs, Matriz2D imMat)
   {
        Matriz2D egFacesMat = new Matriz2D(getEigenSpace().getAuto_vetores());
        Matriz2D egFacesMatPart = egFacesMat.getSubMatrix(numEFs);
        Matriz2D egFacesMatPartTr = egFacesMatPart.transpose();

        return imMat.multiply(egFacesMatPartTr);
    }


    private double[] getEucDists(Matriz2D imWeights)
  /* return an array of the sum of the squared Euclidian distance
     between the input image imWeights and all the training image coordenadas_eigenfaces */ {
        Matriz2D tempWt = new Matriz2D(coordenadas_eigenfaces);
        double[] wts = imWeights.flatten();

        tempWt.subtracaoPorColuna(wts);
        tempWt.multiplyElementWise(tempWt);//ao quadrado
        double[][] sqrWDiffs = tempWt.toArray();
        double[] dists = new double[sqrWDiffs.length];

        for (int row = 0; row < sqrWDiffs.length; row++) {
            double sum = 0.0;
            for (int col = 0; col < sqrWDiffs[0].length; col++)
                sum += sqrWDiffs[row][col];
            dists[row] = sum;
        }
        return dists;
    }  // end of getDists()


    private InfoDistancia getMenorDist(double[] dists) {
        double minDist = Double.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < dists.length; i++)
            if (dists[i] < minDist) {
                minDist = dists[i];
                index = i;
            }
        return new InfoDistancia(dists[index], index);
    }      // end of getMenorDist()


    // PARA TESTE
    public static void main(String[] args) {
        if ((args.length < 1) || (args.length > 2)) {
            System.out.println("Usage: java ACP_Reconhecimento imagePngFnm [numberOfEigenfaces]");
            return;
        }

        int numEFs = 0;
        if (args.length == 2) {
            try {
                numEFs = Integer.parseInt(args[1]);
            } catch (Exception e) {
                System.out.println("Number argument, " + args[1] + " must be an integer");
            }
        }

        long startTime = System.currentTimeMillis();

        ACP_Reconhecimento fr = new ACP_Reconhecimento(numEFs);
        ResultadoReconhecimento result = fr.match(args[0]);

        if (result == null)
            System.out.println("No match found");
        else {
            System.out.println();
            System.out.print("Matches image in " + result.getMatchFileName());
            System.out.printf("; distance = %.4f\n", result.getMatchDistance());
            System.out.println("Matched name: " + result.getName());
        }
        System.out.println("Total time taken: " + (System.currentTimeMillis() - startTime) + " ms");
    }






    /**
     * @return the eigenSpace
     */
    public EigenSpace getEigenSpace() {
        return eigenSpace;
    }

    /**
     * @param eigenSpace the eigenSpace to set
     */
    public void setEigenSpace(EigenSpace eigenSpace) {
        this.eigenSpace = eigenSpace;
    }


}  // end of ACP_Reconhecimento class



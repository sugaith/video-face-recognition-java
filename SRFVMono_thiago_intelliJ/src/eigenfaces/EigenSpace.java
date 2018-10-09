package eigenfaces;


import utils.Matriz2D;

import java.io.Serializable;
import java.util.*;

/*
    Classe que representa um espaço multidimensional para EigenFaces (autovetores , autovalores)
 */

public class EigenSpace implements Serializable {
    // lista_imagens = cada linha é uma imagem, cada coluna = pixels da imagem
    private double[][] lista_imagens;
    //lista dos paths das imagens
    private ArrayList<String> listaPath_imagens;
    // imagem "media" das imagens de treinamento
    private double[] imagem_media;
    // auto-vetores e auto-valores, HxW padrao das imagens
    private double[][] auto_vetores;
    private double[] auto_valores;
    private int imageWidth, imageHeight;


    public EigenSpace(ArrayList<String> nms, double[][] ims, double[] avgImg,
                      double[][] facesMat, double[] evals, int w, int h) {
        listaPath_imagens = nms;
        lista_imagens = ims;
        imagem_media = avgImg;
        auto_vetores = facesMat;
        auto_valores = evals;
        imageWidth = w;
        imageHeight = h;
    }


    public double[][] getImages() {
        return lista_imagens;
    }

    public double[][] getAuto_vetores() {
        return auto_vetores;
    }

    public int getNumEigenFaces() {
        return auto_vetores.length;
    }

    public double[] getImagem_media() {
        return imagem_media;
    }

    public double[] getAuto_valores() {
        return auto_valores;
    }

    public ArrayList<String> getListaPath_imagens() {
        return listaPath_imagens;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }


    /*
    Calcula os coordenadas do espaço multidimensional(ou "pesos" ) de um SUBSET the Eigenfaces
    - As coordenadas podem ser rotacionadas para que os auto-vetores se tornem o eixo do espaço multidimensional

         Sajan Joseph, sajanjoseph@gmail.com
         Modified by Andrew Davison, April 2011, ad@fivedots.coe.psu.ac.th
         Adaptado por Thiago C L da Silva, cls.thiago@gmail.com

  */
    public double[][] calculaCoordenadas(int numEFs){
        Matriz2D imsMat = new Matriz2D(lista_imagens);

        Matriz2D facesMat = new Matriz2D(auto_vetores);
        Matriz2D facesSubMatTr = facesMat.getSubMatrix(numEFs).transpose();

        Matriz2D weights = imsMat.multiply(facesSubMatTr);
        return weights.toArray();
    }


}

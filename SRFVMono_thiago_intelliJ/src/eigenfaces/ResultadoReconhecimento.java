package eigenfaces;

import utils.FileUtils;

import java.awt.image.BufferedImage;


// ResultadoReconhecimento.java
// Sajan Joseph, sajanjoseph@gmail.com
// http://code.google.com/p/javafaces/
// Modified by Andrew Davison, April 2011, ad@fivedots.coe.psu.ac.th
// Adaptado por Thiago C L da Silva, cls.thiago@gmail.com


public class ResultadoReconhecimento {
    private String file_name;
    private double distanciaEuclid;
    private BufferedImage faceImgUsadaParaMatch;

    public ResultadoReconhecimento(String fnm, double dist) {
        file_name = fnm;
        distanciaEuclid = dist;
    }

    // "TRAINING_DIR\nome_da_face_123.png"; return "nome_da_face"
    public String getName() {
        if (getMatchFileName().contains("_"))
            return getMatchFileName().substring(0, getMatchFileName().indexOf("_")).replace(FileUtils.TRAINING_DIR + "\\", "");
        else
            return getMatchFileName();

    }

    /**
     * @return the file_name
     */
    public String getMatchFileName() {
        return file_name;
    }

    /**
     * @param file_name the file_name to set
     */
    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    /**
     * @return the distanciaEuclid
     */
    public double getMatchDistance() {
        return distanciaEuclid;
    }

    /**
     * @param distanciaEuclid the distanciaEuclid to set
     */
    public void setDistanciaEuclid(double distanciaEuclid) {
        this.distanciaEuclid = distanciaEuclid;
    }


    /**
     * @return the faceImgUsadaParaMatch
     */
    public BufferedImage getFaceImgUsadaParaMatch() {
        return faceImgUsadaParaMatch;
    }

    /**
     * @param faceImgUsadaParaMatch the faceImgUsadaParaMatch to set
     */
    public void setFaceImgUsadaParaMatch(BufferedImage faceImgUsadaParaMatch) {
        this.faceImgUsadaParaMatch = faceImgUsadaParaMatch;
    }


}  // end of ResultadoReconhecimento class

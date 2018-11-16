package utils;


// ImageUtils.java
// Sajan Joseph, sajanjoseph@gmail.com
// http://code.google.com/p/javafaces/
// Modified by Andrew Davison, April 2011, ad@fivedots.coe.psu.ac.th
// Adaptado por Thiago C L da Silva, cls.thiago@gmail.com


import org.bytedeco.javacpp.opencv_core;

import java.util.*;
import java.awt.*;
import java.awt.image.*;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgproc.cvEqualizeHist;


public class ImageUtils {
    private static final int IM_SCALE = 4;

    public static BufferedImage createImFromArr(double[] imData, int width) {
        BufferedImage im = null;
        try {
            im = new BufferedImage(width, imData.length / width, BufferedImage.TYPE_BYTE_GRAY);
            Raster rast = im.getData();
            WritableRaster wr = rast.createCompatibleWritableRaster();
            double maxVal = Double.MIN_VALUE;
            double minVal = Double.MAX_VALUE;

            for (int i = 0; i < imData.length; i++) {
                maxVal = Math.max(maxVal, imData[i]);
                minVal = Math.min(minVal, imData[i]);
            }

            for (int j = 0; j < imData.length; j++)
                imData[j] = ((imData[j] - minVal) * 255) / (maxVal - minVal);
            wr.setPixels(0, 0, width, imData.length / width, imData);
            im.setData(wr);
        } catch (Exception e) {
            System.out.println(e);
        }
        return im;
    }  // end of createImFromArr()


    public static double[] createArrFromIm(BufferedImage im) {
        int imWidth = im.getWidth();
        int imHeight = im.getHeight();

        double[] imArr = new double[imWidth * imHeight];
        im.getData().getPixels(0, 0, imWidth, imHeight, imArr);
        return imArr;
    }  // end of createArrFromIm()

    public static opencv_core.IplImage escalaImagemCinza(opencv_core.IplImage img)
  /* Scale the image and convert it to grayscale. Scaling makes
     the image smaller and so faster to process, and Haar detection
     requires a grayscale image as input
  */ {
        // convert to grayscale
        opencv_core.IplImage grayImg = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 1);
        cvCvtColor(img, grayImg, CV_BGR2GRAY);

        // scale the grayscale (to speed up face detection)
        opencv_core.IplImage smallImg = opencv_core.IplImage.create(grayImg.width() / IM_SCALE,
                grayImg.height() / IM_SCALE, IPL_DEPTH_8U, 1);
        cvResize(grayImg, smallImg, CV_INTER_LINEAR);

        cvReleaseImage(grayImg);
        // equalize the small grayscale
        cvEqualizeHist(smallImg, smallImg);
        return smallImg;
    }  // end of scaleGray()

    // public static BufferedImage convertToGray(BufferedImage im)
    public static BufferedImage escalaImagemCinza(BufferedImage im, double scale)
    // scale and convert to grayscale
    {
        int imWidth = im.getWidth();
        int imHeight = im.getHeight();

        int nWidth = (int) Math.round(imWidth * scale);
        int nHeight = (int) Math.round(imHeight * scale);

        // convert to grayscale while resizing
        BufferedImage grayIm = new BufferedImage(nWidth, nHeight,
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = grayIm.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(im, 0, 0, nWidth, nHeight, 0, 0, imWidth, imHeight, null);
        g2.dispose();

        return grayIm;
    }  // end of toScaledGray()


    public static BufferedImage clipToRectangle(BufferedImage im,
                                                int x, int y, int width, int height) {
        BufferedImage clipIm = null;
        try {

            clipIm = im.getSubimage(x, y, width, height);
        } catch (RasterFormatException e) {
            e.printStackTrace();
            System.out.println("Could not clip the image " + im.getWidth() + ": " + im.getHeight());
            clipIm = im;
        }
        return clipIm;
    }  // end of clipToRectangle()


    public static void checkImSizes(ArrayList<String> fNms, BufferedImage[] ims) {
        int imWidth = ims[0].getWidth();
        int imHeight = ims[0].getHeight();
        System.out.println("Image (w,h): (" + imWidth + ", " + imHeight + ")");

        for (int i = 1; i < ims.length; i++) {
            if ((ims[i].getHeight() != imHeight) ||
                    (ims[i].getWidth() != imWidth)) {
                System.out.println("All images should have be the same size; " +
                        fNms.get(i) + " is a different size");
                System.exit(1);
            }
        }
    }     // end of checkImSizes()


}  // end of ImageUtils class

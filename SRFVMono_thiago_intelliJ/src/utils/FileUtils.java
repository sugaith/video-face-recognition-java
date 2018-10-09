package utils;


// FileUtils.java
// Sajan Joseph, sajanjoseph@gmail.com
// http://code.google.com/p/javafaces/
// Modified by Andrew Davison, April 2011, ad@fivedots.coe.psu.ac.th
// Adaptado por Thiago C L da Silva, cls.thiago@gmail.com


import eigenfaces.EigenSpace;

import java.io.*;
import java.util.*;
import java.awt.image.*;
import javax.imageio.*;


public class FileUtils {
    private static final String FILE_EXT = ".png";

//    private static final String TRAINING_DIR = "trainingImages";
    public static final String TRAINING_DIR = "imagensTreinamento";

//    private static final String EF_CACHE = "eigen.cache";
    public static final String EF_CACHE = "eigenspace.cache";

    private static final String EIGENFACES_DIR = "eigenfaces";
    private static final String EIGENFACES_PREFIX = "eigen_";

    private static final String RECON_DIR = "reconstruidos";
    private static final String RECON_PREFIX = "recon_";


    public static ArrayList<String> getNomePath_treinamento()
    // return all the names of the training image files + their paths
    {
        File dirF = new File(TRAINING_DIR);
        String[] fnms = dirF.list(new FilenameFilter() {
            public boolean accept(File f, String name) {
                return name.endsWith(FILE_EXT);
            }
        });

        if (fnms == null) {
            System.out.println(TRAINING_DIR + " not found");
            return null;
        } else if (fnms.length == 0) {
            System.out.println(TRAINING_DIR + " contains no " + " " + FILE_EXT + " files");
            return null;
        } else
            return getPathNomes(fnms);
    }  // end of getNomePath_treinamento()


    private static ArrayList<String> getPathNomes(String[] nomes) {
        ArrayList<String> imFnms = new ArrayList<String>();
        for (String fnm : nomes)
            imFnms.add(TRAINING_DIR + File.separator + fnm);

        Collections.sort(imFnms);
        return imFnms;
    }  // end of getPathNomes()


    /*
        Recebe array dos nomes dos arquivos e retorna-os como BufferedImage.
        Transforma a imagem em escala de cinza se necessário.

        @ listaPaths = Path completo dos arquivos de imagens a serem carregados
     */
    public static BufferedImage[] carregaImagensDeTreino(ArrayList<String> listaPaths){
        System.out.println(":: CARREGANDO IMAGENS DE TREINAMENTO ::  " + TRAINING_DIR);

        BufferedImage[] ims = new BufferedImage[listaPaths.size()];
        BufferedImage im = null;

        int i = 0;
        for (String fnm : listaPaths) {
            try {
                System.out.println(":: Carregando imagem -> " + fnm);
                im = ImageIO.read(new File(fnm));
                ims[i++] = ImageUtils.toScaledGray(im, 1.0);
            } catch (Exception e) {
                System.out.println("Falha ao carregar imagen -> " + fnm);
            }
        }
        System.out.println("Loading done\n");

        ImageUtils.checkImSizes(listaPaths, ims);
        return ims;
    }


    public static BufferedImage carregaImagem(String imFnm)

    {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(imFnm));   // read in as an image
            System.out.println("Reading image " + imFnm);
        } catch (Exception e) {
            System.out.println("Could not read image from " + imFnm);
        }
        return image;
    }


    public static void saveImage(BufferedImage im, String fnm)

    {
        try {
            ImageIO.write(im, "png", new File(fnm));
            // System.out.println("Saved image to " + fnm);
        } catch (IOException e) {
            System.out.println("Could not save image to " + fnm);
        }
    }  // end of saveImage()





    public static EigenSpace carregaEigenSpace2RAM()
    // read the EigenSpace object from a file called EF_CACHE
    {
        EigenSpace bundle = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(EF_CACHE));
            bundle = (EigenSpace) ois.readObject();
            ois.close();
            System.out.println("utilizando bundle: " + EF_CACHE);
            return bundle;
        } catch (FileNotFoundException e) {
            System.out.println("bundle nao encontrado! " + EF_CACHE);
            File f = new File(".//");
            System.out.println("path procurado: " + f.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("erro na leitura do bundle: " + EF_CACHE);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        }
        return bundle;
    }     // end of carregaEigenSpace2RAM()


    public static void salvarEigenSpace(EigenSpace bundle)

    {
        System.out.println("salvando bundle de eigenfaces em: " + EF_CACHE + " ...");
        try {
            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(EF_CACHE));
            oos.writeObject(bundle);
            System.out.println("bundle salva com sucesso!");
            oos.close();
        } catch (Exception e) {
            System.out.println("falha ao salvar o bundle");
            System.out.println(e);
        }
    }





    public static void salvarEigenfaces_imgs(Matriz2D egfaces, int imWidth)
  /* save each row of the eigenfaces matrix as an image in EIGENFACES_DIR, 
     whose pixel width is imWidth */ {
        double[][] egFacesArr = egfaces.toArray();
        mkdir(EIGENFACES_DIR);

        for (int row = 0; row < egFacesArr.length; row++) {
            String fnm = EIGENFACES_DIR + File.separator + EIGENFACES_PREFIX + row + FILE_EXT;
            saveArrAsImage(fnm, egFacesArr[row], imWidth);
        }
    }


    private static void mkdir(String dir)

    {
        File dirF = new File(dir);
        if (dirF.isDirectory()) {
            System.out.println("Directory: " + dir + " already exists; deleting its contents");
            for (File f : dirF.listFiles())
                delFile(f);
        } else {
            dirF.mkdir();
            System.out.println("Created new directory: " + dir);
        }
    }


    private static void delFile(File f) {
        if (f.isFile()) {
            boolean deleted = f.delete();
   /* if(deleted)
        System.out.println("  deleted: "+ f.getName() );
   */
        }
    }


    private static void saveArrAsImage(String fnm, double[] imData, int width)
    // save a ID array as an image
    {
        BufferedImage im = ImageUtils.createImFromArr(imData, width);
        if (im != null) {
            try {
                ImageIO.write(im, "png", new File(fnm));
                System.out.println("  " + fnm);    // saving
            } catch (Exception e) {
                System.out.println("Could not save image to " + fnm);
            }
        }
    }





    public static void saveReconIms2(double[][] ims, int imWidth)
  /* save each row of the images array as a separate image in RECON_DIR, 
     whose pixel width is imWidth */ {
        mkdir(RECON_DIR);
        for (int i = 0; i < ims.length; i++) {
            String fnm = RECON_DIR + File.separator + RECON_PREFIX + i + FILE_EXT;
            saveArrAsImage(fnm, ims[i], imWidth);
        }
    }  // end of saveReconIms()


}  // end of FileUtils class
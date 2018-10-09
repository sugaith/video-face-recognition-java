package utils;

import java.io.File;
import java.io.FilenameFilter;

import static org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_imgcodecs;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import org.bytedeco.javacpp.opencv_imgproc;
import utils.FileUtils;
import utils.ImageUtils;


public class EqualizarHistograma {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
                                    
        String trainingDir = "trainingImagesFF";
        //Mat testImage = imread(args[1], CV_LOAD_IMAGE_GRAYSCALE);

        File root = new File(trainingDir);

        FilenameFilter imgFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
            }
        };

        File[] imageFiles = root.listFiles(imgFilter);

        for (File image : imageFiles) {
            Mat img = imread(image.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
            System.out.println(image.getName());
            
            opencv_imgproc.equalizeHist(img,img);
            
            opencv_imgcodecs.imwrite(FileUtils.TRAINING_DIR +"_equalizeHist\\" + image.getName(),img);
        }
        
        
        System.out.println("PASTA NORMALIZADA! " + (System.currentTimeMillis() - startTime) + "ms");
    }
}

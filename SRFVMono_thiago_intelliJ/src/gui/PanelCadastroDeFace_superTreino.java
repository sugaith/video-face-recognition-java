package gui;


// FaceRecogPanel.java
// Andrew Davison, March 2011, ad@fivedots.psu.ac.th

/* This panel repeatedly snaps a picture and draw it onto
   the panel.  A face is highlighted with a yellow rectangle, which is updated 
   as the face moves. A "crosshairs" graphic is also drawn, positioned at the
   center of the rectangle.

   The highlighted part of the image can be recognized.

   Face detection is done using a Haar face classifier in JavaCV. 
   It is executed inside its own thread since the processing can be lengthy,
   and I don't want the image grabbing speed to be affected.

   This is an extension of the FacesPanel class in the Face Tracking example.
   The recognition is done with the ACP_Reconhecimento class from the JavaFaces
   example.
*/


import java.awt.*;
import javax.swing.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import javax.imageio.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import eigenfaces.ACP_Treinamento;
import eigenfaces.EigenSpace;
import eigenfaces.ACP_Reconhecimento;
import eigenfaces.ResultadoReconhecimento;
import utils.FileUtils;
import utils.ImageUtils;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;   // for grabber/recorder constants
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvFlip;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_INTER_LINEAR;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvEqualizeHist;
import static org.bytedeco.javacpp.opencv_imgproc.cvResize;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_ROUGH_SEARCH;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_FIND_BIGGEST_OBJECT;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.VideoInputFrameGrabber;




public class PanelCadastroDeFace_superTreino extends JPanel implements Runnable
{
  /* dimensao de cada imagem; o panel eh do mesmo tamanho da imagem */
  private static final int WIDTH = 640;  
  private static final int HEIGHT = 480;

  private static final int DELAY = 100;  // ms 

  private static final int CAMERA_ID = 0;

  private static final int IM_SCALE = 4;   
  private static final int DETECT_DELAY = 250;   // tempo (ms) entre cada deteccao de face
  private static final int MAX_TASKS = 4;    // max no. of tasks that can be waiting to be executed
  private static final int COUNT_TRAINING_FACES = 5;//faces colhidas para treino

  // definicao cascade  para deteccao de face
  private static final String FACE_CASCADE_FNM = "haarcascade_frontalface_alt.xml";
                                                 // "haarcascade_frontalface_alt2.xml";
     /* outros em  C:\OpenCV2.2\data\haarcascades\
       e http://alereimondo.no-ip.org/OpenCV/34
     */



  // para reconhecer e detectar uma imagem
  // private static final String FACE_FNM = "savedFace.png";
  private static final int FACE_WIDTH = 125;
  private static final int FACE_HEIGHT = 150;

  private IplImage snapIm = null;
  private volatile boolean isRunning;
  private volatile boolean isFinished;

  // usado para calcular o tempo medio de snap em ms
  private int imageCount = 0;
  private long totalTime = 0;
  private Font msgFont;

  // variaveis JavaCV 
  private CvHaarClassifierCascade classifier;
  private CvMemStorage storage;
  private CanvasFrame debugCanvas;
  private IplImage grayIm;

  // usado para executar athread q faz a deteccao de face
  private ExecutorService executor;
  private AtomicInteger numTasks;  
  
  private long detectStartTime = 0;// grava o numero de tarefas de deteccao
  
  private Rectangle faceDetectada;     // FACE DETECTADA / REGONHECIDA
  private BufferedImage imagem_rosto;
  private static final String IMAGEM_ROSTO = "rosto-preto.png";

  private volatile boolean reconhecerFace = false, salvaFace = false, treinandoFace = false, salvandoBase = false;
  private ACP_Reconhecimento faceRecog;   // this class comes from the javaFaces example
  private String nomeDaFace = null;      // nome da ultima face reconhecida

  private FrameCadastroDeFace pai; 


  public PanelCadastroDeFace_superTreino(FrameCadastroDeFace pai) 
  {
    this.pai = pai;
    pai.setSalvarFaceVisible(false);
    
    setBackground(Color.white); 
    msgFont = new Font("SansSerif", Font.BOLD, 18);

    // load the crosshairs image (a transparent PNG)
    imagem_rosto = loadImage(IMAGEM_ROSTO);
  
    //prepara reconhecimento de face instanciando novo BUNDLE
    faceRecog = new ACP_Reconhecimento(20);

    executor = Executors.newSingleThreadExecutor();
      /* this executor manages a single thread with an unbounded queue.
         Only one task can be executed at a time, the others wait.
      */
    numTasks = new AtomicInteger(0);    
      // used to limit the size of the executor queue

    initDetector();
    faceDetectada = new Rectangle();

    new Thread(this).start();   // start updating the panel's image
  } // end of FaceRecogPanel()

  private BufferedImage loadImage(String imFnm)
  // return an image
  {
    BufferedImage image = null;
    try {
      image = ImageIO.read( new File(imFnm) );   // read in as an image
       System.out.println("lendo imagem " + imFnm);
    }
    catch (Exception e) {
      System.out.println("nao deu pra ler imagem de: " + imFnm);
    }
    return image;
  }  // end of carregaImagem()


  private void initDetector()
  {
    // instantiate a classifier cascade for face detection
    classifier = new CvHaarClassifierCascade(cvLoad(FACE_CASCADE_FNM));
    if (classifier.isNull()) {
      System.out.println("\nArquivo do algoritimo haar cascade Classifier nao encontrado: " + FACE_CASCADE_FNM);
      System.exit(1);
    }

    storage = CvMemStorage.create();  // create storage used during object detection

    // debugCanvas = new CanvasFrame("Debugging Canvas");
             // useful for showing JavaCV IplImage objects, to check on image processing    
  }  // end of initDetector()
  



  public Dimension getPreferredSize()
  // make the panel wide enough for an image
  {   return new Dimension(WIDTH, HEIGHT); }



  public void run()
  /* display the current webcam image every DELAY ms
     The time statistics gathered here will NOT include the time taken to
     find a face, which are farmed out to a separate thread in trackFace().

     Tracking is only started at least every DETECT_DELAY (1000) ms, and only
     if the number of tasks is < MAX_TASKS (one will be executing, the others
     waiting)
  */
  {
    FrameGrabber grabber = initGrabber(CAMERA_ID);
    if (grabber == null)
      return;

    long duration;
    isRunning = true;
    isFinished = false;

    while (isRunning) {
      long startTime = System.currentTimeMillis();

      snapIm = picGrab(grabber, CAMERA_ID); 
        if(snapIm != null){
            cvFlip(snapIm, snapIm, 1);//flipa imagem horizontalmente (espelho)

            if ( ((System.currentTimeMillis() - detectStartTime) > DETECT_DELAY) &&
                 (numTasks.get() < MAX_TASKS) )
              trackFace(snapIm); 

            imageCount++;      
            //pinta a imagem na panel usando metodo sobrescrito paintComponent
            repaint();

            duration = System.currentTimeMillis() - startTime;
            totalTime += duration;
            if (duration < DELAY) {
              try {
                Thread.sleep(DELAY-duration);  // wait until DELAY time has passed
              } 
              catch (Exception ex) {}
            }          
        }
    }
    closeGrabber(grabber, CAMERA_ID);
    System.out.println("Execution End");
    isFinished = true;
  }  // end of run()



  private FrameGrabber initGrabber(int ID)
  {
    FrameGrabber grabber = null;
    //System.out.println("Initializing grabber for " + videoInput.getDeviceName(ID) + " ...");
    System.out.println("inicializando grabberpara webcam com ID " + ID);
    
    try {
      //grabber = FrameGrabber.createDefault(ID);
      grabber = new VideoInputFrameGrabber(ID);
      grabber.setFormat("dshow");       // using DirectShow
      //grabber.setImageWidth(WIDTH);     // default is too small: 320x240
      //grabber.setImageHeight(HEIGHT);
      grabber.start();  
    }
    catch(Exception e) 
    {  System.out.println("Could not start grabber");  
       System.out.println(e);
       System.exit(1);
    }
    return grabber;
  }  // end of initGrabber()



  private IplImage picGrab(FrameGrabber grabber, int ID)
  {
    IplImage im = null;
    try {
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        im = converter.convert(grabber.grab());
      //im = grabber.grab();  // take a snap
    }
    catch(Exception e) 
    {  System.out.println("Problem grabbing image for camera " + ID);  }
    return im;
  }  // end of picGrab()



  private void closeGrabber(FrameGrabber grabber, int ID)
  {
    try {
      grabber.stop();
      grabber.release();
    }
    catch(Exception e) 
    {  System.out.println("Problem stopping grabbing for camera " + ID);  }
  }  // end of closeGrabber()




  @Override
  public void paintComponent(Graphics g)
  /* Draw the image, the rectangle (and crosshairs) around a detected
     face, and the average ms snap time at the bottom left of the panel. 
     Show the currently recognized face name at the bottom middle.
     This time does NOT include the face detection task.
  */
  { 
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    if (snapIm != null){       
        g2.drawImage(IplImageToBufferedImage(snapIm), 0, 0, this); 
    }

    desenhaRosto(g2, WIDTH/2, HEIGHT/2);  
    drawRect(g2);
    writeStats(g2);
    writeStats2(g2);
    writeName(g2);
  }

public static BufferedImage IplImageToBufferedImage(IplImage src) {
    OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
    Java2DFrameConverter paintConverter = new Java2DFrameConverter();
    org.bytedeco.javacv.Frame frame = grabberConverter.convert(src);
    return paintConverter.getBufferedImage(frame,1);
}

  private void drawRect(Graphics2D g2)
  /* use the face rectangle to draw a yellow rectangle around the face, with 
     crosshairs at its center.
     The drawing of faceDetectada is in a synchronized block since it may be being
     updated or used for image saving at the same time in other threads.
  */
  {  
    synchronized(faceDetectada) {
      if (faceDetectada.width == 0)
        return;

      // draw a thick yellow rectangle
      g2.setColor(Color.YELLOW);
      g2.setStroke(new BasicStroke(3)); 
      g2.drawRect(faceDetectada.x, faceDetectada.y, faceDetectada.width, faceDetectada.height);

      int xCenter = faceDetectada.x + faceDetectada.width/2;
      int yCenter = faceDetectada.y + faceDetectada.height/2;
      
      //DESENHA CROSS HAIR
      //drawCrosshairs(g2, xCenter, yCenter);
    }
  }  



  private void desenhaRosto(Graphics2D g2, int xCenter, int yCenter){
    if (imagem_rosto != null)
      g2.drawImage(imagem_rosto, xCenter - imagem_rosto.getWidth()/2, 
                               yCenter - imagem_rosto.getHeight()/2, this);
    else {
      g2.setColor(Color.RED);
      g2.fillOval(xCenter-10, yCenter-10, 20, 20);
    }
  } 

  private void writeName(Graphics2D g2) {
    if (nomeDaFace != null){
        g2.setColor(Color.YELLOW);
        g2.setFont(msgFont);        
        g2.drawString("OLÁ " + nomeDaFace, WIDTH/2, HEIGHT-10);
        
    }
  } 
  
  private void writeStats(Graphics2D g2) {
    g2.setColor(Color.green);
    g2.setFont(new Font("SansSerif", Font.BOLD, 9));
    if (imageCount > 0) {
      String statsMsg = String.format("%.1f fpms",
                                        ((double) totalTime / imageCount));
      g2.drawString(statsMsg, 5, HEIGHT-5);  
                        // write statistics in bottom-left corner
    }
    else  // no image yet
      g2.drawString("Loading...", 5, HEIGHT-5);
  }  
  
  private void writeStats2(Graphics2D g2){      
      g2.setFont(new Font("SansSerif", Font.BOLD, 18));
      if(salvaFace){
            g2.setColor(Color.green);
            g2.setFont(msgFont);
            g2.drawString("Treinando Face...", 50, HEIGHT-10);            
      }
      if(isSalvandoBase()){
            g2.setColor(Color.green);
            g2.setFont(msgFont);
            g2.drawString("Criando Nova Base..", 50, HEIGHT-10);            
      }
  } 



  public void closeDown()    { 
    isRunning = false;
    while (!isFinished) {
      try {
        Thread.sleep(DELAY);
      } 
      catch (Exception ex) {}
    }
  } 


private Integer getUltimoIndexNomeFace(String nomeFace){
    File folder = new File(".//trainingImages//");
    File[] listOfFiles = folder.listFiles();
    java.util.List<Integer> indices = new ArrayList<>();
    
    for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
            System.out.println("File " + listOfFiles[i].getName());
            if(listOfFiles[i].getName().startsWith(nomeFace+"_")){
                indices.add( Integer.valueOf( listOfFiles[i].getName().split(nomeFace+"_")[1].replace(".png", "") ) );  
            }
        } 
    }    
    if(indices.isEmpty())
        return 0;
    else
        return Collections.max(indices);
}


private boolean salvaFace_trainingImages(IplImage img, String nome, int numero){
    try {
        synchronized(faceDetectada) {
            if (faceDetectada.width == 0) {
                System.out.println("nao há face (salvaFaceMakeBundle)");
                return false;
            }
            BufferedImage clipIm = ImageUtils.clipToRectangle(IplImageToBufferedImage(img), 
                         faceDetectada.x, faceDetectada.y, faceDetectada.width, faceDetectada.height);
            BufferedImage faceIm = resizeToFaceWH( resizeImageAndGrayIt(clipIm) );
            FileUtils.saveImage(faceIm, "trainingImages\\" + nome + "_" + numero + ".png");
            return true;
        }
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

private BufferedImage retornaFaceBufferedImg(IplImage img){
    try {
        synchronized(faceDetectada) {
            if (faceDetectada.width == 0) {
                System.out.println("nao há face (salvaFaceMakeBundle)");
                return null;
            }
            BufferedImage clipIm = ImageUtils.clipToRectangle(IplImageToBufferedImage(img), 
                         faceDetectada.x, faceDetectada.y, faceDetectada.width, faceDetectada.height);
            BufferedImage faceIm = resizeToFaceWH( resizeImageAndGrayIt(clipIm) );            
            return faceIm;
        }
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}

  
  // ------------------------- face tracking / regognin ----------------------------
  private int ultimoIndexNomeFace = 0, countTrainingFaces = 0;
  private BufferedImage[] facesParaTreino = new BufferedImage[COUNT_TRAINING_FACES]; 
  private java.util.ArrayList<String> listaNomeFaces = new ArrayList<>();
  private void trackFace(final IplImage img)
  /* cria threads separadas para a deteccao eo reconhecimento:
          acha a face na imagem atual, guarda suas coordenadas em faceDetectada,
          depois reconhece a face, e mostra o nome da face na tela
          
          atualizacao: cadastro de face e treino adicionados 
  */ 
  {
    grayIm = escalaImagemCinza(img); 
    numTasks.getAndIncrement();     // increment no. of tasks before entering queue
    executor.execute(new Runnable() {
      public void run()
       { 
            detectStartTime = System.currentTimeMillis();
            CvRect rect = findFace(grayIm);
            if ( rect != null ) //detectou a face: logica:
            {   //salva posicao da face achada na variavel global
                setRectangle(rect);
                pai.setSalvarFaceVisible(true);

                if ( isSalvaTreinaFace() ) {                
                    String nomeFace = pai.getNomeFaceField();

                    BufferedImage imgFace = retornaFaceBufferedImg(img);
                    if(img != null){
                        facesParaTreino[countTrainingFaces++] = imgFace;
                        listaNomeFaces.add(nomeFace);

                        if( countTrainingFaces > COUNT_TRAINING_FACES-1 ){//TREINA ATÉ TER 'COUNT_TRAINING_FACES'x NUMERO DE FACES
                            setSalvaFace(false);//desliga o treino
                            long startTime = System.currentTimeMillis();
                            //update facebundle
                            EigenSpace novoEigenSpace = ACP_Treinamento.atualiza_EigenSpace(getFaceRecog().getEspaco_multidimensional(), facesParaTreino, listaNomeFaces);
                            getFaceRecog().setNovoEspaco(novoEigenSpace, 20);
                            System.out.println("BUNDLE ATUALIZADO EM " + (System.currentTimeMillis() - startTime) + "ms");            
                            //zera vetor de faces e de nomes
                            countTrainingFaces = 0;
                            listaNomeFaces.clear();
                            facesParaTreino = new BufferedImage[COUNT_TRAINING_FACES]; 

                            //cria nova thread para salvar novo facebundle no disco
                            /*
                            ExecutorService salvaBundle = Executors.newSingleThreadScheduledExecutor();
                            salvaBundle.execute(new Runnable() {
                                @Override
                                public void run() {
                                    salvandoBase = true; 
                                    long startTime = System.currentTimeMillis();
                                    FileUtils.salvarEigenSpace(faceRecog.getEspaco_multidimensional());
                                    System.out.println("BUNDLE SALVO EM " + (System.currentTimeMillis() - startTime) + "ms");                                
                                    salvandoBase = false;
                                }
                            });   
                            */
                        }
                    }
                }else{
                    recogFace(img);
                }
            }else{//nao achou face
                faceDetectada.width = 0;
                pai.setSalvarFaceVisible(false);
                nomeDaFace = null;
            }
                long detectDuration = System.currentTimeMillis() - detectStartTime;
                //System.out.println(" Tempo entre SNAPs: " + detectDuration + "ms");
                numTasks.getAndDecrement();  // decrement no. of tasks since finished
                } 
       
    });
  }  // end of trackFace()

  private IplImage escalaImagemCinza(IplImage img)
  /* Scale the image and convert it to grayscale. Scaling makes
     the image smaller and so faster to process, and Haar detection
     requires a grayscale image as input
  */
  {
    // convert to grayscale
    IplImage grayImg = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 1);
    cvCvtColor(img, grayImg, CV_BGR2GRAY);  

    // scale the grayscale (to speed up face detection)
    IplImage smallImg = IplImage.create(grayImg.width()/IM_SCALE, 
                                        grayImg.height()/IM_SCALE, IPL_DEPTH_8U, 1);
    cvResize(grayImg, smallImg, CV_INTER_LINEAR);

    cvReleaseImage(grayImg);
    // equalize the small grayscale
	cvEqualizeHist(smallImg, smallImg);
    return smallImg;
  }  // end of scaleGray()



  private CvRect findFace(IplImage grayIm)
  /* The Haar detector is a JavaCV function, so requires an IplImage object.
     Also, use JavaCV's grayscale equalizer to improve the image.
  */
  {
/*
     // show the greyscale image to check on image processing steps
     debugCanvas.showImage(grayIm);
	 debugCanvas.waitKey(0);
*/
    // System.out.println("Detecting largest face...");   // cvImage
    CvSeq faces = cvHaarDetectObjects(grayIm, classifier, storage, 1.1, 1,  // 3
                              // CV_HAAR_SCALE_IMAGE |
                              CV_HAAR_DO_ROUGH_SEARCH | CV_HAAR_FIND_BIGGEST_OBJECT); 
          // speed things up by searching for only a single, largest face subimage

    int total = faces.total();
    if (total == 0) {
      // System.out.println("No faces found");
      return null;
    }
    else if (total > 1)   // this case should not happen, but included for safety
      System.out.println("Multiple faces detected (" + total + "); using the first");
    // else
    //  System.out.println("Face detected");

    CvRect rect = new CvRect(cvGetSeqElem(faces, 0));

    cvClearMemStorage(storage);
    return rect;
  }  // end of findface()



  private void setRectangle(CvRect r)
  /* Extract the (x, y, width, height) values of the highlighted image from
     the JavaCV rectangle data structure, and store them in a Java rectangle.
     In the process, undo the scaling which was applied to the image before face 
     detection was carried out.
     Report any movement of the new rectangle compared to the previous one.
     The updating of faceDetectada is in a synchronized block since it may be used
     for drawing or image saving at the same time in other threads.
  */
  {  
    synchronized(faceDetectada) {
      int xNew = r.x()*IM_SCALE;
      int yNew = r.y()*IM_SCALE;
      int widthNew = r.width()*IM_SCALE;
      int heightNew = r.height()*IM_SCALE;

      // calculate movement of the new rectangle compared to the previous one
     // int xMove = (xNew + widthNew/2) - (faceDetectada.x + faceDetectada.width/2);
     // int yMove = (yNew + heightNew/2) - (faceDetectada.y + faceDetectada.height/2);

      // report movement only if it is 'significant'
      // if ((Math.abs(xMove)> SMALL_MOVE) || (Math.abs(yMove) > SMALL_MOVE))
      //  System.out.println("Movement (x,y): (" + xMove + "," + yMove + ")" );

      faceDetectada.setRect( xNew, yNew, widthNew, heightNew);
      // System.out.println("Rectangle: " + faceDetectada);
    }
  }  // end of setRectangle()




  // ---------------- face recognition -------------------------

  public void setReconhecerFace()
  {     setReconhecerFace(true);  }

  private boolean recogFace(IplImage img)
  /* 
    clipa a imagem usando o retangulo da face achada, e tenta reconhece-la          
     o uso de faceDetectada estano bloco sincrronizado pq pose ser atualizado ou ultilizado por outras threads
  */
  {
    BufferedImage clipIm = null;
    synchronized(faceDetectada) {
        if (faceDetectada.width == 0) {
          System.out.println("nao há face");
          return false;
        }
        clipIm = ImageUtils.clipToRectangle(IplImageToBufferedImage(img), 
                     faceDetectada.x, faceDetectada.y, faceDetectada.width, faceDetectada.height);
    }
    if (clipIm != null) 
        return matchClip(clipIm);
    else
        return false;    
  }  

  private boolean matchClip(BufferedImage clipIm)
  // faz resize, converte pra grayscale, clipa pra FACE_WIDTH*FACE_HEIGHT, reconhece
  {
    long startTime = System.currentTimeMillis();
    System.out.println("COMPARANDO SNAP...");
    BufferedImage faceIm = resizeToFaceWH( resizeImageAndGrayIt(clipIm) );
    //FileUtils.saveImage(faceIm, FACE_FNM);    
    ResultadoReconhecimento result = getFaceRecog().match(faceIm);
    System.out.println("tempo de recog: " + (System.currentTimeMillis() - startTime) + " ms");
    
    if (result == null){
        System.out.println("RECOG SEM RESULTADO! =/");
        return false;
    }else {
        double distancia = result.getMatchDistance();
        String distStr = String.format("%.4f", distancia);
//        String nomeDaFaceBanco = new DaoFuncionario(conexao).consultaPK(Integer.valueOf(result.getName())).getNome();
        String nomeDaFaceBanco = result.getName();
        if( distancia < 0.36 ){ //se for menor q 0.36 = nota boa aparece o nome
            nomeDaFace = nomeDaFaceBanco;
            
            System.out.println("  RECONHECIDO: " + nomeDaFace +" ("+distStr+")" );
            System.out.println("  foto: " + result.getMatchFileName() );
            pai.setRecogName(nomeDaFace, distStr);
            return true;
        }else{
            nomeDaFace = null;
            pai.setRecogName("", "");
            System.out.println("RESULTADO DUVIDOSO: " + nomeDaFaceBanco +" ("+distStr+")" );
            System.out.println("  foto: " + result.getMatchFileName() );
            
            return false;
        }
    }
  }  

  private BufferedImage resizeImageAndGrayIt(BufferedImage im)
  /* resize so *at least* FACE_WIDTH*FACE_HEIGHT size
     and convert to grayscale */
  {
    double widthScale = FACE_WIDTH / ((double) im.getWidth());
    double heightScale = FACE_HEIGHT / ((double) im.getHeight());
    double scale = (widthScale > heightScale) ? widthScale : heightScale;
    return ImageUtils.toScaledGray(im, scale);
  }  // end of resizeImage()



  private BufferedImage resizeToFaceWH(BufferedImage im)
  // clip image to FACE_WIDTH*FACE_HEIGHT size
  // I assume the input image is face size or bigger
  {
    int xOffset = (im.getWidth() - FACE_WIDTH)/2;
    int yOffset = (im.getHeight() - FACE_HEIGHT)/2;
    return ImageUtils.clipToRectangle(im, xOffset, yOffset, FACE_WIDTH, FACE_HEIGHT);
  }  // end of clipToFace()

    /**
     * @return the salvaFace
     */
    public boolean isSalvaTreinaFace() {
        return salvaFace;
    }

    /**
     * @param salvaFace the salvaFace to set
     */
    public void setSalvaFace(boolean salvaFace) {
        this.salvaFace = salvaFace;
    }

    /**
     * @return the reconhecerFace
     */
    public boolean isReconhecerFace() {
        return reconhecerFace;
    }

    /**
     * @param reconhecerFace the reconhecerFace to set
     */
    public void setReconhecerFace(boolean reconhecerFace) {
        this.reconhecerFace = reconhecerFace;
    }

    /**
     * @return the faceRecog
     */
    public ACP_Reconhecimento getFaceRecog() {
        return faceRecog;
    }

    /**
     * @param faceRecog the faceRecog to set
     */
    public void setFaceRecog(ACP_Reconhecimento faceRecog) {
        this.faceRecog = faceRecog;
    }

    /**
     * @return the salvandoBase
     */
    public boolean isSalvandoBase() {
        return salvandoBase;
    }

    /**
     * @param salvandoBase the salvandoBase to set
     */
    public void setSalvandoBase(boolean salvandoBase) {
        this.salvandoBase = salvandoBase;
    }



} // end of FaceRecogPanel class


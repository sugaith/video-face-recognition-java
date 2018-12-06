private static EigenSpace gerar_EigenSpace(ArrayList<String> fnms){
        //PASSO 1 :: DADOS!
        //carrega imagens da pasta de treinamento
        BufferedImage[]ims=FileUtils.carregaImagensDeTreino(fnms);
        //converte bufferedImage para matriz2D
        Matriz2D matriz_imgs=buffImage2Matriz2D(ims);

        //PASSO 2:: CALCULAR A MÉDIA DE CADA IMAGEM E APLICAR SUBTRAÇÃO COM AS MESMAS
        double[]avgImage=matriz_imgs.calcMedia_cols();
        matriz_imgs.subtrairMedia();//imagens de treino com a face média subtraída

        //PASSO 3:: CALCULAR A MATRIZ DE COVARIÂNCIA
        Matriz2D imsDataTr=matriz_imgs.transpose();
        Matriz2D covarMat=matriz_imgs.multiply(imsDataTr);

        //PASSO 4:: CALCULAR OS AUTOVETORES E AUTOVALORES DA MATRIZ DE COVARIANCIA
        AutoVetor_decomp egValDecomp=covarMat.getEigenvalueDecomp();
        double[]egVals=egValDecomp.getEigenValues();
        double[][]egVecs=egValDecomp.getEigenVectors();

        //PASSO 4.1:: ordenar o vetor de autovetores por ordem de autovalores (para futuro possivel descarte)
        ordenaEgVecs(egVals,egVecs);

        //PASSO 5:: No último passo cada imagem de treinamento é projetada no espaço face.
        // "O descritor ACP (ou engeifaces, normalizados) é calculado por uma combinação linear
        // de Auto-vetores com os vetores originais."
        Matriz2D egFaces=calcEspaco(matriz_imgs,new Matriz2D(egVecs));

        System.out.println("::: Salvando EigenFaces como imagens...");
        FileUtils.salvarEigenfaces_imgs(egFaces,ims[0].getWidth());
        System.out.println("::: EIGENFACES GERADOS :::");

        //Para cada face, apenas os coeficientes  são armazenados para futura comparação.
        return new EigenSpace(fnms,matriz_imgs.toArray(),avgImage,egFaces.toArray(),
        egVals,ims[0].getWidth(),ims[0].getHeight()
        );
}
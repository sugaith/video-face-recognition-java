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
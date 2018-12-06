CvRect faceDetectada = detectarFace(grayIm);
if (faceDetectada != null) //detectou a face:
{   //salva posicao da face detectada na variavel global
    setRectangle(faceDetectada);
    //abre campos para identificacao
    pai.setSalvarFaceVisible(true);

    //se esta em estado de treinamento
    if (isSalvaTreinaFace()) {
        String nomeFace = pai.getNomeFaceField();
        if (!pegouOIndexDoNome) {
            ultimoIndexNomeFace = getUltimoIndexNomeFace(nomeFace);
            pegouOIndexDoNome = true;
        }
        if (salvaFace_trainingImages(img, nomeFace, ultimoIndexNomeFace++))
            countFacesParaTreino++;
        if (countFacesParaTreino > NUM_FACES_TREINO) {//TREINA ATE TER NUM_FACES_TREINO
            setSalvaFace(false);//desliga o treino

            //reseta vars de controle
            pegouOIndexDoNome = false;
            countFacesParaTreino = 0;
            pai.setSalvarFaceVisible(false);

            //thread para criar novo bundle
            ExecutorService criaBunbdle = Executors.newSingleThreadScheduledExecutor();
            criaBunbdle.execute(new Runnable() {
                @Override
                public void run() {
                    criandoBase = true;
                    long startTime = System.currentTimeMillis();
                    ACP_Treinamento.construirEspaco(NUM_EF_treino);//cria novo bundle
                    System.out.println("BUNDLE CRIADO EM " + (System.currentTimeMillis() - startTime) + "ms");
                    faceRecog = new ACP_Reconhecimento(NUM_EF_recog);//usa novo bundle
                    criandoBase = false;
                }
            });
        }
    } else {
        //reconhece face
        recogFace(img);
    }
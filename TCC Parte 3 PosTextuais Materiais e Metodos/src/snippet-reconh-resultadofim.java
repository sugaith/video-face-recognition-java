if (result == null) {
    System.out.println("RECOG SEM RESULTADO! =/");
    return false;
} else {
    double distancia = result.getMatchDistance();
    String distStr = String.format("%.4f", distancia);
    String nomeDaFaceBanco = result.getName();
    if (distancia < MIN_DIST) { //se for menor q MIN_DIST = sucesso
        nomeDaFace = nomeDaFaceBanco;

        System.out.println("  RECONHECIDO: " + nomeDaFace + " (" + distStr + ")");
        System.out.println("  foto: " + result.getMatchFileName());
        pai.setRecogName(nomeDaFace, distStr);
        return true;
    } else {
        nomeDaFace = null;
        pai.setRecogName("", "");
        System.out.println("RESULTADO DUVIDOSO: " + nomeDaFaceBanco + " (" + distStr + ")");
        System.out.println("  foto: " + result.getMatchFileName());

        return false;
    }
}
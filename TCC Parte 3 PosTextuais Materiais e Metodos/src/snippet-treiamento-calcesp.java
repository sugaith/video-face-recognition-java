private static Matriz2D calcEspaco(Matriz2D imsMat,Matriz2D egVecs){
        Matriz2D egVecsTr=egVecs.transpose();
        Matriz2D egFaces=egVecsTr.multiply(imsMat);
        double[][]egFacesData=egFaces.toArray();

        //normalizacao
        for(int row=0;row<egFacesData.length;row++){
            double norm=Matriz2D.norm(egFacesData[row]);   // valor normal
            for(int col=0;col<egFacesData[row].length;col++)
                 egFacesData[row][col]=egFacesData[row][col]/norm;//normaliza
        }
        return new Matriz2D(egFacesData);
}
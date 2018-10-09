package eigenfaces;


// InfoDistancia.java
// Sajan Joseph, sajanjoseph@gmail.com
// http://code.google.com/p/javafaces/
// Modified by Andrew Davison, April 2011, ad@fivedots.coe.psu.ac.th
// Adapted by Thiago CL Silva, cls.thiago@gmail.com


public class InfoDistancia {
    private int index;
    private double value;

    public InfoDistancia(double val, int idx) {
        value = val;
        index = idx;
    }

    public int getIndex() {
        return index;
    }

    public double getValue() {
        return value;
    }

}


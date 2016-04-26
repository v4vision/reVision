package org.v4vision.reVision.harrisjava;



public class ByteArrayContainer {

    private byte[] arr;

    private int[] corners;

    public ByteArrayContainer(byte[] arr) {
        this.arr = arr;
    }

    public byte[] get() {
        return arr;
    }

    public int[] getCorners() {
        return corners;
    }

    public void setCorners(int[] corners) {
        this.corners = corners;
    }
}

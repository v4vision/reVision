package org.v4vision.reVision.harrisjava;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class HarrisCornerDetector {

    private int imageWidth,imageHeight;

    public HarrisCornerDetector(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    private float cornerResponse(float[][] GIxx, float[][] GIyy, float[][] GIxy, int width, int height) {
        float k = 0.04f;
        float threshold = 10000;

        float response = (GIxx[width][height] * GIyy[width][height] - GIxy[width][height] * GIxy[width][height]) -
                k * (GIxx[width][height] + GIyy[width][height]) * (GIxx[width][height] + GIyy[width][height]);
        return response > threshold ? response : 0;
    }

    private float gaussianFilter(float[][] A, int width, int height) {
        float[] gaussianKernel = new float[] {
                0.004f, 0.015f, 0.026f, 0.015f, 0.004f,
                0.015f, 0.059f, 0.095f, 0.059f, 0.015f,
                0.026f, 0.095f, 0.15f, 0.095f, 0.026f,
                0.015f, 0.059f, 0.095f, 0.059f, 0.015f,
                0.004f, 0.015f, 0.026f, 0.015f, 0.004f
        };

        return
                A[width - 2][height - 2] * gaussianKernel[0] +
                        A[width - 1][height - 2] * gaussianKernel[1] +
                        A[width][height - 2] * gaussianKernel[2] +
                        A[width + 1][height - 2] * gaussianKernel[3] +
                        A[width + 2][height - 2] * gaussianKernel[4] +
                        A[width - 2][height - 1] * gaussianKernel[5] +
                        A[width - 1][height - 1] * gaussianKernel[6] +
                        A[width][height - 1] * gaussianKernel[7] +
                        A[width + 1][height - 1] * gaussianKernel[8] +
                        A[width + 2][height - 1] * gaussianKernel[9] +
                        A[width - 2][height] * gaussianKernel[10] +
                        A[width - 1][height] * gaussianKernel[11] +
                        A[width][height] * gaussianKernel[12] +
                        A[width + 1][height] * gaussianKernel[13] +
                        A[width + 2][height] * gaussianKernel[14] +
                        A[width - 2][height + 1] * gaussianKernel[15] +
                        A[width - 1][height + 1] * gaussianKernel[16] +
                        A[width][height + 1] * gaussianKernel[17] +
                        A[width + 1][height + 1] * gaussianKernel[18] +
                        A[width + 2][height + 1] * gaussianKernel[19] +
                        A[width - 2][height + 2] * gaussianKernel[20] +
                        A[width - 1][height + 2] * gaussianKernel[21] +
                        A[width][height + 2] * gaussianKernel[22] +
                        A[width + 1][height + 2] * gaussianKernel[23] +
                        A[width + 2][height + 2] * gaussianKernel[24];
    }

    private Bitmap byteArrayToBitmap(byte[] byteArray)
    {
        double startingTime = System.currentTimeMillis();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(byteArray, ImageFormat.NV21, imageWidth, imageHeight, null);

        yuvImage.compressToJpeg(new Rect(0, 0, imageWidth, imageHeight), 100, baos);

        byte[] imageData = baos.toByteArray();
        Log.d("byteArrayToBitmap", "Total time: " + (System.currentTimeMillis() - startingTime));
        return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
    }

    public float grayscale(int pixel) {
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        return red * 0.2126f + green * 0.7152f + blue * 0.0722f;
    }

    public Bitmap detect(byte[] data) {
        double startingTime = System.currentTimeMillis();

        Bitmap outputBitmap = byteArrayToBitmap(data).copy(Bitmap.Config.ARGB_8888, true);
        Log.d("ProcessData", "After copy");

        float[][] grayscale = new float[imageWidth][imageHeight];

        float[][] convolutionX = new float[imageWidth][imageHeight];
        float[][] convolutionY = new float[imageWidth][imageHeight];
        float[][] Ixx = new float[imageWidth][imageHeight];
        float[][] Iyy = new float[imageWidth][imageHeight];
        float[][] Ixy = new float[imageWidth][imageHeight];
        float[][] GIxx = new float[imageWidth][imageHeight];
        float[][] GIyy = new float[imageWidth][imageHeight];
        float[][] GIxy = new float[imageWidth][imageHeight];

        float[][] cornerResponse = new float[imageWidth][imageHeight];

        for(float[] row: cornerResponse) {
            Arrays.fill(row, 0);
        }

        for(int height = 0; height < imageHeight; height++) {
            for (int width = 0; width < imageWidth; width++) {
                grayscale[width][height] = grayscale(outputBitmap.getPixel(width, height));
            }
        }

        Log.d("ProcessData", "Before Convolution Loop");

        // Convolution
        for(int height = 1; height < imageHeight - 1; height++) {
            for (int width = 1; width < imageWidth - 1; width++) {
                convolutionX[width][height] =
                        grayscale[width + 1][height - 1] - grayscale[width - 1][height - 1] +
                                grayscale[width + 1][height] - grayscale[width - 1][height] +
                                grayscale[width + 1][height + 1] - grayscale[width - 1][height + 1];

                convolutionY[width][height] =
                        grayscale[width - 1][height + 1] - grayscale[width - 1][height - 1] +
                                grayscale[width][height + 1] - grayscale[width][height - 1] +
                                grayscale[width + 1][height + 1] - grayscale[width + 1][height - 1];

                Ixx[width][height] = convolutionX[width][height] * convolutionX[width][height];
                Iyy[width][height] = convolutionY[width][height] * convolutionY[width][height];
                Ixy[width][height] = convolutionX[width][height] * convolutionY[width][height];
            }
            //Log.d("ProcessData", "Convolution Loop for height: " + height);
        }

        Log.d("ProcessData", "After Convolution Loop");

        // Gaussian
        for(int height = 2; height < imageHeight - 2; height++) {
            for(int width = 2; width < imageWidth - 2; width++) {
                GIxx[width][height] = gaussianFilter(Ixx, width, height);
                GIyy[width][height] = gaussianFilter(Iyy, width, height);
                GIxy[width][height] = gaussianFilter(Ixy, width, height);

                cornerResponse[width][height] = cornerResponse(GIxx, GIyy, GIxy, width, height);
            }
        }

        // Non-maxima suppression
        for(int height = 5; height < imageHeight - 5; height++) {
            for (int width = 5; width < imageWidth - 5; width++) {
                float tmp = cornerResponse[width][height];
                for(int y = -5; y < 6 && tmp != 0; y++) {
                    for(int x = -5; x < 6; x++) {
                        if(cornerResponse[width][height] < cornerResponse[width + x][height + y]) {
                            //TODO: check if these two lines do the same thing!
                            cornerResponse[width][height] = 0;
                            tmp = 0;
                            break;
                        }
                    }
                }
                if(tmp != 0) {
                    outputBitmap.setPixel(width, height, Color.GREEN);
                }
            }

        }

        Log.d("JavaActivity", "Harris Finished with Total Time: " + (System.currentTimeMillis() - startingTime));
        return outputBitmap;
    }
}

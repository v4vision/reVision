package org.v4vision.reVision.harrisjava;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class JavaActivity extends Activity  implements Camera.PreviewCallback, SurfaceHolder.Callback {

    private Camera.Parameters params;
    private Camera camera;

    // couple of variables that controls the pipeline:
    // a flag to skip outstanding frames from the camera, when there some frame is already in process
    private volatile boolean    RenderScriptIsWorking;
    // an on/off flag for the video effect
    private volatile boolean    ApplyEffect;
    private volatile boolean    Convolution5;

    private Bitmap outputBitmap;

    private ImageView outputImageView;

    //last wall-clock frame time
    private long   prevFrameTimestampProcessed;
    private long   prevFrameTimestampCaptured;
    // Frame times, accumulated over the number of iterations using exp moving average.
    private double frameDurationAverProcessed;    // time between processed frames
    private double frameDurationAverCaptured;     //time between captured frames
    private double frameDurationAverJNI;
    // blend factor used to calc exp moving average
    final double   blendFactor = 0.05;
    // time from the last FPS update on the screen.
    private double FPSDuration;

    int imageWidth;
    int imageHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        outputImageView = (ImageView)findViewById(R.id.outputImageView);
        //((TextView)findViewById(R.id.my_text_view)).setText(getMsgFromJni());

        camera = Camera.open(0);
        params = camera.getParameters();

        int pixels = 800*600;
        int dMin = Math.abs(params.getPreviewSize().width*params.getPreviewSize().height-pixels);
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        for(int i = 0; i < sizes.size(); ++i)
        {
            Camera.Size size = sizes.get(i);
            int d = Math.abs(size.width*size.height - pixels);
            if( d < dMin )
            {
                params.setPreviewSize(size.width,size.height);
                dMin = d;
            }
        }
        //params.setPreviewFormat(ImageFormat.NV21);
        //auto-focus
        if (params.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        camera.setParameters(params);
        // get real preview parameters TODO: ?
        params = camera.getParameters();
        imageWidth  = params.getPreviewSize().width;
        imageHeight = params.getPreviewSize().height;
        Log.i("CameraRenderscript", "getPreviewSize() " + imageWidth + "x" + imageHeight);
        camera.release();
        camera = null;

        outputBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.RGB_565);

        //get preview surface for camera preview and set callback for surface
        //the layout is specified the way the inputSurfaceView is completely overlayed by outputSurfaceView
        // outputSurfaceView is the view that displays the resulting image
        // so inputSurfaceView is rather fake view for unprocessed preview frames
        // the inputSurfaceView is required just to setup the callback
        SurfaceView surView = (SurfaceView) findViewById(R.id.inputSurfaceView);
        SurfaceHolder surHolder = surView.getHolder();
        surHolder.addCallback(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            prevFrameTimestampProcessed = System.nanoTime();
            prevFrameTimestampCaptured = prevFrameTimestampProcessed;
            camera = Camera.open(0);
            camera.setParameters(params);
            camera.setPreviewCallback(this);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();
        camera = null;

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // calc time since previous call of this function
        long    curFrameTimestamp = System.nanoTime();
        // required FPS for the effect
        final double  EffectFPS = 40.0;
        // current average FPS
        double  AverFPS = (1e9f/frameDurationAverProcessed);
        // duration between last 2 processed frames (i.e. post-processed with RS effect and displayed)
        long    frameDurationProcessed =   curFrameTimestamp - prevFrameTimestampProcessed;
        // duration between last captured (i.e. for which a preview data arrived) frame and current being processed
        long     frameDurationCaptured  =   curFrameTimestamp - prevFrameTimestampCaptured;
        // calc average time between captured frames
        // we need this time to calculate time threshold to achieve exact EffectFPS that we need for effect
        frameDurationAverCaptured += (frameDurationCaptured-frameDurationAverCaptured)*blendFactor;
        prevFrameTimestampCaptured = curFrameTimestamp;

        // calc time interval since last processing
        FPSDuration += (double)frameDurationCaptured;
        if(FPSDuration>0.5e9f)
        {//update FPS on the screen every 0.5 sec
            double RenderScriptFPS = 1e9/ frameDurationAverJNI;
            getActionBar().setSubtitle(String.format("%dx%d: %4.1f FPS (RenderScript: %4.1f FPS)", imageWidth, imageHeight, AverFPS, RenderScriptFPS));
            FPSDuration = 0;
        }

        double frameDurationT = (1e9f/EffectFPS);
        if(AverFPS<EffectFPS) // correct duration threshold in case of averageFPS is not enough
            frameDurationT -= frameDurationAverCaptured;
        //skip frame if processing of the previous is not finished yet
        // or FPS is higher than 12, since being slow/jerky is important for OldMovie perception
        if (RenderScriptIsWorking || (ApplyEffect && (frameDurationProcessed<frameDurationT)))
            return;

        // submit frame to process in background
        RenderScriptIsWorking = true;

        Log.d("JavaActivity", "imageSize: " + imageWidth + "x" + imageHeight);
        Log.d("JavaActivity", "byte[] size: " + data.length);

        new ProcessData().execute(new ByteArrayContainer(data));
//
//        outputBitmap = BitmapFactory.decodeByteArray(data,0,data.length);
//
//        outputImageView.setImageBitmap(outputBitmap);
//        outputImageView.invalidate();

        // update last processed time stamp and processing time average
        prevFrameTimestampProcessed = curFrameTimestamp;
        frameDurationAverProcessed += (frameDurationProcessed-frameDurationAverProcessed)*blendFactor;
    }

    private float cornerResponse(float[] GIxx, float[] GIyy, float[] GIxy, int index) {
        float k = 0.04f;
        float threshold = 300000000;
        float response = (GIxx[index] * GIyy[index] - GIxy[index] * GIxy[index]) -
                        k * (GIxx[index] + GIyy[index]) * (GIxx[index] + GIyy[index]);
        return (response < -threshold) || (response > threshold) ? response : 0;
    }

    private float gaussianFilter(float[] A, int index) {
        float[] gaussianKernel = new float[] {
                0.004f, 0.015f, 0.026f, 0.015f, 0.004f,
                0.015f, 0.059f, 0.095f, 0.059f, 0.015f,
                0.026f, 0.095f, 0.15f, 0.095f, 0.026f,
                0.015f, 0.059f, 0.095f, 0.059f, 0.015f,
                0.004f, 0.015f, 0.026f, 0.015f, 0.004f
        };

        return
                A[index - 2 * imageWidth - 2] * gaussianKernel[0] +
                A[index - 2 * imageWidth - 1] * gaussianKernel[1] +
                A[index - 2 * imageWidth] * gaussianKernel[2] +
                A[index - 2 * imageWidth + 1] * gaussianKernel[3] +
                A[index - 2 * imageWidth + 2] * gaussianKernel[4] +
                A[index - imageWidth - 2] * gaussianKernel[5] +
                A[index - imageWidth - 1] * gaussianKernel[6] +
                A[index - imageWidth] * gaussianKernel[7] +
                A[index - imageWidth + 1] * gaussianKernel[8] +
                A[index - imageWidth + 2] * gaussianKernel[9] +
                A[index - 2] * gaussianKernel[10] +
                A[index - 1] * gaussianKernel[11] +
                A[index] * gaussianKernel[12] +
                A[index + 1] * gaussianKernel[13] +
                A[index + 2] * gaussianKernel[14] +
                A[index + imageWidth - 2] * gaussianKernel[15] +
                A[index + imageWidth - 1] * gaussianKernel[16] +
                A[index + imageWidth] * gaussianKernel[17] +
                A[index + imageWidth + 1] * gaussianKernel[18] +
                A[index + imageWidth + 2] * gaussianKernel[19] +
                A[index + 2 * imageWidth - 2] * gaussianKernel[20] +
                A[index + 2 * imageWidth - 1] * gaussianKernel[21] +
                A[index + 2 * imageWidth] * gaussianKernel[22] +
                A[index + 2 * imageWidth + 1] * gaussianKernel[23] +
                A[index + 2 * imageWidth + 2] * gaussianKernel[24];
    }

    private Bitmap byteArrayToBitmap(Bitmap bmp, byte[] byteArray)
    {
//        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
//        bmp.compress(Bitmap.CompressFormat.PNG, 100, baoStream);
//        byteArray = baoStream.toByteArray();
//        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        int[] intColors = new int[byteArray.length / 3];
        for (int intIndex = 0; intIndex < byteArray.length - 2; intIndex = intIndex + 3) {
            intColors[intIndex / 3] = (255 << 24) | (byteArray[intIndex] << 16) | (byteArray[intIndex + 1] << 8) | byteArray[intIndex + 2];
        }
        return Bitmap.createBitmap(intColors, imageWidth/3, imageHeight/3, Bitmap.Config.ARGB_8888);
//        int nrOfPixels = byteArray.length / 3; // Three bytes per pixel.
//        int pixels[] = new int[nrOfPixels];
//        for(int i = 0; i < nrOfPixels; i++) {
//            int r = byteArray[3*i];
//            int g = byteArray[3*i + 1];
//            int b = byteArray[3*i + 2];
//            pixels[i] = Color.rgb(r, g, b);
//        }
//        return Bitmap.createBitmap(pixels, imageWidth/3, imageHeight/3, Bitmap.Config.ARGB_8888);
    }

    private class ProcessData extends AsyncTask<ByteArrayContainer, Void, Boolean>
    {
        long RenderScriptTime;
        @Override
        protected Boolean doInBackground(ByteArrayContainer... args)
        {
            long rsStart = System.nanoTime();
            if(true)
            {
                int arraySize = imageWidth * imageHeight;
                byte[] grayscaleData = Arrays.copyOfRange(args[0].get(), 0, arraySize);

                float[] convolutionX = new float[arraySize];
                float[] convolutionY = new float[arraySize];
                float[] Ixx = new float[arraySize];
                float[] Iyy = new float[arraySize];
                float[] Ixy = new float[arraySize];
                float[] GIxx = new float[arraySize];
                float[] GIyy = new float[arraySize];
                float[] GIxy = new float[arraySize];
                Arrays.fill(convolutionX, 0);
                Arrays.fill(convolutionY, 0);
                Arrays.fill(Ixx, 0);
                Arrays.fill(Iyy, 0);
                Arrays.fill(Ixy, 0);
                Arrays.fill(GIxx, 0);
                Arrays.fill(GIyy, 0);
                Arrays.fill(GIxy, 0);

                float[] cornerResponse = new float[arraySize];
                Arrays.fill(cornerResponse, 0);

                ArrayList<Integer> corners = new ArrayList<Integer>();

                for(int height = 1; height < imageHeight; height++) {
                    int index1 = imageWidth * height;
                    for(int width = 1; width < imageWidth; width++) {
                        int index = index1 + width;
                        if((index % imageWidth < imageWidth - 1) && (index / imageWidth < imageHeight - 1)) {
                            convolutionX[index] =
                                    grayscaleData[index - imageWidth + 1] - grayscaleData[index - imageWidth - 1] +
                                            grayscaleData[index + 1] - grayscaleData[index - 1] +
                                            grayscaleData[index + imageWidth + 1] - grayscaleData[index + imageWidth - 1];

                            convolutionY[index] =
                                    grayscaleData[index + imageWidth + 1] - grayscaleData[index - imageWidth + 1] +
                                            grayscaleData[index + imageWidth] - grayscaleData[index - imageWidth] +
                                            grayscaleData[index + imageWidth - 1] - grayscaleData[index - imageWidth - 1];

                            Ixx[index] = convolutionX[index] * convolutionX[index];
                            Iyy[index] = convolutionY[index] * convolutionY[index];
                            Ixy[index] = convolutionX[index] * convolutionY[index];
                        }

                        if((index / imageWidth < imageHeight - 2) &&
                                (index % imageWidth < imageWidth - 2) &&
                                (index % imageWidth > 1) &&
                                (index / imageWidth > 1)) {
                            GIxx[index] = gaussianFilter(Ixx, index);
                            GIyy[index] = gaussianFilter(Iyy, index);
                            GIxy[index] = gaussianFilter(Ixy, index);

                            cornerResponse[index] = cornerResponse(GIxx, GIyy, GIxy, index);
                        }
                    }
                }

                for(int height = 5; height < imageHeight; height++) {
                    int index1 = imageWidth * height;
                    for (int width = 5; width < imageWidth; width++) {
                        int index = index1 + width;
                        if((index % imageWidth < imageWidth - 5) && (index / imageWidth < imageHeight - 5)) {
                            float tmp = cornerResponse[index];
                            for(int y = -5; y < 6 && tmp != 0; y++) {
                                for(int x = -5; x < 6; x++) {
                                    if(cornerResponse[index] < cornerResponse[index + y * imageWidth + x]) {
                                        //TODO: check if these two lines do the same thing!
                                        cornerResponse[index] = 0;
                                        tmp = 0;
                                        break;
                                    }
                                    else {
                                        corners.add(index);
                                    }
                                }
                            }
                        }
                    }
                }

                HashSet<Integer> distinctCorners = new HashSet<Integer>(corners);
                Log.d("Corners: ", distinctCorners.toString());

                for (Integer corner : distinctCorners) {
                    args[0].get()[corner] = 127;
                }

                //byte[] colors = Arrays.copyOfRange(args[0].get(), arraySize, args[0].get().length);
                outputBitmap = byteArrayToBitmap(outputBitmap, args[0].get());
            }
            else
            {

            }
            //long stepStart = System.nanoTime();

            long stepEnd = System.nanoTime();
            //Log.i("RenderScript Camera", "Copy time: "+(stepEnd-stepStart)/1000000.0f+" ms");
            RenderScriptTime = stepEnd - rsStart;

            return true;
        }
        protected void onPostExecute(Boolean result) {
            //update average render script time processing
            frameDurationAverJNI += (RenderScriptTime-frameDurationAverJNI)*blendFactor;

            //TODO understand
            Log.d("OnPost: ", "Execute :)");
            outputImageView.setImageBitmap(outputBitmap);
            outputImageView.invalidate();
            RenderScriptIsWorking = false;

        }
    }
}
package org.v4vision.reVision.examples.harris;

import java.io.IOException;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.widget.*;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import org.v4vision.reVision.core.harris.Harris;


public class MainActivity extends Activity implements Camera.PreviewCallback, SurfaceHolder.Callback
{
    // Output bitmap, serves as a destination for the image-processing RenderScript kernel
    private Bitmap outputBitmap;

    // Single ImageView that is used to output the resulting bitmap
    private ImageView outputImageView;

    private Harris harris;

    //size of processed image
    private int imageWidth;
    private int imageHeight;
    //chosen camera parameters
    private Camera.Parameters params;
    // camera in use
    private Camera camera;

    // couple of variables that controls the pipeline:
    // a flag to skip outstanding frames from the camera, when there some frame is already in process
    private volatile boolean    RenderScriptIsWorking;
    // an on/off flag for the video effect
    private volatile boolean    ApplyEffect;
    private volatile boolean    Convolution5;

    //last wall-clock frame time
    private long   prevFrameTimestampProcessed;
    private long   prevFrameTimestampCaptured;
    // Frame times, accumulated over the number of iterations using exp moving average.
    private double frameDurationAverProcessed;    // time between processed frames
    private double frameDurationAverCaptured;     //time between captured frames
    private double frameDurationAverRenderScript; // time that renderscript takes to process frame
    // blend factor used to calc exp moving average
    final double   blendFactor = 0.05;
    // time from the last FPS update on the screen.
    private double FPSDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        RenderScriptIsWorking = false;
        ApplyEffect = true;
        Convolution5 = true;
        FPSDuration = 0;
        prevFrameTimestampProcessed = System.nanoTime();
        prevFrameTimestampCaptured = System.nanoTime();
        frameDurationAverProcessed = 0;
        frameDurationAverCaptured = 0;
        frameDurationAverRenderScript = 0;

        setContentView(R.layout.activity_main);
        //get ImageView, used to display the resulting image
        outputImageView = (ImageView)findViewById(R.id.outputImageView);

        // open camera (default is back-facing) to set preview format
        // as we require a camera in the manifest declaration we don't need to check if a camera is available at runtime
        // to check in runtime instead use hasSystemFeature(PackageManager.FEATURE_CAMERA)
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
        // we want preview in NV21 format (which is supported by all cameras, unlike RGB)
        params.setPreviewFormat(ImageFormat.NV21);

        //auto-focus
        if (params.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        camera.setParameters(params);

        // get real preview parameters
        params = camera.getParameters();
        if(params.getPreviewFormat()!=ImageFormat.NV21)
        {
            Log.i("CameraRenderscript", "params.getPreviewFormat()!=ImageFormat.NV21 params.getPreviewFormat()="+params.getPreviewFormat());
        }
        //get preview image sizes
        imageWidth  = params.getPreviewSize().width;
        imageHeight = params.getPreviewSize().height;
        getActionBar().setSubtitle(String.format("%dx%d:XXX FPS", imageWidth, imageHeight));
        Log.i("CameraRenderscript", "getPreviewSize() " + imageWidth + "x" + imageHeight);
        camera.release();
        camera = null;

        //create bitmap for output.
        outputBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

        harris = new Harris(this, outputBitmap, Harris.CONVOLVE_5X5);

        //get preview surface for camera preview and set callback for surface
        //the layout is specified the way the inputSurfaceView is completely overlayed by outputSurfaceView
        // outputSurfaceView is the view that displays the resulting image
        // so inputSurfaceView is rather fake view for unprocessed preview frames
        // the inputSurfaceView is required just to setup the callback
        SurfaceView surView = (SurfaceView) findViewById(R.id.inputSurfaceView);
        SurfaceHolder surHolder = surView.getHolder();
        surHolder.addCallback(this);
    }

    private class ProcessData extends AsyncTask<byte[], Void, Boolean>
    {
        long    RenderScriptTime;
        @Override
        protected Boolean doInBackground(byte[]... args)
        {
            long rsStart = System.nanoTime();
            if(ApplyEffect)
            {
                if(Convolution5) {
                    harris.process(true);
                }
                else {
                    harris.process(false);
                }
                //long stepStart = System.nanoTime();
                //long stepEnd = System.nanoTime();
                //Log.i("RenderScript Camera", "RS time: "+(stepEnd-stepStart)/1000000.0f+" ms");
            }
            else
            {
                harris.stopProcess();
            }
            //long stepStart = System.nanoTime();
            harris.syncAll();
            long stepEnd = System.nanoTime();
            //Log.i("RenderScript Camera", "Copy time: "+(stepEnd-stepStart)/1000000.0f+" ms");
            RenderScriptTime = stepEnd - rsStart;

            return true;
        }
        protected void onPostExecute(Boolean result) {
            //update average render script time processing
            frameDurationAverRenderScript += (RenderScriptTime-frameDurationAverRenderScript)*blendFactor;

            //TODO understand
            outputImageView.setImageBitmap(outputBitmap);
            outputImageView.invalidate();
            RenderScriptIsWorking = false;
        }
    }
    @Override
    public void onPreviewFrame(byte[] arg0, Camera arg1)
    {
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
            double RenderScriptFPS = 1e9/frameDurationAverRenderScript;
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
        // copy input image data to the allocation for further processing in async fashion
        harris.setFrame(arg0);
        // issue an async task to process the frame, it will cause ImageView update upon completion
        // since the task is async, camera continues to produce frames
        new ProcessData().execute();

        // update last processed time stamp and processing time average
        prevFrameTimestampProcessed = curFrameTimestamp;
        frameDurationAverProcessed += (frameDurationProcessed-frameDurationAverProcessed)*blendFactor;
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
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
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();
        camera = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch(id) {
            case R.id.action_settings:
                break;
            case R.id.convolution5x5: {
                boolean isChecked = item.isChecked();
                Convolution5 = !isChecked;
                if (isChecked) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }
                break;
            }
            case R.id.detection_on: {
                boolean isChecked = item.isChecked();
                ApplyEffect = !isChecked;
                if (isChecked) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }
}

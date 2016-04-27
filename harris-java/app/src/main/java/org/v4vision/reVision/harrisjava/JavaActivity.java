package org.v4vision.reVision.harrisjava;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.io.IOException;
import java.util.List;

public class JavaActivity extends Activity  implements Camera.PreviewCallback, SurfaceHolder.Callback {

    private Camera.Parameters params;
    private Camera camera;

    // couple of variables that controls the pipeline:
    // a flag to skip outstanding frames from the camera, when there some frame is already in process
    private volatile boolean IsProcessing;
    // an on/off flag for the video effect
    private volatile boolean    ApplyEffect;

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

        outputBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

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
    public void onPreviewFrame(final byte[] data, Camera camera) {
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
        if (IsProcessing || (ApplyEffect && (frameDurationProcessed<frameDurationT)))
            return;

        // submit frame to process in background
        IsProcessing = true;

        new ProcessData().execute(new ByteArrayWrapper(data));

        // update last processed time stamp and processing time average
        prevFrameTimestampProcessed = curFrameTimestamp;
        frameDurationAverProcessed += (frameDurationProcessed-frameDurationAverProcessed)*blendFactor;
    }

    private class ProcessData extends AsyncTask<ByteArrayWrapper, Void, Boolean>
    {
        long ProcessingTime;
        @Override
        protected Boolean doInBackground(ByteArrayWrapper... args)
        {
            long startingTime = System.nanoTime();

            outputBitmap = new HarrisCornerDetector(imageWidth, imageHeight).detect(args[0].get());

            long stepEnd = System.nanoTime();
            //Log.i("RenderScript Camera", "Copy time: "+(stepEnd-stepStart)/1000000.0f+" ms");
            ProcessingTime = stepEnd - startingTime;

            return true;
        }
        protected void onPostExecute(Boolean result) {
            //update average render script time processing
            frameDurationAverJNI += (ProcessingTime -frameDurationAverJNI)*blendFactor;

            outputImageView.setImageBitmap(outputBitmap);
            outputImageView.invalidate();
            IsProcessing = false;

        }
    }
}
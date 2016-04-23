package org.v4vision.reVision.harrisjava;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.util.List;

public class JavaActivity extends Activity  implements Camera.PreviewCallback, SurfaceHolder.Callback {

    private Camera.Parameters params;
    private Camera camera;
    private Bitmap outputBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java);
        ImageView outputImageView = (ImageView)findViewById(R.id.outputImageView);

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
        params.setPreviewFormat(ImageFormat.NV21);
        //auto-focus
        if (params.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        camera.setParameters(params);
        // get real preview parameters TODO: ?
        params = camera.getParameters();
        int imageWidth  = params.getPreviewSize().width;
        int imageHeight = params.getPreviewSize().height;
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

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }
}

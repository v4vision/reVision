package org.v4vision.reVision.core.harris;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.renderscript.ScriptIntrinsicConvolve5x5;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import org.v4vision.reVision.ScriptC_harris;

public class Harris {

    private Bitmap outputBitMap;
    private RenderScript rs;
    private ScriptC_harris script;
    private Allocation allocationIn, allocationOut, allocationYUV, allocationGray, allocationConvX, allocationConvY;
    private ScriptIntrinsicYuvToRGB intrinsicYuvToRGB;
    private ScriptIntrinsicConvolve3x3 intrinsicConvolve3x3X, intrinsicConvolve3x3Y;
    private ScriptIntrinsicConvolve5x5 intrinsicConvolve5x5X, intrinsicConvolve5x5Y;
    private int convolution;
    public static final int CONVOLVE_3X3 = 1;
    public static final int CONVOLVE_5X5 = 2;
    //private ScriptIntrinsicBlur intrinsicBlur;

    public Harris(Context ctx, Bitmap outputBitMap, int convolution) {
        this.convolution = convolution;
        this.outputBitMap = outputBitMap;
        this.rs = RenderScript.create(ctx);
        this.script = new ScriptC_harris(rs);
        // Create an allocation (which is memory abstraction in the Renderscript) that corresponds to the outputBitmap
        this.allocationOut = Allocation.createFromBitmap(rs, this.outputBitMap);
        // allocationIn and allocationBlur matches the allocationOut
        this.allocationIn =  Allocation.createTyped(rs, allocationOut.getType(), Allocation.USAGE_SCRIPT);
        this.allocationConvX =  Allocation.createTyped(rs, allocationOut.getType(),Allocation.USAGE_SCRIPT);
        this.allocationConvY =  Allocation.createTyped(rs, allocationOut.getType(),Allocation.USAGE_SCRIPT);
        this.allocationGray = Allocation.createTyped(rs, allocationOut.getType(), Allocation.USAGE_SCRIPT);

        Type.Builder typeYUV = new Type.Builder(rs, Element.createPixel(rs, Element.DataType.UNSIGNED_8, Element.DataKind.PIXEL_YUV));
        typeYUV.setYuvFormat(ImageFormat.NV21);
        // allocation for the YUV input from the camera
        this.allocationYUV = Allocation.createTyped(rs, typeYUV.setX(outputBitMap.getWidth()).setY(outputBitMap.getHeight()).create(), Allocation.USAGE_SCRIPT);

        //create the instance of the YUV2RGB (built-in) RS intrinsic
        this.intrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        //this.intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        // set blur radius (blurring is important component of the Old Movie video effect)
        //this.intrinsicBlur.setRadius(outputBitMap.getWidth() / 200.0f);

//        if(convolution == CONVOLVE_3X3) {
            this.intrinsicConvolve3x3X = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
            this.intrinsicConvolve3x3X.setCoefficients(new float[]{-1, 0, 1, -2, 0, 2, -1, 0, 1});

            this.intrinsicConvolve3x3Y = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
            this.intrinsicConvolve3x3Y.setCoefficients(new float[]{-1, -2, -1, 0, 0, 0, 1, 2, 1});

            this.script.set_harrisThreshold(-0.07f);
//        }
//        else if(convolution == CONVOLVE_5X5) {
            this.intrinsicConvolve5x5X = ScriptIntrinsicConvolve5x5.create(rs, Element.U8_4(rs));
            this.intrinsicConvolve5x5X.setCoefficients(new float[]{-1, -2, 0, 2, 1, -1, -2, 0, 2, 1, -1, -2, 0, 2, 1, -1, -2, 0, 2, 1, -1, -2, 0, 2, 1});

            this.intrinsicConvolve5x5Y = ScriptIntrinsicConvolve5x5.create(rs, Element.U8_4(rs));
            this.intrinsicConvolve5x5Y.setCoefficients(new float[]{-1, -1, -1, -1, -1, -2, -2, -2, -2, -2, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1});
            this.script.set_harrisThreshold(-0.14f);
//        }
//        else throw new IllegalArgumentException("Wrong convolution value");
    }

    public void setFrame(byte[] frame) {
        this.allocationYUV.copyFrom(frame);
    }

    //TODO: fix
    public void process(boolean isConvolution5x5) {
        intrinsicYuvToRGB.setInput(allocationYUV);
        intrinsicYuvToRGB.forEach(allocationIn);

        script.forEach_grayscale(allocationIn, allocationGray);
        //intrinsicBlur.setInput(allocationGray);
        //intrinsicBlur.forEach(allocationGray);

        if(isConvolution5x5) {
            intrinsicConvolve3x3X.setInput(allocationGray);
            intrinsicConvolve3x3X.forEach(allocationConvX);

            intrinsicConvolve3x3Y.setInput(allocationGray);
            intrinsicConvolve3x3Y.forEach(allocationConvY);
        }
        else {
            intrinsicConvolve5x5X.setInput(allocationGray);
            intrinsicConvolve5x5X.forEach(allocationConvX);

            intrinsicConvolve5x5Y.setInput(allocationGray);
            intrinsicConvolve5x5Y.forEach(allocationConvY);
        }

        /* intrinsicBlur.setInput(allocationConvY);
        intrinsicBlur.forEach(allocationConvY);

        intrinsicBlur.setInput(allocationConvX);
        intrinsicBlur.forEach(allocationConvX);*/

        script.invoke_initConvX(allocationConvX);
        script.invoke_initConvY(allocationConvY);

        script.forEach_harris(allocationIn, allocationOut);

        rs.finish();
    }

    public void stopProcess() {
        intrinsicYuvToRGB.setInput(allocationYUV);
        intrinsicYuvToRGB.forEach(allocationOut);
    }

    public void syncAll() {
        allocationOut.syncAll(Allocation.USAGE_SHARED);
    }
}

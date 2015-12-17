package org.v4vision.reVision.core.harris;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import org.v4vision.reVision.ScriptC_process;

public class Harris {

    private Bitmap outputBitMap;
    private RenderScript rs;
    private ScriptC_process script;
    private Allocation allocationIn, allocationOut, allocationYUV, allocationGray, allocationConvX, allocationConvY;
    private ScriptIntrinsicYuvToRGB intrinsicYuvToRGB;
    private ScriptIntrinsicConvolve3x3 intrinsicConvolveX, intrinsicConvolveY;
    //private ScriptIntrinsicBlur intrinsicBlur;

    public Harris(Bitmap outputBitMap, Context ctx) {
        this.outputBitMap = outputBitMap;
        this.rs = RenderScript.create(ctx);
        this.script = new ScriptC_process(rs);
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
        //this.intrinsicBlur.setRadius(imageWidth / 400.0f);

        this.intrinsicConvolveX = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        this.intrinsicConvolveX.setCoefficients(new float[]{-1,0,1,-1,0,1,-1,0,1});

        this.intrinsicConvolveY = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs));
        this.intrinsicConvolveY.setCoefficients(new float[]{-1,-1,-1,0,0,0,1,1,1});
    }

    public void setFrame(byte[] frame) {
        this.allocationYUV.copyFrom(frame);
    }

    public void process() {
        intrinsicYuvToRGB.setInput(allocationYUV);
        intrinsicYuvToRGB.forEach(allocationIn);

        script.forEach_grayscale(allocationIn, allocationGray);
        //intrinsicBlur.setInput(allocationGray);
        //intrinsicBlur.forEach(allocationGray);

        intrinsicConvolveX.setInput(allocationGray);
        intrinsicConvolveX.forEach(allocationConvX);

        intrinsicConvolveY.setInput(allocationGray);
        intrinsicConvolveY.forEach(allocationConvY);

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

/*
 * Copyright (C) 2011-2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is auto-generated. DO NOT MODIFY!
 * The source Renderscript file: /Users/ulasakdeniz/Documents/reVision/app/src/main/rs/harris.rs
 */
package org.v4vision.reVision;

import android.renderscript.*;
import android.content.res.Resources;

/**
 * @hide
 */
public class ScriptC_harris extends ScriptC {
    private static final String __rs_resource_name = "harris";
    // Constructor
    public  ScriptC_harris(RenderScript rs) {
        this(rs,
             rs.getApplicationContext().getResources(),
             rs.getApplicationContext().getResources().getIdentifier(
                 __rs_resource_name, "raw",
                 rs.getApplicationContext().getPackageName()));
    }

    public  ScriptC_harris(RenderScript rs, Resources resources, int id) {
        super(rs, resources, id);
        __U8_4 = Element.U8_4(rs);
        __I32 = Element.I32(rs);
        mExportVar_harrisThreshold = -0.16f;
        __F32 = Element.F32(rs);
        __ALLOCATION = Element.ALLOCATION(rs);
    }

    private Element __ALLOCATION;
    private Element __F32;
    private Element __I32;
    private Element __U8_4;
    private FieldPacker __rs_fp_ALLOCATION;
    private FieldPacker __rs_fp_F32;
    private FieldPacker __rs_fp_I32;
    private FieldPacker __rs_fp_U8_4;
    private final static int mExportVarIdx_cornerColorRGB = 0;
    private Short4 mExportVar_cornerColorRGB;
    public synchronized void set_cornerColorRGB(Short4 v) {
        mExportVar_cornerColorRGB = v;
        FieldPacker fp = new FieldPacker(4);
        fp.addU8(v);
        int []__dimArr = new int[1];
        __dimArr[0] = 1;
        setVar(mExportVarIdx_cornerColorRGB, fp, __U8_4, __dimArr);
    }

    public Short4 get_cornerColorRGB() {
        return mExportVar_cornerColorRGB;
    }

    public Script.FieldID getFieldID_cornerColorRGB() {
        return createFieldID(mExportVarIdx_cornerColorRGB, null);
    }

    private final static int mExportVarIdx_i = 1;
    private int mExportVar_i;
    public synchronized void set_i(int v) {
        setVar(mExportVarIdx_i, v);
        mExportVar_i = v;
    }

    public int get_i() {
        return mExportVar_i;
    }

    public Script.FieldID getFieldID_i() {
        return createFieldID(mExportVarIdx_i, null);
    }

    private final static int mExportVarIdx_j = 2;
    private int mExportVar_j;
    public synchronized void set_j(int v) {
        setVar(mExportVarIdx_j, v);
        mExportVar_j = v;
    }

    public int get_j() {
        return mExportVar_j;
    }

    public Script.FieldID getFieldID_j() {
        return createFieldID(mExportVarIdx_j, null);
    }

    private final static int mExportVarIdx_harrisThreshold = 3;
    private float mExportVar_harrisThreshold;
    public synchronized void set_harrisThreshold(float v) {
        setVar(mExportVarIdx_harrisThreshold, v);
        mExportVar_harrisThreshold = v;
    }

    public float get_harrisThreshold() {
        return mExportVar_harrisThreshold;
    }

    public Script.FieldID getFieldID_harrisThreshold() {
        return createFieldID(mExportVarIdx_harrisThreshold, null);
    }

    private final static int mExportVarIdx_convX = 4;
    private Allocation mExportVar_convX;
    public synchronized void set_convX(Allocation v) {
        setVar(mExportVarIdx_convX, v);
        mExportVar_convX = v;
    }

    public Allocation get_convX() {
        return mExportVar_convX;
    }

    public Script.FieldID getFieldID_convX() {
        return createFieldID(mExportVarIdx_convX, null);
    }

    private final static int mExportVarIdx_convY = 5;
    private Allocation mExportVar_convY;
    public synchronized void set_convY(Allocation v) {
        setVar(mExportVarIdx_convY, v);
        mExportVar_convY = v;
    }

    public Allocation get_convY() {
        return mExportVar_convY;
    }

    public Script.FieldID getFieldID_convY() {
        return createFieldID(mExportVarIdx_convY, null);
    }

    //private final static int mExportForEachIdx_root = 0;
    private final static int mExportForEachIdx_grayscale = 1;
    public Script.KernelID getKernelID_grayscale() {
        return createKernelID(mExportForEachIdx_grayscale, 3, null, null);
    }

    public void forEach_grayscale(Allocation ain, Allocation aout) {
        forEach_grayscale(ain, aout, null);
    }

    public void forEach_grayscale(Allocation ain, Allocation aout, Script.LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        // check aout
        if (!aout.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        // Verify dimensions
        Type tIn = ain.getType();
        Type tOut = aout.getType();
        if ((tIn.getCount() != tOut.getCount()) ||
            (tIn.getX() != tOut.getX()) ||
            (tIn.getY() != tOut.getY()) ||
            (tIn.getZ() != tOut.getZ()) ||
            (tIn.hasFaces() != tOut.hasFaces()) ||
            (tIn.hasMipmaps() != tOut.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between input and output parameters!");
        }
        forEach(mExportForEachIdx_grayscale, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_harris = 2;
    public Script.KernelID getKernelID_harris() {
        return createKernelID(mExportForEachIdx_harris, 3, null, null);
    }

    public void forEach_harris(Allocation ain, Allocation aout) {
        forEach_harris(ain, aout, null);
    }

    public void forEach_harris(Allocation ain, Allocation aout, Script.LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        // check aout
        if (!aout.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        // Verify dimensions
        Type tIn = ain.getType();
        Type tOut = aout.getType();
        if ((tIn.getCount() != tOut.getCount()) ||
            (tIn.getX() != tOut.getX()) ||
            (tIn.getY() != tOut.getY()) ||
            (tIn.getZ() != tOut.getZ()) ||
            (tIn.hasFaces() != tOut.hasFaces()) ||
            (tIn.hasMipmaps() != tOut.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between input and output parameters!");
        }
        forEach(mExportForEachIdx_harris, ain, aout, null, sc);
    }

    private final static int mExportFuncIdx_initConvX = 0;
    public void invoke_initConvX(Allocation inx) {
        FieldPacker initConvX_fp = new FieldPacker(4);
        initConvX_fp.addObj(inx);
        invoke(mExportFuncIdx_initConvX, initConvX_fp);
    }

    private final static int mExportFuncIdx_initConvY = 1;
    public void invoke_initConvY(Allocation iny) {
        FieldPacker initConvY_fp = new FieldPacker(4);
        initConvY_fp.addObj(iny);
        invoke(mExportFuncIdx_initConvY, initConvY_fp);
    }

}


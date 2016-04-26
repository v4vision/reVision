#include <jni.h>

void fillWithZeros(jfloat *, jint);
void fillWithZerosInt(jint *, jint);
float gaussianFilter(float *, int , int);
float cornerResponseFunction(float *, float *, float *, int index);

JNIEXPORT jintArray JNICALL
Java_org_v4vision_reVision_harrisjava_JavaActivity_grayscale(JNIEnv *env, jobject instance,
                                                             jbyteArray frame_, jint imageWidth,
                                                             jint imageHeight) {
    jbyte *frame = (*env)->GetByteArrayElements(env, frame_, 0);
    /**
     * Inialization for the number of pixels.
     */
    jint arraysize = imageWidth * imageHeight;

    /**
     * Decleration of the arrays which will be used to calculate the required values.
     */
    jfloatArray convolutionX_ = (*env)->NewFloatArray(env,arraysize);
    jfloatArray convolutionY_ = (*env)->NewFloatArray(env,arraysize);
    jfloatArray Ixx_ = (*env)->NewFloatArray(env,arraysize);
    jfloatArray Iyy_ = (*env)->NewFloatArray(env,arraysize);
    jfloatArray Ixy_ = (*env)->NewFloatArray(env,arraysize);
    jfloatArray GIxx_ = (*env)->NewFloatArray(env,arraysize);
    jfloatArray GIyy_ = (*env)->NewFloatArray(env,arraysize);
    jfloatArray GIxy_ = (*env)->NewFloatArray(env,arraysize);
    jfloatArray cornerResponse_ = (*env)->NewFloatArray(env,arraysize);

    jintArray corners_ = (*env)->NewIntArray(env,arraysize);

    /**
     * Getting elements of arrays to iterate on.
     */
    jfloat *convolutionX = (*env)->GetFloatArrayElements(env,convolutionX_,0);
    jfloat *convolutionY = (*env)->GetFloatArrayElements(env,convolutionY_,0);
    jfloat *Ixx = (*env)->GetFloatArrayElements(env,Ixx_,0);
    jfloat *Iyy = (*env)->GetFloatArrayElements(env,Iyy_,0);
    jfloat *Ixy = (*env)->GetFloatArrayElements(env,Ixy_,0);
    jfloat *GIxx = (*env)->GetFloatArrayElements(env,GIxx_,0);
    jfloat *GIyy = (*env)->GetFloatArrayElements(env,GIyy_,0);
    jfloat *GIxy = (*env)->GetFloatArrayElements(env,GIxy_,0);
    jfloat *cornerResponse = (*env)->GetFloatArrayElements(env,cornerResponse_,0);

    jint *corners = (*env)->GetIntArrayElements(env,corners_,0);

    fillWithZeros(convolutionX,arraysize);
    fillWithZeros(convolutionY,arraysize);
    fillWithZeros(Ixx,arraysize);
    fillWithZeros(Iyy,arraysize);
    fillWithZeros(Ixy,arraysize);
    fillWithZeros(GIxx,arraysize);
    fillWithZeros(GIyy,arraysize);
    fillWithZeros(GIxy,arraysize);
    fillWithZeros(cornerResponse,arraysize);
    fillWithZerosInt(corners,arraysize);

    int height, width;

    for(height = 1; height < imageHeight; height++) {
        int index1 = imageWidth * height;
        for(width = 1; width < imageWidth; width++) {
            int index = index1 + width;
            if((index % imageWidth < imageWidth - 1) && (index / imageWidth < imageHeight - 1)) {
                /**
                 * Calculation of convolutionX matrix.
                 */
                convolutionX[index] =
                        frame[index - imageWidth + 1] - frame[index - imageWidth - 1] +
                        frame[index + 1] - frame[index - 1] +
                        frame[index + imageWidth + 1] - frame[index + imageWidth - 1];
                /**
                 * Calculation of convolutionY matrix.
                 */
                convolutionY[index] =
                        frame[index + imageWidth + 1] - frame[index - imageWidth + 1] +
                        frame[index + imageWidth] - frame[index - imageWidth] +
                        frame[index + imageWidth - 1] - frame[index - imageWidth - 1];

                /**
                 * Calculations of Ixx,Iyy and Ixy matrices.
                 */
                Ixx[index] = convolutionX[index] * convolutionX[index];
                Iyy[index] = convolutionY[index] * convolutionY[index];
                Ixy[index] = convolutionX[index] * convolutionY[index];
            }

            if((index / imageWidth < imageHeight - 2) &&
               (index % imageWidth < imageWidth - 2) &&
               (index % imageWidth > 1) &&
               (index / imageWidth > 1)) {
                GIxx[index] = gaussianFilter(Ixx, index, imageWidth);
                GIyy[index] = gaussianFilter(Iyy, index, imageWidth);
                GIxy[index] = gaussianFilter(Ixy, index, imageWidth);

                cornerResponse[index] = cornerResponseFunction(GIxx, GIyy, GIxy, index);
            }
        }
    }

    int y,x;

    for(height = 5; height < imageHeight; height++) {
        int index1 = imageWidth * height;
        for (width = 5; width < imageWidth; width++) {
            int index = index1 + width;
            if((index % imageWidth < imageWidth - 5) && (index / imageWidth < imageHeight - 5)) {
                float tmp = cornerResponse[index];
                for(y = -5; y < 6 && tmp != 0; y++) {
                    for(x = -5; x < 6; x++) {
                        if(cornerResponse[index] < cornerResponse[index + y * imageWidth + x]) {
                            //TODO: check if these two lines do the same thing!
                            cornerResponse[index] = 0;
                            tmp = 0;
                            break;
                        }
                        else {
                            corners[index] = 1;
                            //TODO: This data structure would be dynamic.
                        }
                    }
                }
            }
        }
    }

    // TODO

    (*env)->ReleaseByteArrayElements(env, frame_, frame, 0);
    (*env)->ReleaseFloatArrayElements(env, convolutionX_, convolutionX, 0);
    (*env)->ReleaseFloatArrayElements(env, convolutionY_, convolutionY, 0);
    (*env)->ReleaseFloatArrayElements(env, Ixx_, Ixx, 0);
    (*env)->ReleaseFloatArrayElements(env, Iyy_, Iyy, 0);
    (*env)->ReleaseFloatArrayElements(env, Ixy_, Ixy, 0);
    (*env)->ReleaseFloatArrayElements(env, GIxx_, GIxx, 0);
    (*env)->ReleaseFloatArrayElements(env, GIyy_, GIyy, 0);
    (*env)->ReleaseFloatArrayElements(env, GIxy_, GIxy, 0);
    (*env)->ReleaseFloatArrayElements(env, cornerResponse_, cornerResponse, 0);
    //TODO: Do this line for all arrays.
    return corners_;
}

/**
 * Fill arrays with '0' for initialization.
 */
void fillWithZeros(jfloat *arr, jint size) {
    int i = 0;
    for(i=0; i<size; i++) {
        arr[i] = 0;
    }
}

void fillWithZerosInt(jint *arr, jint size) {
    int i = 0;
    for(i=0; i<size; i++) {
        arr[i] = 0;
    }
}

float gaussianFilter(float *A, int index, int imageWidth) {
    float gaussianKernel[] = {
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

float cornerResponseFunction(float *GIxx, float *GIyy, float *GIxy, int index) {
    float k = 0.04f;
    float threshold = 300000000;
    float response = (GIxx[index] * GIyy[index] - GIxy[index] * GIxy[index]) -
                     k * (GIxx[index] + GIyy[index]) * (GIxx[index] + GIyy[index]);
    return (response < -threshold) || (response > threshold) ? response : 0;
}

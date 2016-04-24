#pragma version(1)
#pragma rs java_package_name(org.v4vision.reVision)
#pragma rs_fp_relaxed

const static float3 gMonoMult = {0.2126f, 0.7152f, 0.0722f};

uchar4 cornerColorRGB = {255, 0, 0, 10};
int i,j;
float c = 0.04;

float harrisThreshold = 3000000000;

rs_allocation convX, convY, covImg, allIxx, allIyy, allIxy;

void initConvX(rs_allocation inx) {
    convX = inx;
}

void initConvY(rs_allocation iny) {
    convY = iny;
}

void initCov(rs_allocation in) {
    covImg = in;
}

void initIxx(rs_allocation in) {
    allIxx = in;
}

void initIyy(rs_allocation in) {
    allIyy = in;
}

void initIxy(rs_allocation in) {
    allIxy = in;
}


uchar4 __attribute__((kernel)) grayscale(const uchar4 in, uint32_t x, uint32_t y)
{
    uchar mono = (uchar)(in.r * gMonoMult[0] + in.g * gMonoMult[1] + in.b * gMonoMult[2]);
    uchar4 ret = {mono, mono, mono, in.a};
    return ret;
}

float __attribute__((kernel)) covIxx(const float in, uint32_t x, uint32_t y)
{
    uchar4 convXpixel = rsGetElementAt_uchar4(convX, x, y);

    uchar Ix = convXpixel.r; // * gMonoMult[0] + convXpixel.g * gMonoMult[1] + convXpixel.b * gMonoMult[2];
    float Ixx = Ix * Ix;
    return Ixx;
}

float __attribute__((kernel)) covIyy(const float in, uint32_t x, uint32_t y)
{
    uchar4 convYpixel = rsGetElementAt_uchar4(convY, x, y);

    uchar Iy = convYpixel.r; // * gMonoMult[0] + convYpixel.g * gMonoMult[1] + convYpixel.b * gMonoMult[2];
    float Iyy = Iy * Iy;
    return Iyy;
}

float __attribute__((kernel)) covIxy(const float in, uint32_t x, uint32_t y)
{
    uchar4 convXpixel = rsGetElementAt_uchar4(convX, x, y);
    uchar4 convYpixel = rsGetElementAt_uchar4(convY, x, y);

    uchar Ix = convXpixel.r; // * gMonoMult[0] + convXpixel.g * gMonoMult[1] + convXpixel.b * gMonoMult[2];
    uchar Iy = convYpixel.r; // * gMonoMult[0] + convYpixel.g * gMonoMult[1] + convYpixel.b * gMonoMult[2];
    float Ixy = Ix * Iy;
    return Ixy;
}

uchar4 __attribute__((kernel)) harris(const uchar4 in, uint32_t x, uint32_t y)
{
    float Ixx = rsGetElementAt_float(allIxx, x, y);
    float Iyy = rsGetElementAt_float(allIyy, x, y);
    float Ixy = rsGetElementAt_float(allIxy, x, y);

    //float l1 = 0.5f * ( Ixx + Iyy - sqrt((Ixx - Iyy) * (Ixx - Iyy) + 4 * Ixy * Ixy ));
    //float l2 = 0.5f * ( Ixx + Iyy + sqrt((Ixx - Iyy) * (Ixx - Iyy) + 4 * Ixy * Ixy ));

    //float cornerResponse = l1 * l2 - c * (l1 + l2) * (l1 + l2);
    float cornerResponse = (Ixx*Iyy - Ixy*Ixy - c*(Ixx+Iyy)*(Ixx+Iyy));
    if(cornerResponse < -harrisThreshold) { //|| cornerResponse > harrisThreshold) {
    //    uchar4 ret = {cornerResponse, cornerResponse, cornerResponse, 100};
    //    return ret;
    //rsDebug("aa", cornerResponse);
    //return (uchar4){cornerResponse, cornerResponse, cornerResponse, 100};
        return cornerColorRGB;
    }

    return in;
}
#pragma version(1)
#pragma rs java_package_name(org.v4vision.reVision)
#pragma rs_fp_relaxed


const static float3 gMonoMult = {0.299f, 0.587f, 0.114f};

uchar4 cornerColorRGB;
int i,j;

rs_allocation convX, convY;

void initConvX(rs_allocation inx) {
  convX = inx;
}

void initConvY(rs_allocation iny) {
  convY = iny;
}

uchar4 __attribute__((kernel)) grayscale(const uchar4 in, uint32_t x, uint32_t y)
{
    float mono = in.r /* * gMonoMult[0] */ + in.g /* * gMonoMult[1] */ + in.b /* * gMonoMult[2] */;
    return mono;
}

uchar4 __attribute__((kernel)) harris(const uchar4 in, uint32_t x, uint32_t y)
{
    float c = 0.04;

    float4 convXpixel = rsUnpackColor8888(rsGetElementAt_uchar4(convX, x, y));
    float4 convYpixel = rsUnpackColor8888(rsGetElementAt_uchar4(convY, x, y));

    float Ix = convXpixel.r + convXpixel.g + convXpixel.b;
    float Iy = convYpixel.r + convYpixel.g + convYpixel.b;
    float Ixx = Ix * Ix;
    float Iyy = Iy * Iy;
    float Ixy = Ix * Iy;

    float cornerResponse = (Ixx*Iyy - Ixy*Ixy - c*(Ixx+Iyy)*(Ixx+Iyy));
    if(cornerResponse < -4.5) {
        cornerColorRGB.r = 0;
        cornerColorRGB.g = 255;
        cornerColorRGB.b = 0;
        return cornerColorRGB;
    } else {
        return in;
    }
}

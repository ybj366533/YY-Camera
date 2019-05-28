#ifndef FEATURE_DESCRIPTOR_H_
#define FEATURE_DESCRIPTOR_H_


extern "C" {
    #include "hog.h" // From the VLFeat C library
}

#include <vector>
#include <string.h>
#include <assert.h>
#include <stdlib.h>
#include "matrix.h"

struct HoGParam
{
    VlHogVariant vlhog_variant;
    int num_cells;
    int cell_size;
    int num_bins;
    float relative_patch_size; // the patch size we'd like in percent of the IED of the current image
    // note: alternatively, we could dynamically vary cell_size. Guess it works if the hog features are somehow normalised.
};


cv2::Matrix2d<float> CalculateHogDescriptor(const cv2::Matrix2d<uint8_t> &src, cv2::Matrix2d<float> shape, std::vector<int> LandmarkIndexs, std::vector<int> eyes_index, HoGParam mHoGParam);



#endif

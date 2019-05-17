
#include "feature_descriptor.h"
#include <sys/time.h>

static int64_t zzz_system_current_milli()
{
    int64_t milli = 0;
    
    struct timeval    tp;
    
    gettimeofday(&tp, NULL);
    
    //milli = tp.tv_sec * 1000 + tp.tv_usec/1000;
    milli = tp.tv_sec * (int64_t)1000000 + tp.tv_usec;
    
    return milli;
}

cv2::Matrix2d<float> CalculateHogDescriptor(const cv2::Matrix2d<uint8_t> &src, cv2::Matrix2d<float> shape, std::vector<int> LandmarkIndexs, std::vector<int> eyes_index, HoGParam mHoGParam) {
	assert(shape.rows() == 1 && eyes_index.size() == 4);
    
    int64_t CalculateHogDescriptor_perfs = zzz_system_current_milli();
	
	cv2::Matrix2d<uint8_t> grayImage = src;
	
	int numLandmarks = shape.cols() / 2;
	
	float lx = (shape(0, eyes_index.at(0)) + shape(0, eyes_index.at(1)))*0.5;
	float ly = (shape(0, eyes_index.at(0) + numLandmarks) + shape(0, eyes_index.at(1) + numLandmarks))*0.5;
	float rx = (shape(0, eyes_index.at(2)) + shape(0, eyes_index.at(3)))*0.5;
	float ry = (shape(0, eyes_index.at(2) + numLandmarks) + shape(0, eyes_index.at(3) + numLandmarks))*0.5;
	float distance = sqrt((rx - lx)*(rx - lx) + (ry - ly)*(ry - ly));
	int patch_width_half = round(mHoGParam.relative_patch_size * distance);
	
	cv2::Matrix2d<float> hogDescriptors;
	int colsSize = (1 << mHoGParam.num_bins) * mHoGParam.num_cells * mHoGParam.num_cells * LandmarkIndexs.size() + 1;
	hogDescriptors.create(1, colsSize);				// 1* 3073 / 5121
	hogDescriptors(0, colsSize - 1) = 1;		// add a bias row (affine part)  第一列 固定为1
    
    cv2::Matrix2d<uint8_t> roiImg;
    roiImg.create(patch_width_half * 2, patch_width_half * 2);
	
	// 抽取hog特征用的图片
	int fixed_roi_size = mHoGParam.num_cells * mHoGParam.cell_size;
	cv2::Matrix2d<float> roiImgF;
	roiImgF.create(fixed_roi_size, fixed_roi_size);

	VlHog* hog = vl_hog_new(VlHogVariant::VlHogVariantUoctti, mHoGParam.num_bins, false); // transposed (=col-major) = false
    
	for (int i = 0; i < LandmarkIndexs.size(); i++) {
		
		int x = round(shape(0, LandmarkIndexs.at(i)));
		int y = round(shape(0, LandmarkIndexs.at(i) + numLandmarks));
		
		if (x - patch_width_half < 0 || y - patch_width_half < 0 || x + patch_width_half >= grayImage.cols() || y + patch_width_half >= grayImage.rows()) {
            
            // 如果特征点靠近边缘，抽取特征所需要的区域超出图片，则只拷贝图片内部分，其他部分在cloneFrom内部主动填充0
            int x1 = (x - patch_width_half) < 0 ? 0 : (x - patch_width_half);
            int y1 = (y - patch_width_half) < 0 ? 0 : (y - patch_width_half);
            int x2 = (x + patch_width_half) >= grayImage.cols() ? grayImage.cols() : (x + patch_width_half);
            int y2 = (y + patch_width_half) >= grayImage.rows() ? grayImage.rows() : (y + patch_width_half);
            
            int borderLeft = (x - patch_width_half) < 0 ? std::abs(x - patch_width_half) : 0; // x and y are patch-centers
            int borderTop = (y - patch_width_half) < 0 ? std::abs(y - patch_width_half) : 0;
            roiImg.cloneFrom(grayImage, x1, y1, (x2 - x1)<0?0:(x2 - x1), (y2 - y1)<0?0:(y2 - y1), borderLeft, borderTop);
		}
		else {
			cv2::Rect roi(x - patch_width_half, y - patch_width_half, patch_width_half * 2, patch_width_half * 2); // x y w h. Rect: x and y are top-left corner. Our x and y are center. Convert.
																												   //roiImg = grayImage(roi).clone(); // clone because we need a continuous memory block
            
            roiImg.cloneFrom(grayImage, roi.x, roi.y, roi.width, roi.height);
			
		}
		
		// 为了减轻内存的分配次数（前提是每次矩阵大小是一样的）
		//cv2::Matrix2d<float> roiImgF = roiImg.resize_and_convert(fixed_roi_size, fixed_roi_size);
		roiImg.resize_and_convert(fixed_roi_size, fixed_roi_size, roiImgF.getData());
		
		vl_hog_put_image(hog, (float*)roiImgF.getData(), roiImgF.cols(), roiImgF.rows(), 1, mHoGParam.cell_size); // (the '1' is numChannels)
		int ww = static_cast<int>(vl_hog_get_width(hog)); // assert ww == hh == numCells
		int hh = static_cast<int>(vl_hog_get_height(hog));
		int dd = static_cast<int>(vl_hog_get_dimension(hog)); // assert ww=hogDim1, hh=hogDim2, dd=hogDim3
															  //cv::Mat hogArray(1, ww*hh*dd, CV_32FC1); // safer & same result. Don't use C-style memory management.
//printf("i:%d %d other cost %lld\n", i, __LINE__, zzz_system_current_milli()-CalculateHogDescriptor_perfs);
		cv2::Matrix2d<float> hogArray;
		hogArray.create(1, ww*hh*dd);
		vl_hog_extract(hog, hogArray.getData());
		int startPos = hh*ww*dd*i;
		float *hogDescriptors_data = hogDescriptors.getData();
//printf("i:%d %d other cost %lld\n", i, __LINE__, zzz_system_current_milli()-CalculateHogDescriptor_perfs);
		
		// todo hog内部计算的结果能不能放在连续的内存，减轻这边的性能
		for (int j = 0; j < dd; ++j) {
			float *dataTo = hogDescriptors_data + hh*ww*dd*i + hh*ww*j;
			float *dataFrom = hogArray.getData() +  ww*hh*j;

			for (int p = 0; p < hh; ++p) {
				for (int q = 0; q < ww; ++q) {
					dataTo[hh*q + p] = dataFrom[ww*p + q];
				}
			}
		}
//        printf("i:%d clone cost %lld\n", i, zzz_system_current_milli()-s);
	}
	
	vl_hog_delete(hog);
	
	return hogDescriptors;
}


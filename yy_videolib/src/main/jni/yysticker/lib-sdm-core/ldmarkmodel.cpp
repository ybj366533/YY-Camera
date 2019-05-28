#include "plat_log.h"
#include "ldmarkmodel.h"
using namespace cv2;

#include <sys/time.h>

//static int64_t eee_system_current_milli()
//{
//    int64_t milli = 0;
//
//    struct timeval    tp;
//
//    gettimeofday(&tp, NULL);
//
//    //milli = tp.tv_sec * 1000 + tp.tv_usec/1000;
//    milli = tp.tv_sec * (int64_t)1000 + tp.tv_usec/1000;
//
//    return milli;
//}

LinearRegressor::LinearRegressor() : weights()
{
//    LOGE("LinearRegressor is created\n");
}

cv2::Matrix2d<float> LinearRegressor::predict(cv2::Matrix2d<float> values)
{
//LOGE("LinearRegressor is predict\n");
	//assert(values.cols == this->weights.rows);
//    int64_t perf_s = eee_system_current_milli();
    
    cv2::Matrix2d<float> a = values*this->weights;
    //return  values*this->weights;
//    LOGE("predict %d %lld \n", __LINE__, eee_system_current_milli()-perf_s);
    
    return a;
}

void mydeserialize(LinearRegressor& item, std::istream& in)
{
//    LOGE("mydeserialize is called %d\n", __LINE__);
	//cv::Mat eigenvectors; 24* 136 或者 40 * 136
	cv2::Matrix2d<float> eigenvectors;
	{

		int rows, cols, type;
		bool continuous;

		in.read((char*)&rows, sizeof(int));
		in.read((char*)&cols, sizeof(int));
		in.read((char*)&type, sizeof(int));
		in.read((char*)&continuous, sizeof(bool));

		eigenvectors.create(rows, cols);
		// for (int i = 0; i < rows; ++i) {
			// for (int j = 0; j < cols; ++j) {
				// float value;
				// in.read((char*)&value, sizeof(float));
				// //float *data_ptr = (float*)(eigenvectors.ptr(i, j));
				// //*data_ptr = value;
				// eigenvectors(i, j) =value;
			// }
		// }
		in.read((char*)eigenvectors.getData(), sizeof(float)*rows*cols);
	}

	//cv::Mat meanvalue; 1* 136
	cv2::Matrix2d<float> meanvalue;
	{

		int rows, cols, type;
		bool continuous;

		in.read((char*)&rows, sizeof(int));
		in.read((char*)&cols, sizeof(int));
		in.read((char*)&type, sizeof(int));
		in.read((char*)&continuous, sizeof(bool));

		meanvalue.create(rows, cols);
		// for (int i = 0; i < rows; ++i) {
			// for (int j = 0; j < cols; ++j) {
				// float value;
				// in.read((char*)&value, sizeof(float));
				// //float *data_ptr = (float*)(meanvalue.ptr(i, j));
				// //*data_ptr = value;
				// meanvalue(i, j) = value;
			// }
		// }
		in.read((char*)meanvalue.getData(), sizeof(float)*rows*cols);
	}

	//cv::Mat x;	3073*24 或者 5121*40
	cv2::Matrix2d<float> x;
	{

		int rows, cols, type;
		bool continuous;

		in.read((char*)&rows, sizeof(int));
		in.read((char*)&cols, sizeof(int));
		in.read((char*)&type, sizeof(int));
		in.read((char*)&continuous, sizeof(bool));

		x.create(rows, cols);
		// for (int i = 0; i < rows; ++i) {
			// for (int j = 0; j < cols; ++j) {
				// float value;
				// in.read((char*)&value, sizeof(float));
				// //float *data_ptr = (float*)(x.ptr(i, j));
				// //*data_ptr = value;
				// x(i, j) = value;
			// }
		// }
		in.read((char*)x.getData(), sizeof(float)*rows*cols);
	}

	//通过上面计算weight
	item.weights = x*eigenvectors;
	for (int i = 0; i<item.weights.rows(); i++) {
		//item.weights.row(i) = item.weights.row(i) + meanvalue;
		for (int j = 0; j < item.weights.cols(); j++) {
			item.weights(i, j) += meanvalue(0, j);
		}
	}
}

ldmarkmodel::ldmarkmodel(){
	
}


int ldmarkmodel::track(const cv2::Matrix2d<uint8_t>& grayImage, cv2::Matrix2d<float>& current_shape, bool newFacePos, cv2::Rect facePosBox){
    //cv::Mat grayImage;
    //if(src.channels() == 1){
    //    grayImage = src;
    //}else if(src.channels() == 3){
    //    cv::cvtColor(src, grayImage, CV_BGR2GRAY);
    //}else if(src.channels() == 4){
    //    cv::cvtColor(src, grayImage, CV_RGBA2GRAY);
    //}else{
    //    return SDM_ERROR_IMAGE;
    //}

	//std::cout << src.type() << std::endl;
	//std::cout << grayImage.type() << std::endl;
//    int64_t perf_s = eee_system_current_milli();
//    LOGE("ldmarkmodel::track %d %d \n", __LINE__, eee_system_current_milli()-perf_s);
    if(!current_shape.empty()){
        faceBox = get_enclosing_bbox(current_shape);
    }else{
        faceBox = cv2::Rect(0,0,0,0);
    }
    int error_code = SDM_NO_ERROR;
    cv2::Rect mfaceBox = faceBox & cv2::Rect(0, 0, grayImage.cols(), grayImage.rows());
    float ratio = ((float)faceBox.width)/faceBox.height;
//LOGE("ldmarkmodel::track %d %d \n", __LINE__, eee_system_current_milli()-perf_s);
//    if(isDetFace || faceBox.area()<10000 || ratio>1.45f || ratio<0.8f || ((float)mfaceBox.area())/faceBox.area()<0.85f){
//        std::vector<cv::Rect> mFaceRects;
//        face_cascade.detectMultiScale(grayImage, mFaceRects, 1.3, 3, 0, cv::Size(100, 100));
//        if(mFaceRects.size() <=0){
//            current_shape = cv::Mat();
//            return SDM_ERROR_FACENO;
//        }
//        faceBox = mFaceRects[0];
//        for(int i=1; i<mFaceRects.size(); i++){
//            if(faceBox.area() < mFaceRects[i].area())
//                faceBox = mFaceRects[i];
//        }
//        error_code = SDM_ERROR_FACEDET;
//    }
  //  if(isDetFace || faceBox.area()<100){
  //      //std::vector<cv::Rect> mFaceRects;
  //      //face_cascade.detectMultiScale(grayImage, mFaceRects, 1.3, 3, 0, cv::Size(100, 100));
  //      //if(mFaceRects.size() <=0){
  //      //    current_shape = cv::Mat();
  //      //    return SDM_ERROR_FACENO;
  //      //}
  //      //faceBox = mFaceRects[0];
  //      //for(int i=1; i<mFaceRects.size(); i++){
  //      //    if(faceBox.area() < mFaceRects[i].area())
  //      //        faceBox = mFaceRects[i];
  //      //}
  //      //error_code = SDM_ERROR_FACEDET;
		////todo 固定写死
  //  }

	if (newFacePos) {
		faceBox = facePosBox;
	}
    // float scaling_x=1.0f, float scaling_y=1.0f, float translation_x=0.0f, float translation_y=0.0f
//LOGE("ldmarkmodel::track %d %d \n", __LINE__, eee_system_current_milli()-perf_s);
    current_shape = align_mean(meanShape, faceBox);
//LOGE("ldmarkmodel::track %d %d \n", __LINE__, eee_system_current_milli()-perf_s);
    int numLandmarks = current_shape.cols()/2;
	
//LOGE("ldmarkmodel::track %d %d \n", __LINE__, eee_system_current_milli()-perf_s);
    for(int i=0; i<LinearRegressors.size(); i++){
//    for( int i=0; i<5; i++ ){
//        LOGE("ldmarkmodel::track %d %d %d \n", __LINE__, i, eee_system_current_milli()-perf_s);
        cv2::Matrix2d<float> Descriptor = CalculateHogDescriptor(grayImage, current_shape, LandmarkIndexs.at(i), eyes_index, HoGParams.at(i));
		
//        LOGE("ldmarkmodel::track %d %d %d \n", __LINE__, i, eee_system_current_milli()-perf_s);
		cv2::Matrix2d<float> update_step = LinearRegressors.at(i).predict(Descriptor);//normalRegs[i].predict(Descriptor);
//        LOGE("ldmarkmodel::track %d %d %d \n", __LINE__, i, eee_system_current_milli()-perf_s);
        if(isNormal){
            float lx = ( current_shape(0, eyes_index.at(0))+current_shape(0, eyes_index.at(1)) )*0.5;
            float ly = ( current_shape(0, eyes_index.at(0)+numLandmarks)+current_shape(0, eyes_index.at(1)+numLandmarks) )*0.5;
            float rx = ( current_shape(0, eyes_index.at(2))+current_shape(0, eyes_index.at(3)) )*0.5;
            float ry = ( current_shape(0, eyes_index.at(2)+numLandmarks)+current_shape(0, eyes_index.at(3)+numLandmarks) )*0.5;
            float distance = sqrt( (rx-lx)*(rx-lx)+(ry-ly)*(ry-ly) );
            update_step = update_step*distance;
        }
        current_shape = current_shape + update_step;
    }
//    LOGE("ldmarkmodel::track %d %d \n", __LINE__, eee_system_current_milli()-perf_s);

    return error_code;
}


void mydeserialize(ldmarkmodel& item, std::istream& in)
{
	//std::vector<std::vector<int>> LandmarkIndexs;
	uint64_t LandmarkIndexs_size = 0;
	in.read((char*)&LandmarkIndexs_size, sizeof(uint64_t));
	item.LandmarkIndexs.resize(LandmarkIndexs_size);
	for (int i = 0; i < LandmarkIndexs_size; ++i) {
		uint64_t size = 0;
		in.read((char*)&size, sizeof(uint64_t));

		std::vector<int> data(size);

		for (int j = 0; j < size; ++j) {
			int value;
			in.read((char*)&value, sizeof(int));
			data[j] = value;
		}
		item.LandmarkIndexs[i] = data;
	}
	//std::vector<int> eyes_index;
	{
		uint64_t size;
		in.read((char*)&size, sizeof(uint64_t));
		item.eyes_index.resize(size);
		for (int i = 0; i < size; ++i) {
			int value;
			in.read((char*)&value, sizeof(int));
			item.eyes_index[i] = value;
		}
	}
    
	//cv::Mat meanShape;
	{
		int rows, cols, type;
		bool continuous;

		in.read((char*)&rows, sizeof(int));
		in.read((char*)&cols, sizeof(int));
		in.read((char*)&type, sizeof(int));
		in.read((char*)&continuous, sizeof(bool));
		item.meanShape.create(rows, cols);
        
		// 没有明确证据，根据type？
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				float value;
				in.read((char*)&value, sizeof(float));
				item.meanShape(i, j) = value;
			}
		}

	}
	//std::vector<HoGParam> HoGParams;
	{
		uint64_t size;
		in.read((char*)&size, sizeof(uint64_t));
		item.HoGParams.resize(size);
		for (int i = 0; i < size; ++i) {
			int  vlhog_variant;
			int num_cells;
			int cell_size;
			int num_bins;
			float relative_patch_size;

			in.read((char*)&vlhog_variant, sizeof(int));
			in.read((char*)&num_cells, sizeof(int));
			in.read((char*)&cell_size, sizeof(int));
			in.read((char*)&num_bins, sizeof(int));
			in.read((char*)&relative_patch_size, sizeof(float));
			HoGParam hogParams;
			hogParams.vlhog_variant = (VlHogVariant)vlhog_variant;
			hogParams.num_cells = num_cells;
			hogParams.cell_size = cell_size;
			hogParams.num_bins = num_bins;
			hogParams.relative_patch_size = relative_patch_size;

			item.HoGParams[i] = hogParams;
		}
	}
	//bool isNormal;
	{
		bool isNormal;
		in.read((char*)&isNormal, sizeof(bool));
		item.isNormal = isNormal;

	}
	//std::vector<LinearRegressor> LinearRegressors;
	{
		uint64_t size;
		in.read((char*)&size, sizeof(uint64_t));
		//outfile << size << std::endl;
        item.LinearRegressors.resize(size);

        for (int m = 0; m < size; ++m) {

            LinearRegressor linearRegressor;
            mydeserialize(linearRegressor, in);
            item.LinearRegressors[m] = linearRegressor;
//            mydeserialize(item.normalRegs[m], in);
        }
	}
}

bool load_ldmarkmodel_reduce(std::string filename, ldmarkmodel &model)
{
    std::ifstream file;
    file.open(filename.c_str(), std::ifstream::binary);
    if (!file.is_open()) {
        LOGE("file.is_open false %d \n", __LINE__);
		return false;
    }
	mydeserialize(model, file);
	file.close();
	return true;
}



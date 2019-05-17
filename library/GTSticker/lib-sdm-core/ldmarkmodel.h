#ifndef LDMARKMODEL_H_
#define LDMARKMODEL_H_

#include <iostream>
#include <vector>
#include <fstream>


#include "matrix.h"
#include "helper.h"
#include "feature_descriptor.h"




#define SDM_NO_ERROR        0       //无错误
#define SDM_ERROR_FACEDET   200     //重新通过CascadeClassifier检测到人脸
#define SDM_ERROR_FACEPOS   201     //人脸位置变化较大，可疑
#define SDM_ERROR_FACESIZE  202     //人脸大小变化较大，可疑
#define SDM_ERROR_FACENO    203     //找不到人脸
#define SDM_ERROR_IMAGE     204     //图像错误

#define SDM_ERROR_ARGS      400     //参数传递错误
#define SDM_ERROR_MODEL     401     //模型加载错误

class LinearRegressor{

public:
    LinearRegressor();
	cv2::Matrix2d<float> predict(cv2::Matrix2d<float> values);
    friend void mydeserialize(LinearRegressor& item, std::istream& in);

public:
    cv2::Matrix2d<float> weights;

};

class ldmarkmodel{

public:
    ldmarkmodel();
	
	int  track(const cv2::Matrix2d<uint8_t>& src, cv2::Matrix2d<float>& current_shape, bool newFacePos, cv2::Rect facePosBox);
	
	friend void mydeserialize(ldmarkmodel& item, std::istream& in);
private:
    cv2::Rect faceBox;


    std::vector<std::vector<int>> LandmarkIndexs;
    std::vector<int> eyes_index;
	cv2::Matrix2d<float> meanShape;
    std::vector<HoGParam> HoGParams;
    bool isNormal;
    std::vector<LinearRegressor> LinearRegressors;
//    LinearRegressor normalRegs[5];
};

void mydeserialize(LinearRegressor& item, std::istream& in);
void mydeserialize(ldmarkmodel& item, std::istream& in);


//加载模型
bool load_ldmarkmodel_reduce(std::string filename, ldmarkmodel &model);


#endif



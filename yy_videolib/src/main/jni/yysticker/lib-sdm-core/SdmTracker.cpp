//
//  SdmWrapper.m
//  EyeTest
//
//  Created by YY on 2018/1/13.
//  Copyright © 2018年 YY. All rights reserved.
//
#include <vector>
#include <iostream>
#include <fstream>

#include "plat_log.h"

//#include "opencv2/opencv.hpp"
//#include "opencv2/core/core.hpp"
//#include "opencv2/highgui/highgui.hpp"
//#include "opencv2/objdetect/objdetect.hpp"

#include "ldmarkmodel.h"

#include "plat_log.h"
#include "SdmTracker.h"

using namespace std;
using namespace cv2;

SdmTracker::SdmTracker()
{
    this->ldmarkmodel_ptr = new ldmarkmodel();
    memset(&(this->points[0]), 0x00, sizeof(this->points));
    this->mark_x = &(this->points[0]);
    this->mark_y = &(this->points[68]);
//    memset(&mark_x[0], 0x00, sizeof(mark_x));
//    memset(&mark_y[0], 0x00, sizeof(mark_y));
}

void SdmTracker::loadModel(char * path)
{
    //ldmarkmodel modelt;
    ldmarkmodel * p = (ldmarkmodel*)this->ldmarkmodel_ptr;
    
    std::string modelFilePath = path;//"roboman-landmark-model.bin";
//    while(!load_ldmarkmodel(modelFilePath, *p)){
//        std::cout << "no model." << std::endl;
//        return;
//    }
    load_ldmarkmodel_reduce(modelFilePath, *p);
    
//    LOGE( "load model ok ." );
    
    return;
}
//static cv::Mat current_shape;
/*
void SdmTracker::trackImage(void * data, int size, int w, int h, void * output)
{
    //ldmarkmodel modelt;
    ldmarkmodel * p = (ldmarkmodel*)this->ldmarkmodel_ptr;
    
    cv::Mat Image = cv::Mat(h, w, CV_8UC4, data, 0);;
    
    p->track(Image, current_shape);
    cv::Vec3d eav;
    p->EstimateHeadPose(current_shape, eav);
    p->drawPose(Image, current_shape, 50);
    
    int numLandmarks = current_shape.cols/2;
    for(int j=0; j<numLandmarks; j++){
        int x = current_shape.at<float>(j);
        int y = current_shape.at<float>(j + numLandmarks);
        
        mark_x[j] = x;
        mark_y[j] = y;
//        printf("x,y %d,%d ", x, y);
        std::stringstream ss;
        ss << j;
        cv::putText(Image, ss.str(), cv::Point(x, y), 0.5, 0.5, cv::Scalar(0, 0, 255));
        cv::circle(Image, cv::Point(x, y), 2, cv::Scalar(0, 0, 255), -1);
    }
//    printf("\n");
    
    memcpy(output, Image.data, Image.total()*4);
}
*/
/***
void SdmTracker::trackImage(void * data, int size, int w, int h, void * output)
{
    //ldmarkmodel modelt;
    ldmarkmodel * p = (ldmarkmodel*)this->ldmarkmodel_ptr;
    uint8_t * u = (uint8_t*)data;
//    LOGE("SdmTracker::trackImage trackImage %d \n", __LINE__);
//    cv::Mat Image = cv::Mat(h, w, CV_8UC1, data, 0);;
    
    cv2::Matrix2d<uint8_t> grayImage;
    grayImage.create(h, w);
//    for (int i = 0; i < h; ++i) {
//        for (int j = 0; j < w; ++j) {
////            LOGE("SdmTracker::trackImage bef trackImage (%d,%d) %d \n", i, j, __LINE__);
//            grayImage(i, j) = u[i*w+j];
////            LOGE("SdmTracker::trackImage aft trackImage (%d,%d) %d \n", i, j, __LINE__);
//        }
//    }
	memcpy(grayImage.getData(), u, w*h);
//    LOGE("SdmTracker::trackImage trackImage %d \n", __LINE__);
    // TODO:
    cv2::Matrix2d<float> current_shape;
    
    cv2::Rect faceBox;
    faceBox.x = this->face_x;
    faceBox.y = this->face_y;
    faceBox.width = this->face_width;
    faceBox.height = this->face_height;
//    LOGE("SdmTracker::trackImage trackImage %d (%d,%d,%d,%d) \n", __LINE__, this->face_x, this->face_y, this->face_width, this->face_height);
    p->track(grayImage, current_shape, true, faceBox);
//    LOGE("SdmTracker::trackImage trackImage %d \n", __LINE__);
//    LOGE("@@@@@@\n");
    float cache_points[136];
    float * o_data = (float*) output;
    for (int i = 0; i < current_shape.rows(); ++i) {
        for (int j = 0; j < current_shape.cols(); ++j) {
            float value = current_shape(i, j);
            //            if( j<8 )
            //            LOGE("(%d,%d:%f)", i,j,value);
            cache_points[j+j*i] = value;
            //            this->points[j+j*i] = value;
            //            o_data[j+j*i] = value;
        }
    }
    float diff_x = cache_points[28] - this->points[28];
    float diff_y = cache_points[28+68] - this->points[28+68];
    if( (diff_x < 8.0f && diff_x > -8.0f) && (diff_y < 8.0f && diff_y > -8.0f) ) {
        
        for( int i=0; i<136; i++ ) {
            this->points[i] = (cache_points[i]*0.2 + this->points[i]*0.8);
            o_data[i] = this->points[i];
        }
        return;
    }
    for( int i=0; i<136; i++ ) {
        this->points[i] = cache_points[i];
        o_data[i] = this->points[i];
    }
//    LOGE("@@@@@@\n");
    
    return;
}
***/

void SdmTracker::trackImage(void * data, int size, int w, int h, void * output)
{
    this->trackImage(data, size, w, h, 0, 0, 0, output);
}
void SdmTracker::trackImage(void * data, int size, int w, int h, int x, int y, double angle, void * output)
{
    //ldmarkmodel modelt;
    ldmarkmodel * p = (ldmarkmodel*)this->ldmarkmodel_ptr;
    uint8_t * u = (uint8_t*)data;
    //    LOGE("SdmTracker::trackImage trackImage %d \n", __LINE__);
    //    cv::Mat Image = cv::Mat(h, w, CV_8UC1, data, 0);;
    
    cv2::Matrix2d<uint8_t> grayImage;
    grayImage.create(h, w);
    //    for (int i = 0; i < h; ++i) {
    //        for (int j = 0; j < w; ++j) {
    ////            LOGE("SdmTracker::trackImage bef trackImage (%d,%d) %d \n", i, j, __LINE__);
    //            grayImage(i, j) = u[i*w+j];
    ////            LOGE("SdmTracker::trackImage aft trackImage (%d,%d) %d \n", i, j, __LINE__);
    //        }
    //    }
    memcpy(grayImage.getData(), u, w*h);
    //    LOGE("SdmTracker::trackImage trackImage %d \n", __LINE__);
    // TODO:
    cv2::Matrix2d<float> current_shape;
    
    cv2::Rect faceBox;
    faceBox.x = this->face_x;
    faceBox.y = this->face_y;
    faceBox.width = this->face_width;
    faceBox.height = this->face_height;
//        LOGE("SdmTracker::trackImage trackImage %d (%d,%d,%d,%d) \n", __LINE__, this->face_x, this->face_y, this->face_width, this->face_height);
    p->track(grayImage, current_shape, true, faceBox);
//        LOGE("SdmTracker::trackImage trackImage %d \n", __LINE__);
    //    LOGE("@@@@@@\n");
    float cache_points[136];
    float * o_data = (float*) output;
    
    for (int i = 0; i < current_shape.rows(); ++i) {
        for (int j = 0; j < current_shape.cols(); ++j) {
            float value = current_shape(i, j);
            //            if( j<8 )
            //            LOGE("(%d,%d:%f)", i,j,value);
            cache_points[j+j*i] = value;
            //            this->points[j+j*i] = value;
            //            o_data[j+j*i] = value;
        }
    }
    
    if(angle < -0.0001 || angle > 0.0001){
        double m[6];
        getRotationMatrix2D( x, y, -angle, 1, m);
        for(int i = 0; i < 68; ++i){
            int x = cache_points[i];
            int y = cache_points[68+i];
            cache_points[i] = m[0] *x + m[1] *y + m[2];
            cache_points[68+i] = m[3] *x + m[4] *y + m[5];
        }

    }
    
    float diff_x = cache_points[28] - this->points[28];
    float diff_y = cache_points[28+68] - this->points[28+68];
    if( (diff_x < 5.0f && diff_x > -5.0f) && (diff_y < 5.0f && diff_y > -5.0f) ) {
        
        for( int i=0; i<136; i++ ) {
            this->points[i] = (cache_points[i]*0.2 + this->points[i]*0.8);
            o_data[i] = this->points[i];
        }
        return;
    }
    
    for( int i=0; i<136; i++ ) {
        this->points[i] = cache_points[i];
        o_data[i] = this->points[i];
    }
    
    return;
}

void SdmTracker::getRotationParams(int *centerX, int *centerY, float *angle)
{
    int leftX, leftY;
    int rightX, rightY;
    locateLeftEye(&leftX, &leftY);
    locateRightEye(&rightX, &rightY);
    
    *centerX = (leftX + rightX) /2;
    *centerY = (leftY + rightY) / 2;
    
    *angle = calculateAngle(leftX, leftY, rightX, rightY);
}

void SdmTracker::locatePoint(int index, int * x, int * y)
{
    *x = (int)(mark_x[index]);
    *y = (int)(mark_y[index]);
    
    return;
}

void SdmTracker::locateLeftEye(int * x, int * y)
{
    // 取以下几个点的中心点
//    36,37,38,39
//    41,40
    
    *x = (int)(mark_x[37-1] + mark_x[38-1] + mark_x[40-1] + mark_x[41-1])/4;
    *y = (int)(mark_y[37-1] + mark_y[38-1] + mark_y[40-1] + mark_y[41-1])/4;
    
    return;
}

void SdmTracker::locateRightEye(int * x, int * y)
{
//    42,43,44,45
//    47,46
    *x = (int)(mark_x[43-1] + mark_x[44-1] + mark_x[46-1] + mark_x[47-1])/4;
    *y = (int)(mark_y[43-1] + mark_y[44-1] + mark_y[46-1] + mark_y[47-1])/4;
    
    return;
}

void SdmTracker::locateRightThinFace(int * sx, int * sy, int * dx, int * dy)
{
    int mark_a = 10;
    int mark_b = 14;
    
    // 10 - 14
    // 1 - 7, 4
    float center_x = (mark_x[mark_a] + mark_x[mark_b]) / 2;
    float center_y = (mark_y[mark_a] + mark_y[mark_b]) / 2;
    
    // 把mark[7]当作坐标原点
    Matrix2d<int> m1, m2, m3;
    
    m1.create(3, 3);
    m2.create(3, 3);
    m3.create(3, 3);
    
    m1(0,0) = 1;
    m1(0,1) = 0;
    m1(0,2) = center_x;
    m1(1,0) = 0;
    m1(1,1) = 1;
    m1(1,2) = center_y;
    m1(2,0) = 0;
    m1(2,1) = 0;
    m1(2,2) = 1;
    
    m2(0,0) = 0;
    m2(0,1) = 1;
    m2(0,2) = 0;
    m2(1,0) = -1;
    m2(1,1) = 0;
    m2(1,2) = 0;
    m2(2,0) = 0;
    m2(2,1) = 0;
    m2(2,2) = 1;
    
    m3(0,0) = 1;
    m3(0,1) = 0;
    m3(0,2) = -center_x;
    m3(1,0) = 0;
    m3(1,1) = 1;
    m3(1,2) = -center_y;
    m3(2,0) = 0;
    m3(2,1) = 0;
    m3(2,2) = 1;
    
    Matrix2d<int> markpoint7, markpoint1;
    
    markpoint7.create(3, 1);
    markpoint7(0,0) = mark_x[mark_b];
    markpoint7(1,0) = mark_y[mark_b];
    markpoint7(2,0) = 1;
    
    markpoint1.create(3, 1);
    markpoint1(0,0) = mark_x[mark_a];
    markpoint1(1,0) = mark_y[mark_a];
    markpoint1(2,0) = 1;
    
    // 分别把markpoint1和7，围绕center旋转90度
    Matrix2d<int> merge = m1 * m2;
    merge = merge * m3;
    
    Matrix2d<int> start = merge * markpoint1;
    Matrix2d<int> end = merge * markpoint7;
    
    *sx = start(0, 0);
    *sy = start(0, 1);
    
    *dx = end(0, 0);
    *dy = end(0, 1);
    /*
    int mark_a = 10;
    int mark_b = 14;
    
    // 10 - 14
    // 1 - 7, 4
    float center_x = (mark_x[mark_a] + mark_x[mark_b]) / 2;
    float center_y = (mark_y[mark_a] + mark_y[mark_b]) / 2;
    
    // 把mark[7]当作坐标原点
    cv::Mat m1(cv::Size(3, 3),CV_32FC1);
    m1.at<float>(0,0)=1;
    m1.at<float>(0,1)=0;
    m1.at<float>(0,2)=center_x;
    m1.at<float>(1,0)=0;
    m1.at<float>(1,1)=1;
    m1.at<float>(1,2)=center_y;
    m1.at<float>(2,0)=0;
    m1.at<float>(2,1)=0;
    m1.at<float>(2,2)=1;
    
    cout << "m1:" << m1 << endl;
    
    cv::Mat m2(cv::Size(3, 3),CV_32FC1);
    m2.at<float>(0,0)=0;
    m2.at<float>(0,1)=1;
    m2.at<float>(0,2)=0;
    m2.at<float>(1,0)=-1;
    m2.at<float>(1,1)=0;
    m2.at<float>(1,2)=0;
    m2.at<float>(2,0)=0;
    m2.at<float>(2,1)=0;
    m2.at<float>(2,2)=1;
    
    cout << "m2:" << m2 << endl;
    
    cv::Mat m3(cv::Size(3, 3),CV_32FC1);
    m3.at<float>(0,0)=1;
    m3.at<float>(0,1)=0;
    m3.at<float>(0,2)=-center_x;
    m3.at<float>(1,0)=0;
    m3.at<float>(1,1)=1;
    m3.at<float>(1,2)=-center_y;
    m3.at<float>(2,0)=0;
    m3.at<float>(2,1)=0;
    m3.at<float>(2,2)=1;
    
    cout << "m3:" << m3 << endl;
    
    cv::Mat markpoint7(3, 1, CV_32FC1);
    markpoint7.at<float>(0,0)=mark_x[mark_b];
    markpoint7.at<float>(1,0)=mark_y[mark_b];
    markpoint7.at<float>(2,0)=1;
    
    cv::Mat markpoint1(3, 1, CV_32FC1);
    markpoint1.at<float>(0,0)=mark_x[mark_a];
    markpoint1.at<float>(1,0)=mark_y[mark_a];
    markpoint1.at<float>(2,0)=1;
    
    // 分别把markpoint1和7，围绕center旋转90度
    cv::Mat merge = m1 * m2 * m3;
    
    cv::Mat start = merge * markpoint1;
    cv::Mat end = merge * markpoint7;
    
    cout << "markpoint1:" << markpoint1 << " ---> " << start << endl;
    cout << "markpoint7:" << markpoint7 << " ---> " << end << endl;
    
    *sx = start.at<float>(0, 0);
    *sy = start.at<float>(0, 1);
    
    *dx = end.at<float>(0, 0);
    *dy = end.at<float>(0, 1);
    */
    return;
}

void SdmTracker::locateLeftThinFace(int * sx, int * sy, int * dx, int * dy)
{
    int mark_a = 2;
    int mark_b = 6;
    
    // 1 - 7, 4
    float center_x = (mark_x[mark_a] + mark_x[mark_b]) / 2;
    float center_y = (mark_y[mark_a] + mark_y[mark_b]) / 2;
    
    // 把mark[7]当作坐标原点
    Matrix2d<int> m1, m2, m3;
    
    m1.create(3, 3);
    m2.create(3, 3);
    m3.create(3, 3);
    
    m1(0,0) = 1;
    m1(0,1) = 0;
    m1(0,2) = center_x;
    m1(1,0) = 0;
    m1(1,1) = 1;
    m1(1,2) = center_y;
    m1(2,0) = 0;
    m1(2,1) = 0;
    m1(2,2) = 1;
    
    m2(0,0) = 0;
    m2(0,1) = 1;
    m2(0,2) = 0;
    m2(1,0) = -1;
    m2(1,1) = 0;
    m2(1,2) = 0;
    m2(2,0) = 0;
    m2(2,1) = 0;
    m2(2,2) = 1;
    
    m3(0,0) = 1;
    m3(0,1) = 0;
    m3(0,2) = -center_x;
    m3(1,0) = 0;
    m3(1,1) = 1;
    m3(1,2) = -center_y;
    m3(2,0) = 0;
    m3(2,1) = 0;
    m3(2,2) = 1;
    
    Matrix2d<int> markpoint7, markpoint1;
    
    markpoint7.create(3, 1);
    markpoint7(0,0) = mark_x[mark_b];
    markpoint7(1,0) = mark_y[mark_b];
    markpoint7(2,0) = 1;
    
    markpoint1.create(3, 1);
    markpoint1(0,0) = mark_x[mark_a];
    markpoint1(1,0) = mark_y[mark_a];
    markpoint1(2,0) = 1;
    
    // 分别把markpoint1和7，围绕center旋转90度
    Matrix2d<int> merge = m1 * m2;
    merge = merge * m3;
    
    Matrix2d<int> start = merge * markpoint1;
    Matrix2d<int> end = merge * markpoint7;
    
    *sx = start(0, 0);
    *sy = start(0, 1);
    
    *dx = end(0, 0);
    *dy = end(0, 1);
    
    /*
    int mark_a = 2;
    int mark_b = 6;
    
    // 1 - 7, 4
    float center_x = (mark_x[mark_a] + mark_x[mark_b]) / 2;
    float center_y = (mark_y[mark_a] + mark_y[mark_b]) / 2;
    
    // 把mark[7]当作坐标原点
    cv::Mat m1(cv::Size(3, 3),CV_32FC1);
    m1.at<float>(0,0)=1;
    m1.at<float>(0,1)=0;
    m1.at<float>(0,2)=center_x;
    m1.at<float>(1,0)=0;
    m1.at<float>(1,1)=1;
    m1.at<float>(1,2)=center_y;
    m1.at<float>(2,0)=0;
    m1.at<float>(2,1)=0;
    m1.at<float>(2,2)=1;
    
    cout << "m1:" << m1 << endl;
    
    cv::Mat m2(cv::Size(3, 3),CV_32FC1);
    m2.at<float>(0,0)=0;
    m2.at<float>(0,1)=1;
    m2.at<float>(0,2)=0;
    m2.at<float>(1,0)=-1;
    m2.at<float>(1,1)=0;
    m2.at<float>(1,2)=0;
    m2.at<float>(2,0)=0;
    m2.at<float>(2,1)=0;
    m2.at<float>(2,2)=1;
    
    cout << "m2:" << m2 << endl;
    
    cv::Mat m3(cv::Size(3, 3),CV_32FC1);
    m3.at<float>(0,0)=1;
    m3.at<float>(0,1)=0;
    m3.at<float>(0,2)=-center_x;
    m3.at<float>(1,0)=0;
    m3.at<float>(1,1)=1;
    m3.at<float>(1,2)=-center_y;
    m3.at<float>(2,0)=0;
    m3.at<float>(2,1)=0;
    m3.at<float>(2,2)=1;
    
    cout << "m3:" << m3 << endl;
    
    cv::Mat markpoint7(3, 1, CV_32FC1);
    markpoint7.at<float>(0,0)=mark_x[mark_b];
    markpoint7.at<float>(1,0)=mark_y[mark_b];
    markpoint7.at<float>(2,0)=1;
    
    cv::Mat markpoint1(3, 1, CV_32FC1);
    markpoint1.at<float>(0,0)=mark_x[mark_a];
    markpoint1.at<float>(1,0)=mark_y[mark_a];
    markpoint1.at<float>(2,0)=1;
    
    // 分别把markpoint1和7，围绕center旋转90度
    cv::Mat merge = m1 * m2 * m3;
    
    cv::Mat start = merge * markpoint1;
    cv::Mat end = merge * markpoint7;
    
    cout << "markpoint1:" << markpoint1 << " ---> " << start << endl;
    cout << "markpoint7:" << markpoint7 << " ---> " << end << endl;
    
    *sx = start.at<float>(0, 0);
    *sy = start.at<float>(0, 1);
    
    *dx = end.at<float>(0, 0);
    *dy = end.at<float>(0, 1);
    */
    return;
}

void SdmTracker::updateFaceRect(int x, int y, int width, int height)
{
//    ldmarkmodel * p = (ldmarkmodel*)this->ldmarkmodel_ptr;
//    p->updateFaceRect(x, y, width, height);
    this->face_x = x;
    this->face_y = y;
    this->face_width = width;
    this->face_height = height;
    
    return;
}

int SdmTracker::getEnclosingBox(float *left, float *top, float *right, float *bottom)
{
    if( this->mark_x == NULL || this->mark_y == NULL )
        return -1;
    
    *left = *right = this->mark_x[0];
    *top = *right = this->mark_y[0];
    
    for ( int i = 1; i < 68; i++ ) {
        if(this->mark_x[i] < *left) {
            *left = this->mark_x[i];
        } else if(this->mark_x[i] > *right) {
            *right = this->mark_x[i];
        }
        
        if(this->mark_y[i] < *top) {
            *top = this->mark_y[i];
        } else if(this->mark_y[i] > *bottom) {
            *bottom = this->mark_y[i];
        }
    }
    
    return 0;
}

void SdmTracker::getGlesImageVertex(float centerX, float centerY, float angle, float canvasWidth, float canvasHeight, float * out_data, int size)
{
    float imageVertices[] = {
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f,  1.0f,
        1.0f,  1.0f,
    };
    
    if( centerX >= 0 && centerY >= 0 && (angle > 10.0f || angle < -10.0f) ) {
        
        {
            cv2::Matrix2d<float> a1;
            cv2::Matrix2d<float> a2;
            cv2::Matrix2d<float> a3;
            
            cv2::Matrix2d<float> m1;
            cv2::Matrix2d<float> m2;
            cv2::Matrix2d<float> m3;
            cv2::Matrix2d<float> m4;
            
            a1.create(3, 3);
            a2.create(3, 3);
            a3.create(3, 3);
            
            m1.create(3, 1);
            m2.create(3, 1);
            m3.create(3, 1);
            m4.create(3, 1);
            
            float center_x = centerX;
            float center_y = canvasHeight - centerY;
            
            angle *= 3.1415926f / 180.0f;
            angle = 0.0f - angle;
            double cos_angle = cos(angle);
            double sin_angle = sin(angle);
            
            a1(0,0) = 1;
            a1(0,1) = 0;
            a1(0,2) = center_x;
            a1(1,0) = 0;
            a1(1,1) = 1;
            a1(1,2) = center_y;
            a1(2,0) = 0;
            a1(2,1) = 0;
            a1(2,2) = 1;
            
            a2(0,0) = cos_angle;
            a2(0,1) = sin_angle;
            a2(0,2) = 0;
            a2(1,0) = -sin_angle;
            a2(1,1) = cos_angle;
            a2(1,2) = 0;
            a2(2,0) = 0;
            a2(2,1) = 0;
            a2(2,2) = 1;
            
            a3(0,0) = 1;
            a3(0,1) = 0;
            a3(0,2) = -center_x;
            a3(1,0) = 0;
            a3(1,1) = 1;
            a3(1,2) = -center_y;
            a3(2,0) = 0;
            a3(2,1) = 0;
            a3(2,2) = 1;
            
            // 左上
            m1(0,0) = 0.0f;
            m1(1,0) = canvasHeight;
            m1(2,0) = 1;
            // 右上
            m2(0,0) = canvasWidth;
            m2(1,0) = canvasHeight;
            m2(2,0) = 1;
            // 左下
            m3(0,0) = 0.0f;
            m3(1,0) = 0.0f;
            m3(2,0) = 1;
            // 右下
            m4(0,0) = canvasWidth;
            m4(1,0) = 0.0f;
            m4(2,0) = 1;
            
            cv2::Matrix2d<float> merge = a1 * a2;
            merge = merge * a3;
            
            ////////////////////////////////////
            cv2::Matrix2d<float> result = merge * m1;
            
            float tx = result(0, 0);
            float ty = result(0, 1);
            
            // 左上
            imageVertices[0] = tx*2.0f / canvasWidth - 1.0f;
            imageVertices[1] = - (ty*2.0f / canvasHeight - 1.0f);
            
            ////////////////////////////////////
            result = merge * m2;
            
            tx = result(0, 0);
            ty = result(0, 1);
            
            // 右上
            imageVertices[2] = tx*2.0f / canvasWidth - 1.0f;
            imageVertices[3] = -(ty*2.0f / canvasHeight - 1.0f);
            
            ////////////////////////////////////
            result = merge * m3;
            
            tx = result(0, 0);
            ty = result(0, 1);
            
            // 左下
            imageVertices[4] = tx*2.0f / canvasWidth - 1.0f;
            imageVertices[5] = -(ty*2.0f / canvasHeight - 1.0f);
            
            ////////////////////////////////////
            result = merge * m4;
            
            tx = result(0, 0);
            ty = result(0, 1);
            
            // 右下
            imageVertices[6] = tx*2.0f / canvasWidth - 1.0f;
            imageVertices[7] = -(ty*2.0f / canvasHeight - 1.0f);
        }
    }
    
    // copy to outside
    for( int i=0; i<8 && i<size; i++ ) {
        out_data[i] = imageVertices[i];
    }
    
    return;
}

SdmTracker::~SdmTracker()
{
    if (this->ldmarkmodel_ptr != NULL) {
		delete this->ldmarkmodel_ptr;
		this->ldmarkmodel_ptr = NULL;
	}
	
}


//
//  SdmWrapper.h
//  EyeTest
//
//  Created by YY on 2018/1/13.
//  Copyright © 2018年 YY. All rights reserved.
//
#ifndef _SDM_WRAPPER_H_

#define _SDM_WRAPPER_H_

class SdmTracker {
    
    int face_x;
    int face_y;
    int face_width;
    int face_height;
    
public:
    float points[136];
    
    SdmTracker();
	~SdmTracker();
    
    void loadModel(char * path);
    
    void trackImage(void * data, int size, int w, int h, void * output);
    
    void locatePoint(int index, int * x, int * y);
    
    void locateLeftEye(int * x, int * y);
    void locateRightEye(int * x, int * y);
    
    void locateLeftThinFace(int * sx, int * sy, int * dx, int * dy);
    void locateRightThinFace(int * sx, int * sy, int * dx, int * dy);
    
    void updateFaceRect(int x, int y, int width, int height);
    
    void getGlesImageVertex(float centerX, float centerY, float angle, float canvasWidth, float canvasHeight, float * output, int size);
    void getRotationParams(int *centerX, int *centerY, float *angle);
    void trackImage(void * data, int size, int w, int h, int x, int y, double angle, void * output);
    int getEnclosingBox(float *left, float *top, float *right, float *bottom);
    
private:
    
    void * ldmarkmodel_ptr;
    float * mark_x;
    float * mark_y;
};

#endif

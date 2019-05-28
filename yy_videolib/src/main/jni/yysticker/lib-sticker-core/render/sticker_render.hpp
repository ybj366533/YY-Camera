//
//  sticker_render.hpp
//  YY
//
//  Created by YY on 2018/3/7.
//  Copyright © 2018年 YY. All rights reserved.
//

#ifndef sticker_render_hpp
#define sticker_render_hpp

#include <iostream>
#include <vector>
#include <string>
#include <map>

#include "matrix.h"

namespace gst
{
    class SingleFrame {
        
    public:
        
        SingleFrame();
        ~SingleFrame();
        
        std::string         imageFile;
        
        int                 width;
        int                 height;
        
        int                 positionIndex;
        int                 positionX;
        int                 positionY;
        
        int                 zPosition;
    };
    
    class GLESStickerRender
    {
    private:
        std::map<std::string, int> glesTextureMap;
        
        int filterInputTextureUniform;
        int filterPositionAttribute;
        int filterTextureCoordinateAttribute;
        
        float facePointList[512];
        int facePointCount;
        
        float canvasWidth;
        float canvasHeight;
        
        float faceRotateCos;
        float faceRotateSin;
        float faceScaleFactor;
        
        cv2::Matrix2d<float> a1;
        cv2::Matrix2d<float> a2;
        cv2::Matrix2d<float> a3;
        
        cv2::Matrix2d<float> m1;
        cv2::Matrix2d<float> m2;
        cv2::Matrix2d<float> m3;
        cv2::Matrix2d<float> m4;
        
        int realLoadImageToTexture(std::string path);
        void realDrawTexture(int textureId, int filterInputTextureUniform, int filterPositionAttribute, int filterTextureCoordinateAttribute, float * vArr);
        void realDeleteTexture(int textureId);
        
        void prepareMatrix();
        
    public:
        GLESStickerRender();
        ~GLESStickerRender();
        
        void clearTextureCache();
        void preloadTexture(std::vector<SingleFrame> v);
        void unloadTexture(std::vector<SingleFrame> v);
        
        void prepareProgramInfo(int uniform, int posAttri, int coorAttri);
        void drawTexture(std::vector<SingleFrame> v);
        
        void updateCanvasSize(int width, int height);
        void updateFacePoint(float * a, int s);
    };
}

#endif /* sticker_render_hpp */

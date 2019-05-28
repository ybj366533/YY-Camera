//
//  IOSRenderAPI.h
//  MGLiveFramework
//
//  Created by YY on 2018/3/7.
//  Copyright © 2018年 YY. All rights reserved.
//

#include <iostream>
#include <vector>
#include <string>
#include <map>

namespace gst
{
    class AndroidRenderAPI
    {
    public:
        AndroidRenderAPI();
        ~AndroidRenderAPI();
        
        static void drawTexture(int textureId, int filterInputTextureUniform, int filterPositionAttribute, int filterTextureCoordinateAttribute, float * vArr);
        static int loadPngToTexture(std::string imagefile);
        
        static void deleteTexture(int textureId);
    };
}

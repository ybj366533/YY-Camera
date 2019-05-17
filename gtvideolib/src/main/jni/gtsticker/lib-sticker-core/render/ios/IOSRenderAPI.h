//
//  IOSRenderAPI.h
//  gtv
//
//  Created by gtv on 2018/3/7.
//  Copyright © 2018年 gtv. All rights reserved.
//

#include <iostream>
#include <vector>
#include <string>
#include <map>

namespace gst
{
    class IOSRenderAPI
    {
    public:
        IOSRenderAPI();
        ~IOSRenderAPI();
        
        static void drawTexture(int textureId, int filterInputTextureUniform, int filterPositionAttribute, int filterTextureCoordinateAttribute, float * vArr);
        static int loadPngToTexture(std::string imagefile);
        static int loadPngToTexture2(std::string imagefile);
        static int loadPngToTexture3(std::string imagefile);
        
        static void deleteTexture(int textureId);
    };
}

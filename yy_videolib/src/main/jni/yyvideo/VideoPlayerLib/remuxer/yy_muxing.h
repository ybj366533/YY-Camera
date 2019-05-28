//
//  Created by YY on 16/12/22.
//  Copyright © 2016年 YY. All rights reserved.
//

#ifndef yy_muxing_h
#define yy_muxing_h

#ifdef __cplusplus
extern "C" {
#endif
    
    void * initStream(char * out_filename, int video_w, int video_h, int video_bps, int video_fps);
    
    int openStream(void * handle, uint8_t * video_meta, int len);
    int closeStream(void * handle);
    
    int checkStreamReady(void * handle);
    
    void writeAudioFrame(void * handle, uint8_t * data, int len, int64_t pts);
    void writeVideoFrame(void * handle, uint8_t * data, int len, int64_t pts, int isKey);
    
#ifdef __cplusplus
}
#endif

#endif /* YY_muxing_h */

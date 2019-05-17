//
//  gtv_segments.h
//  GTVideoLib
//
//  Created by gtv on 2018/1/26.
//  Copyright © 2018年 gtv. All rights reserved.
//

#ifndef gtv_segments_h
#define gtv_segments_h

#include "gtv_com_def.h"

#define D_SEG_PATH_LEN                  (1024)
#define D_SEG_FILE_LEN                  (64)
#define D_SEG_LIMIT_COUNT               (64)

typedef struct ST_GTV_ELEM_VIDEO_T {
    
    int                         valid;                      // 1:valid 0:invalid
    char                        name[D_SEG_FILE_LEN];       // 01.mp4
    int                         duration;                   // milli seconds
    float                       speed;                      // record speed 0.5,1,1.5
    
} ST_GTV_ELEM_VIDEO;

typedef struct ST_GTV_SEGMENTS_REC_T {
    
    char                        path[D_SEG_PATH_LEN];
    
    int                         count;
    ST_GTV_ELEM_VIDEO           data[D_SEG_LIMIT_COUNT];
    
} ST_GTV_SEGMENTS_REC;

typedef ST_GTV_SEGMENTS_REC* HANDLE_GTV_SEGMENTS_REC;

#ifdef __cplusplus
extern "C" {
#endif
    
// 打开分段管理文件
HANDLE_GTV_SEGMENTS_REC gtv_segrec_open(const char * path);

// 返回分段数量
int gtv_segrec_get_count(HANDLE_GTV_SEGMENTS_REC handle);

// 返回指定分段的视频名和时长
int gtv_segrec_get_video(HANDLE_GTV_SEGMENTS_REC handle, int index, char * name, int * duration, float * speed);

// 添加分段视频
int gtv_segrec_add_video(HANDLE_GTV_SEGMENTS_REC handle, const char * fname, int duration, float speed);

// 删除最后一个分段的视频
int gtv_segrec_remove_last(HANDLE_GTV_SEGMENTS_REC handle);

// 关闭分段管理
void gtv_segrec_close(HANDLE_GTV_SEGMENTS_REC handle);

#ifdef __cplusplus
}
#endif

#endif /* gtv_segments_h */

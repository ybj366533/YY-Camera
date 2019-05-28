//
//  Created by YY on 2017/4/30.
//  Copyright © 2017年 YY. All rights reserved.
//

#ifndef yy_raw_frame_h
#define yy_raw_frame_h

#include <stdio.h>
#include <stdint.h>

typedef enum {
    
    YY_UNKNOW_FRAME         = 0x0000,
    YY_VIDEO_FRAME         = 0x0001,
    YY_AUDIO_FRAME         = 0x0002
    
} YY_FRAME_TYPE;

typedef struct {
    
    YY_FRAME_TYPE type;
    
    int         pixel_width;
    int         pixel_height;
    
    uint8_t *   plane_data[3];
    uint32_t    plane_size[3];
    uint32_t	stride_size[3];
    
    int         plane_count;
    
    uint64_t    timestamp;
    int         retain_count;
    
} ST_YY_RAW_FRAME;

typedef ST_YY_RAW_FRAME * ST_YY_RAW_FRAME_REF;

typedef void (*FUNC_RAW_FRAME_NOTIFY)(void * target, ST_YY_RAW_FRAME_REF ref);

ST_YY_RAW_FRAME_REF YY_raw_frame_create_form_data(YY_FRAME_TYPE type, uint64_t timestamp, uint8_t * data[3], uint32_t size[3], uint32_t stride_size[3],int width, int hegiht);

// 创建一个空的图像frame
ST_YY_RAW_FRAME_REF YY_raw_frame_create(YY_FRAME_TYPE type, uint64_t timestamp, int width, int hegiht, uint8_t bgRGB[3]);

// 创建一个全0的声音frame
ST_YY_RAW_FRAME_REF YY_raw_frame_create_audio(YY_FRAME_TYPE type, uint64_t timestamp, int size);

//int YY_raw_frame_add_img(ST_YY_RAW_FRAME_REF frame_dst, ST_YY_RAW_FRAME_REF frame_src, int pos_x, int pos_y);
int YY_raw_frame_add_img(ST_YY_RAW_FRAME_REF frame_dst, ST_YY_RAW_FRAME_REF frame_src, int pos_x, int pos_y, int width, int height);

void YY_raw_frame_retain(ST_YY_RAW_FRAME_REF p);

void YY_raw_frame_destroy(ST_YY_RAW_FRAME_REF * p);

#endif /* YY_raw_frame_h */

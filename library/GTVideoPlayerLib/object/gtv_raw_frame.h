//
//  gtv_frame_packer.h
//  gtv
//
//  Created by gtv on 2017/4/30.
//  Copyright © 2017年 gtv. All rights reserved.
//

#ifndef gtv_raw_frame_h
#define gtv_raw_frame_h

#include <stdio.h>
#include <stdint.h>

typedef enum {
    
    GTV_UNKNOW_FRAME         = 0x0000,
    GTV_VIDEO_FRAME         = 0x0001,
    GTV_AUDIO_FRAME         = 0x0002
    
} GTV_FRAME_TYPE;

typedef struct {
    
    GTV_FRAME_TYPE type;
    
    int         pixel_width;
    int         pixel_height;
    
    uint8_t *   plane_data[3];
    uint32_t    plane_size[3];
    uint32_t	stride_size[3];
    
    int         plane_count;
    
    uint64_t    timestamp;
    int         retain_count;
    
} ST_GTV_RAW_FRAME;

typedef ST_GTV_RAW_FRAME * ST_GTV_RAW_FRAME_REF;

typedef void (*FUNC_RAW_FRAME_NOTIFY)(void * target, ST_GTV_RAW_FRAME_REF ref);

ST_GTV_RAW_FRAME_REF gtv_raw_frame_create_form_data(GTV_FRAME_TYPE type, uint64_t timestamp, uint8_t * data[3], uint32_t size[3], uint32_t stride_size[3],int width, int hegiht);

// 创建一个空的图像frame
ST_GTV_RAW_FRAME_REF gtv_raw_frame_create(GTV_FRAME_TYPE type, uint64_t timestamp, int width, int hegiht, uint8_t bgRGB[3]);

// 创建一个全0的声音frame
ST_GTV_RAW_FRAME_REF gtv_raw_frame_create_audio(GTV_FRAME_TYPE type, uint64_t timestamp, int size);

//int gtv_raw_frame_add_img(ST_GTV_RAW_FRAME_REF frame_dst, ST_GTV_RAW_FRAME_REF frame_src, int pos_x, int pos_y);
int gtv_raw_frame_add_img(ST_GTV_RAW_FRAME_REF frame_dst, ST_GTV_RAW_FRAME_REF frame_src, int pos_x, int pos_y, int width, int height);

void gtv_raw_frame_retain(ST_GTV_RAW_FRAME_REF p);

void gtv_raw_frame_destroy(ST_GTV_RAW_FRAME_REF * p);

#endif /* gtv_raw_frame_h */

//
//  Created by YY on 2017/4/30.
//  Copyright © 2017年 YY. All rights reserved.
//

#include "yy_raw_frame.h"
#include "yy_logger.h"

#include "stdio.h"
#include "stdlib.h"
#include "string.h"

ST_YY_RAW_FRAME_REF YY_raw_frame_create(YY_FRAME_TYPE type, uint64_t timestamp, int width, int height, uint8_t bgRGB[3])
{
	if (type != YY_VIDEO_FRAME)
	{
		return NULL;
	}

    ST_YY_RAW_FRAME_REF f = (ST_YY_RAW_FRAME_REF)malloc(sizeof(ST_YY_RAW_FRAME));

    if( f == NULL ) {
        return NULL;
    }

    memset(f, 0x00, sizeof(ST_YY_RAW_FRAME));

    f->timestamp = timestamp;
    f->type = type;
    f->pixel_width = width;
    f->pixel_height = height;

    f->plane_count = 3;
    //暂不考虑跨距

    uint8_t Y, U, V;
//    Y=0.30 * bgRGB[0] +0.59 * bgRGB[1] + 0.11 * bgRGB[2];
//    U=0.493 * (bgRGB[0] - Y);
//    V=0.877 * (bgRGB[1] - Y);
    Y = 0;
    U = 0;//0x7f;
    V = 0;//0x7f;

    f->stride_size[0] = f->pixel_width;
    f->plane_size[0] = f->stride_size[0] * f->pixel_height;
    f->plane_data[0] = (uint8_t*)malloc(f->plane_size[0]);
    memset(f->plane_data[0], Y, f->plane_size[0]);

    f->stride_size[1] = f->pixel_width >>1;
    f->plane_size[1] = f->stride_size[1] * (f->pixel_height >> 1);
    f->plane_data[1] = (uint8_t*)malloc(f->plane_size[1]);
    memset(f->plane_data[1], U, f->plane_size[1]);

    f->stride_size[2] = f->pixel_width >>1;
    f->plane_size[2] = f->stride_size[2] * (f->pixel_height >> 1);
    f->plane_data[2] = (uint8_t*)malloc(f->plane_size[2]);
    memset(f->plane_data[2], V, f->plane_size[2]);

    f->retain_count ++;

    return f;
}

ST_YY_RAW_FRAME_REF YY_raw_frame_create_audio(YY_FRAME_TYPE type, uint64_t timestamp, int size)
{
	if (type != YY_AUDIO_FRAME)
	{
		return NULL;
	}

    ST_YY_RAW_FRAME_REF f = (ST_YY_RAW_FRAME_REF)malloc(sizeof(ST_YY_RAW_FRAME));

    if( f == NULL ) {
        return NULL;
    }

    memset(f, 0x00, sizeof(ST_YY_RAW_FRAME));

    f->timestamp = timestamp;
    f->type = type;

    f->plane_count = 1;
    f->plane_size[0] = size;
    f->plane_data[0] = (uint8_t*)malloc(f->plane_size[0]);
    memset(f->plane_data[0],0x00, f->plane_size[0]);

    f->retain_count ++;

    return f;
}

ST_YY_RAW_FRAME_REF YY_raw_frame_create_form_data(YY_FRAME_TYPE type, uint64_t timestamp, uint8_t * data[3], uint32_t size[3], uint32_t stride_size[3], int width, int height)
{
    int i=0;
    ST_YY_RAW_FRAME_REF f = (ST_YY_RAW_FRAME_REF)malloc(sizeof(ST_YY_RAW_FRAME));
    
    if( f == NULL ) {
        return NULL;
    }
    
    memset(f, 0x00, sizeof(ST_YY_RAW_FRAME));
    for( i=0; i<3; i++ ) {
        
        if( data[i] != NULL && size[i] > 0 ) {
            
            f->plane_data[i] = (uint8_t*)malloc(size[i]);
            
            memcpy(f->plane_data[i], data[i], size[i]);
            f->plane_size[i] = size[i];
            f->plane_count ++;
            f->stride_size[i] = stride_size[i];
        }
        else {
            f->plane_data[i] = NULL;
            f->plane_size[i] = 0;
        }
    }
    
    f->timestamp = timestamp;
    f->type = type;
    f->pixel_width = width;
    f->pixel_height = height;
    f->retain_count ++;
    
    return f;
}

// 拷贝图像的一个平面（无缩放）
void copy_image_plane(uint8_t *image_data_to, uint32_t width_to, uint32_t height_to, uint32_t stride_to, int pos_x, int pos_y,
		uint8_t *image_data_from, uint32_t width_from, uint32_t height_from, uint32_t stride_from)
{
    int i = 0;
	{
		uint8_t *dst_data = image_data_to + stride_to * pos_y + pos_x;
        uint8_t *src_data = image_data_from;
        int dst_len = width_to - pos_x;
        int left = 0;

		for ( i = 0; i < height_from && i < height_to - pos_y; ++i)
		{
            left = dst_len < width_from ? dst_len : width_from;
			memcpy(dst_data, src_data, left);

			dst_data += stride_to;
			src_data += stride_from;
		}
	}
}

// 拷贝图像的一个平面（缩放）(最粗暴)
// todo 计算后的坐标是否越界检查

void copy_image_plane_scale(uint8_t *image_data_to, uint32_t width_to, uint32_t height_to, uint32_t stride_to, int pos_x, int pos_y, int w, int h,
		uint8_t *image_data_from, uint32_t width_from, uint32_t height_from, uint32_t stride_from, double scale_x, double scale_y)
{
    int x, y, x1, y1;
    x = y = x1 = y1 = 0;
	//uint8_t *dst_data = image_data_to + stride_to * pos_y + pos_x;
	//uint8_t *src_data = image_data_from;

	// 根据目标图像的宽高调整 宽高
	if (pos_x + w > width_to)
	{
		w = width_to - pos_x;
	}

	if (pos_y + h > height_to)
	{
		h = height_to - pos_y;
	}

	for ( y = 0; y < h; ++y)
	{
		for (x = 0; x < w; ++x)
		{
			x1 = x * scale_x + 0.5;
			y1 = y * scale_y + 0.5;

			uint8_t *dst_data = image_data_to + stride_to * (pos_y + y) + (pos_x + x);
			uint8_t *src_data = image_data_from + stride_from * y1 + x1;

			*dst_data = *src_data;

		}
	}
}

int YY_raw_frame_add_img(ST_YY_RAW_FRAME_REF frame_dst, ST_YY_RAW_FRAME_REF frame_src, int pos_x, int pos_y, int width, int height)
{
	if (frame_dst->type != YY_VIDEO_FRAME || frame_src->type != YY_VIDEO_FRAME)
	{
		return -1;
	}

	// 不做缩放的情况
	if (((width == 0) && (height == 0)) || ((frame_src->pixel_width == width) && (frame_src->pixel_height == height)))
	{
		// 拷贝Y-plane
		copy_image_plane(frame_dst->plane_data[0], frame_dst->pixel_width, frame_dst->pixel_height, frame_dst->stride_size[0], pos_x, pos_y,
				frame_src->plane_data[0], frame_src->pixel_width, frame_src->pixel_height, frame_src->stride_size[0]);

		// 拷贝U-plane
		copy_image_plane(frame_dst->plane_data[1], frame_dst->pixel_width/2, frame_dst->pixel_height/2, frame_dst->stride_size[1], pos_x/2, pos_y/2,
				frame_src->plane_data[1], frame_src->pixel_width/2, frame_src->pixel_height/2, frame_src->stride_size[1]);

		// 拷贝V-plane
		copy_image_plane(frame_dst->plane_data[2], frame_dst->pixel_width/2, frame_dst->pixel_height/2, frame_dst->stride_size[2], pos_x/2, pos_y/2,
				frame_src->plane_data[2], frame_src->pixel_width/2, frame_src->pixel_height/2, frame_src->stride_size[2]);
	} else
	{
		YY_DEBUG("image scale start.\n");

		// 需要缩放 (todo, 这2个变量在函数内算)
		double scale_x = frame_src->pixel_width * 1.0 / width;
		double scale_y = frame_src->pixel_height * 1.0 / height;

		// 拷贝Y-plane
		copy_image_plane_scale(frame_dst->plane_data[0], frame_dst->pixel_width, frame_dst->pixel_height, frame_dst->stride_size[0], pos_x, pos_y, width, height,
				frame_src->plane_data[0], frame_src->pixel_width, frame_src->pixel_height, frame_src->stride_size[0], scale_x, scale_y);

		// 拷贝U-plane
		copy_image_plane_scale(frame_dst->plane_data[1], frame_dst->pixel_width/2, frame_dst->pixel_height/2, frame_dst->stride_size[1], pos_x/2, pos_y/2, width/2, height/2,
				frame_src->plane_data[1], frame_src->pixel_width/2, frame_src->pixel_height/2, frame_src->stride_size[1], scale_x, scale_y);

		// 拷贝V-plane
		copy_image_plane_scale(frame_dst->plane_data[2], frame_dst->pixel_width/2, frame_dst->pixel_height/2, frame_dst->stride_size[2], pos_x/2, pos_y/2, width/2, height/2,
				frame_src->plane_data[2], frame_src->pixel_width/2, frame_src->pixel_height/2, frame_src->stride_size[2], scale_x, scale_y);

		YY_DEBUG("image scale end.\n");
	}

	// 其他合法性检查，比如大小等（src，要比dst小）
//
//	{
//		uint8_t *dst_data = frame_dst->plane_data[0] + frame_dst->stride_size[0] * pos_y + pos_x;
//        uint8_t *src_data = frame_src->plane_data[0];
//        int dst_len = frame_dst->stride_size[0] - pos_x;
//        int left = 0;
//
//		for ( int i = 0; i < frame_src->pixel_height && i < frame_dst->pixel_height - pos_y; ++i)
//		{
//            left = dst_len < frame_src->pixel_width ? dst_len : frame_src->pixel_width;
//			memcpy(dst_data, src_data, left);
//
//			dst_data += frame_dst->stride_size[0];
//			src_data += frame_src->stride_size[0];
//		}
//	}
//
//	{
//		uint8_t *dst_data = frame_dst->plane_data[1] + frame_dst->stride_size[1] * (pos_y/2) + pos_x/2;
//		uint8_t *src_data = frame_src->plane_data[1];
//        int dst_len = frame_dst->stride_size[1] - pos_x/2;
//        int left = 0;
//
//		for ( int i = 0; i < frame_src->pixel_height/2 && i < frame_dst->pixel_height/2 - pos_y/2; ++i)
//		{
//            left = dst_len < frame_src->pixel_width/2 ? dst_len : frame_src->pixel_width/2;
//			memcpy(dst_data, src_data, left);
//
//			dst_data += frame_dst->stride_size[1];
//			src_data += frame_src->stride_size[1];
//		}
//	}
//
//	{
//		uint8_t *dst_data = frame_dst->plane_data[2] + frame_dst->stride_size[2] * (pos_y/2) + pos_x/2;
//		uint8_t *src_data = frame_src->plane_data[2];
//        int dst_len = frame_dst->stride_size[2] - pos_x/2;
//        int left = 0;
//
//		for ( int i = 0; i < frame_src->pixel_height/2 && i < frame_dst->pixel_height/2 - pos_y/2; ++i)
//		{
//            left = dst_len < frame_src->pixel_width/2 ? dst_len : frame_src->pixel_width/2;
//			memcpy(dst_data, src_data, left);
//
//			dst_data += frame_dst->stride_size[2];
//			src_data += frame_src->stride_size[2];
//		}
//	}

    return 0;
}

void YY_raw_frame_retain(ST_YY_RAW_FRAME_REF p)
{
    p->retain_count ++;
    
    return;
}

void YY_raw_frame_destroy(ST_YY_RAW_FRAME_REF * p)
{
    int i=0;
    
    if( p == NULL )
        return;
    
    ST_YY_RAW_FRAME_REF ref = *p;
    
    if( ref ) {
        
        ref->retain_count --;
        if( ref->retain_count > 0 ) {
            return;
        }
        
        for( i=0; i<3; i++ ) {
            
            if( ref->plane_data[i] != NULL ) {
                free(ref->plane_data[i]);
                ref->plane_data[i] = NULL;
            }
        }
        
        free(ref);
    }
    
    *p = NULL;
}

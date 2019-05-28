//
//  Created by YY on 2018/1/29.
//  Copyright © 2018年 YY. All rights reserved.
//

#ifndef yy_remuxer_h
#define yy_remuxer_h

#include <stdio.h>

#define MAX_PATH 512

#define IMG_FORMAT_JPEG 1
#define IMG_FORMAT_YUV  2

#ifdef __cplusplus
extern "C" {
#endif

int YY_mp4_video_reverse(const char *in_filename, const char * out_filename);

int YY_mp4_clips_merge(const char *const* in_filenames, int in_file_num, const char * out_filename);

int YY_mp4_audio_video_merge(const char *in_filename_a, const char *in_filename_v, const char * out_filename);

int YY_mp4_time_crop(const char * in_filename, int start_milli, int end_milli, const char * out_filename);

// 抽取关键帧
// 在指定的时间范围内 和 指定的帧数，尽力均匀的抽取最近的关键帧。
// 输出图片格式支持 IMG_FORMAT_JPEG 1 / IMG_FORMAT_YUV  2
int YY_mp4_video_extract_frame(const char *in_filename, int out_imgformat, const char * out_folder, const char* out_file_prefix, int start_time, int end_time, int *data_timestamp, int data_num, int *out_data_num, float scale);

// 抽取声音波形图
// 在指定的时间范围内 和指定的数据个数，均匀的抽取
// todo  抽取单个数据，会不会偶然性太大？
int mp4_audio_extract_waveform(const char *in_filename, int start_time, int end_time, float* audio_data, int data_num, int *out_data_num);

// 视频重新用x264编码
typedef int(*OnProgress)(void * target, int progress);
int mp4_transcode_by_x264(const char *in_filename, const char *out_filename, int gop_size,OnProgress onProgress, void *target);
// 导入第三方视频
int mp4_import_video_by_x264(const char *in_filename, const char *out_filename, int gop_size, OnProgress onProgress, void* target, int width, int height);

int YY_mp4_video_get_info(const char *in_filename, int *video_duration, int *audio_duration, int *width, int *height);
    
#ifdef __cplusplus
}
#endif


#endif /* YY_remuxer_h */

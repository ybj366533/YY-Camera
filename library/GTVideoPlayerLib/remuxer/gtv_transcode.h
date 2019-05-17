#ifndef TRANSCODE_H
#define TRANSCODE_H

#include "stdint.h"
#include "stdbool.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef int(*OnProgress)(void * target, int progress);
int mp4_video_transcode_video(const char *in_filename, const char *out_filename, int gop_size, OnProgress onProgress, void* target, int width, int height, bool crop_flag, int start_time, int end_time);
int mp4_video_transcode_audio(const char *in_filename, const char *out_filename, bool crop_flag, int start_time, int end_time);

int64_t eee_system_current_milli();

#ifdef __cplusplus
}
#endif

#endif // TRANSCODE_H

#ifndef MP4_REMUX_H
#define MP4_REMUX_H

#ifdef __cplusplus
extern "C" {
#endif

// mp4倒序
// 限制： 全I帧，并且只有视频
int mp4_reverse(const char *filePathFrom, const char *filePathTo);

// 限制 只支持png
// 图片不能太大， moov最大2M
// 问题：如果是大图片要压缩吗？
int mp4_set_cover(const char* filePathFrom, const char *filePathTo, const char *tagFile);

#ifdef __cplusplus
}
#endif

#endif


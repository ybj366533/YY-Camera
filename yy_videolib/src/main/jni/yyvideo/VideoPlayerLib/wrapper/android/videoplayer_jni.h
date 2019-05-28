#ifndef __VIDEO_JNI_H__
#define __VIDEO_JNI_H__

#include <stdio.h>
#include <stdarg.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

void JNI_AttachThread();
void JNI_detachThread();

#ifdef __cplusplus
}
#endif

#endif

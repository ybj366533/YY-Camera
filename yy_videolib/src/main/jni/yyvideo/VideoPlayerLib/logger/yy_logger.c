//
//  YY_logger.c
//  YY
//
//  Created by YY on 2017/4/30.
//  Copyright © 2017年 YY. All rights reserved.
//

#include "stdarg.h"

#include "yy_logger.h"
#include <time.h>
#include <sys/time.h>
#include <string.h>

#if defined(TARGET_OS_IPHONE)
#include "YY_ios_logger.h"
#endif

#if defined(TARGET_OS_ANDROID)
#include "android/plat_log.h"
#endif

#define YY_MAX_PRINT_LEN	2048

static uint8_t _YY_logger_level = YY_LOGDEBUG;
static YY_logger_inject_callback _YY_logger_inj_cb = NULL;

static void afk_logger_get_time(char* str_time, int size)
{
    //time_t now;
	struct timeval tv;
    struct tm *timenow;

    //time(&now);
    gettimeofday(&tv, NULL);
    timenow = localtime(&tv.tv_sec);
    //strftime (str_time,size,"%H:%M:%S",timenow);
    snprintf(str_time,20,"%02d:%02d:%02d.%03d", timenow->tm_hour, timenow->tm_min, timenow->tm_sec, (int)(tv.tv_usec/1000));

    return;
}

static void YY_log_default(int level, const char *format, va_list vl)
{
    char str[YY_MAX_PRINT_LEN]="";
    
    if( level < _YY_logger_level ) {
        return;
    }
    
    char str_time[32];
    afk_logger_get_time(str_time, 32);

    snprintf(str, 32, "%s ", str_time);
    char *str2 = &str[strlen(str)];
    //vsnprintf(str, YY_MAX_PRINT_LEN-1, format, vl);
    vsnprintf(str2, YY_MAX_PRINT_LEN-1 -32, format, vl);
    ////如果不是换行符结尾，则添加一个换行符
    //if(str2[strlen(str2)-1] != '\n')
    //{
    //	int len = strlen(str2);
    //	str2[len] = '\n';
    //	str2[len+1] = '\0';
    //}
    
    if( _YY_logger_inj_cb != NULL ) {
        _YY_logger_inj_cb(str);
        return;
    }
    

    #if defined(TARGET_OS_LINUX)
    //char str_time[32];
    //afk_logger_get_time(str_time, 32);
    printf("%s:%s\n", str_time,str);
    #endif
    
    #if defined(TARGET_OS_IPHONE) || defined(TARGET_IPHONE_SIMULATOR)
    YY_ios_print(str);
    #endif
	
	#if defined(TARGET_OS_ANDROID)
	if(level == YY_LOGDEBUG) {
		LOGD("%s", str);
	} else if (level == YY_LOGWARNING) {
		LOGW("%s", str);
	} else if (level == YY_LOGINFO) {
		LOGI("%s", str);
	} else if (level == YY_LOGERROR) {
		LOGE("%s", str);
	} else if (level == YY_LOGCRIT) {
		LOGE("%s", str);
	}
	#endif
    
    return;
}

void YY_logger_set_level(uint8_t level)
{
    _YY_logger_level = level;
    
    return;
}

void YY_logger_set_callback(YY_logger_inject_callback cb)
{
    _YY_logger_inj_cb = cb;
}

void YY_logger_print(int level, const char * format, ...)
{
    va_list args;
    va_start(args, format);
    YY_log_default(level, format, args);
    va_end(args);
}

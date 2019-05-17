//
//  gtv_logger.c
//  gtv
//
//  Created by gtv on 2017/4/30.
//  Copyright © 2017年 gtv. All rights reserved.
//

#include "stdarg.h"

#include "gtv_logger.h"
#include <time.h>
#include <sys/time.h>
#include <string.h>

#if defined(TARGET_OS_IPHONE)
#include "gtv_ios_logger.h"
#endif

#if defined(TARGET_OS_ANDROID)
#include "android/plat_log.h"
#endif

#define GTV_MAX_PRINT_LEN	2048

static uint8_t _gtv_logger_level = GTV_LOGDEBUG;
static gtv_logger_inject_callback _gtv_logger_inj_cb = NULL;

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

static void gtv_log_default(int level, const char *format, va_list vl)
{
    char str[GTV_MAX_PRINT_LEN]="";
    
    if( level < _gtv_logger_level ) {
        return;
    }
    
    char str_time[32];
    afk_logger_get_time(str_time, 32);

    snprintf(str, 32, "%s ", str_time);
    char *str2 = &str[strlen(str)];
    //vsnprintf(str, GTV_MAX_PRINT_LEN-1, format, vl);
    vsnprintf(str2, GTV_MAX_PRINT_LEN-1 -32, format, vl);
    ////如果不是换行符结尾，则添加一个换行符
    //if(str2[strlen(str2)-1] != '\n')
    //{
    //	int len = strlen(str2);
    //	str2[len] = '\n';
    //	str2[len+1] = '\0';
    //}
    
    if( _gtv_logger_inj_cb != NULL ) {
        _gtv_logger_inj_cb(str);
        return;
    }
    

    #if defined(TARGET_OS_LINUX)
    //char str_time[32];
    //afk_logger_get_time(str_time, 32);
    printf("%s:%s\n", str_time,str);
    #endif
    
    #if defined(TARGET_OS_IPHONE) || defined(TARGET_IPHONE_SIMULATOR)
    gtv_ios_print(str);
    #endif
	
	#if defined(TARGET_OS_ANDROID)
	if(level == GTV_LOGDEBUG) {
		LOGD("%s", str);
	} else if (level == GTV_LOGWARNING) {
		LOGW("%s", str);
	} else if (level == GTV_LOGINFO) {
		LOGI("%s", str);
	} else if (level == GTV_LOGERROR) {
		LOGE("%s", str);
	} else if (level == GTV_LOGCRIT) {
		LOGE("%s", str);
	}
	#endif
    
    return;
}

void gtv_logger_set_level(uint8_t level)
{
    _gtv_logger_level = level;
    
    return;
}

void gtv_logger_set_callback(gtv_logger_inject_callback cb)
{
    _gtv_logger_inj_cb = cb;
}

void gtv_logger_print(int level, const char * format, ...)
{
    va_list args;
    va_start(args, format);
    gtv_log_default(level, format, args);
    va_end(args);
}

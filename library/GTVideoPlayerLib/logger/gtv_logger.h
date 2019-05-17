//
//  gtv_logger.h
//  gtv
//
//  Created by gtv on 2017/4/30.
//  Copyright © 2017年 gtv. All rights reserved.
//

#ifndef gtv_logger_h
#define gtv_logger_h

#include <stdio.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum
{
    GTV_LOGDEBUG = 0,
    GTV_LOGWARNING,
    GTV_LOGINFO,
    GTV_LOGERROR,
    GTV_LOGCRIT
    
} E_GTV_LOGLEVEL;

typedef void (*gtv_logger_inject_callback)(const char *data);

void gtv_logger_set_level(uint8_t level);
void gtv_logger_set_callback(gtv_logger_inject_callback cb);
void gtv_logger_print(int level, const char * format, ...);

#define GTV_PRINT(level, fmt, ...)      do { gtv_logger_print(level, fmt, ##__VA_ARGS__); } while(0);

#define GTV_ERROR(fmt, ...)             GTV_PRINT(GTV_LOGERROR, fmt, ##__VA_ARGS__)
#define GTV_WARN(fmt, ...)              GTV_PRINT(GTV_LOGWARNING, fmt, ##__VA_ARGS__)
#define GTV_INFO(fmt, ...)              GTV_PRINT(GTV_LOGINFO, fmt, ##__VA_ARGS__)
#define GTV_DEBUG(fmt, ...)             GTV_PRINT(GTV_LOGDEBUG, fmt, ##__VA_ARGS__)

#ifdef __cplusplus
}
#endif

#endif /* gtv_logger_h */

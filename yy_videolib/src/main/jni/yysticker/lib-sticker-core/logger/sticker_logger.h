//
//  YY_logger.h
//  YY
//
//  Created by YY on 2017/4/30.
//  Copyright © 2017年 YY. All rights reserved.
//

#ifndef sticker_logger_h
#define sticker_logger_h

#include <stdio.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum
{
    STICKER_LOGDEBUG = 0,
    STICKER_LOGWARNING,
    STICKER_LOGINFO,
    STICKER_LOGERROR
    
} E_STICKER_LOGLEVEL;

typedef void (*sticker_logger_inject_callback)(const char *data);

void sticker_logger_set_level(uint8_t level);
void sticker_logger_set_callback(sticker_logger_inject_callback cb);
void sticker_logger_print(int level, const char * format, ...);

#define STICKER_PRINT(level, fmt, ...)      do { sticker_logger_print(level, fmt, ##__VA_ARGS__); } while(0);

#define STICKER_ERROR(fmt, ...)             STICKER_PRINT(STICKER_LOGERROR, fmt, ##__VA_ARGS__)
#define STICKER_WARN(fmt, ...)              STICKER_PRINT(STICKER_LOGWARNING, fmt, ##__VA_ARGS__)
#define STICKER_INFO(fmt, ...)              STICKER_PRINT(STICKER_LOGINFO, fmt, ##__VA_ARGS__)
#define STICKER_DEBUG(fmt, ...)             STICKER_PRINT(STICKER_LOGDEBUG, fmt, ##__VA_ARGS__)

#ifdef __cplusplus
}
#endif

#endif /* sticker_logger_h */

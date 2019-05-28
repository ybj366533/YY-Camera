//
//  YY_logger.h
//  YY
//
//  Created by YY on 2017/4/30.
//  Copyright © 2017年 YY. All rights reserved.
//

#ifndef YY_logger_h
#define YY_logger_h

#include <stdio.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum
{
    YY_LOGDEBUG = 0,
    YY_LOGWARNING,
    YY_LOGINFO,
    YY_LOGERROR,
    YY_LOGCRIT
    
} E_YY_LOGLEVEL;

typedef void (*YY_logger_inject_callback)(const char *data);

void YY_logger_set_level(uint8_t level);
void YY_logger_set_callback(YY_logger_inject_callback cb);
void YY_logger_print(int level, const char * format, ...);

#define YY_PRINT(level, fmt, ...)      do { YY_logger_print(level, fmt, ##__VA_ARGS__); } while(0);

#define YY_ERROR(fmt, ...)             YY_PRINT(YY_LOGERROR, fmt, ##__VA_ARGS__)
#define YY_WARN(fmt, ...)              YY_PRINT(YY_LOGWARNING, fmt, ##__VA_ARGS__)
#define YY_INFO(fmt, ...)              YY_PRINT(YY_LOGINFO, fmt, ##__VA_ARGS__)
#define YY_DEBUG(fmt, ...)             YY_PRINT(YY_LOGDEBUG, fmt, ##__VA_ARGS__)

#ifdef __cplusplus
}
#endif

#endif /* YY_logger_h */

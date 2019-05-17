//
//  gtv_logger.h
//  gtv
//
//  Created by gtv on 2017/4/30.
//  Copyright © 2017年 gtv. All rights reserved.
//

#ifndef gtv_sticker_logger_h
#define gtv_sticker_logger_h

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

typedef void (*gtv_sticker_logger_inject_callback)(const char *data);

void gtv_sticker_logger_set_level(uint8_t level);
void gtv_sticker_logger_set_callback(gtv_sticker_logger_inject_callback cb);
void gtv_sticker_logger_print(int level, const char * format, ...);

#define STICKER_PRINT(level, fmt, ...)      do { gtv_sticker_logger_print(level, fmt, ##__VA_ARGS__); } while(0);

#define STICKER_ERROR(fmt, ...)             STICKER_PRINT(STICKER_LOGERROR, fmt, ##__VA_ARGS__)
#define STICKER_WARN(fmt, ...)              STICKER_PRINT(STICKER_LOGWARNING, fmt, ##__VA_ARGS__)
#define STICKER_INFO(fmt, ...)              STICKER_PRINT(STICKER_LOGINFO, fmt, ##__VA_ARGS__)
#define STICKER_DEBUG(fmt, ...)             STICKER_PRINT(STICKER_LOGDEBUG, fmt, ##__VA_ARGS__)

#ifdef __cplusplus
}
#endif

#endif /* gtv_sticker_logger_h */

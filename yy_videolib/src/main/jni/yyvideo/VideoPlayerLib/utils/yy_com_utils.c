//
//  Created by YY on 2017/5/1.
//  Copyright © 2017年 YY. All rights reserved.
//

#include "stdio.h"
#include "stdlib.h"
#include "string.h"
#include "pthread.h"
#include "unistd.h"
#include <sys/errno.h>
#include <sys/time.h>

#include "yy_com_utils.h"

#include <libavutil/imgutils.h>
#include <libavutil/samplefmt.h>
#include <libavutil/timestamp.h>
#include <libavutil/time.h>
#include <libavformat/avformat.h>

void YY_milliseconds_sleep(int32_t mSec)
{
//    usleep(mSec*1000);
//    struct timeval tv;
//
//    tv.tv_sec = mSec/1000;
//    tv.tv_usec = (mSec%1000)*1000;
//
//    do {
//        select(0,NULL,NULL,NULL,&tv);
//    } while(0);
    av_usleep(mSec * 1000);
}

void YY_useconds_sleep(int32_t usec)
{
    av_usleep(usec);
//    struct timespec ts = { usec / 1000000, usec % 1000000 * 1000 };
//    while (nanosleep(&ts, &ts) < 0 && errno == EINTR);
}

void YY_dump_binary(uint8_t * p, int len)
{
    int i=0;
    
    for( i=0; i<len; i++ ) {
        printf("%02x", p[i]);
    }
    printf("\n");
}

int64_t YY_system_current_milli()
{
    int64_t milli = 0;
    
    struct timeval    tp;
    
    gettimeofday(&tp, NULL);
    
	//milli = tp.tv_sec * 1000 + tp.tv_usec/1000;
	milli = tp.tv_sec * (int64_t)1000 + tp.tv_usec/1000;
    
    return milli;
}

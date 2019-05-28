//  Created by YY on 2017/6/24.
//  Copyright © 2017年 YY. All rights reserved.
//

#include "yy_data_buffer.h"
#include "stdlib.h"
#include <sys/time.h>
#include <string.h>

void YY_databuffer_init(ST_YY_DATA_BUFFER * buf, int size)
{
    pthread_mutex_init(&buf->mutex, NULL);
    pthread_cond_init(&buf->cond, NULL);
    
    buf->read_pos = 0;
    buf->write_pos = 1;
    
    buf->buffer = malloc(size+1);
    buf->size = size;
    
    return;
}

void YY_databuffer_destroy(ST_YY_DATA_BUFFER * buf)
{
    pthread_mutex_destroy(&buf->mutex);
    pthread_cond_destroy(&buf->cond);
    
    free(buf->buffer);
    
    return;
}

int YY_databuffer_wait(ST_YY_DATA_BUFFER * buf, int timeout)
{
    int               rc,tim_sec,tim_milli;
    struct timespec   ts;
    struct timeval    tp;
    
    tim_sec = timeout/1000;
    tim_milli = timeout - tim_sec*1000;
    
    gettimeofday(&tp, NULL);
    ts.tv_sec  = tp.tv_sec;
    ts.tv_nsec = tp.tv_usec * 1000;
    
    if( ts.tv_nsec > (1000*1000*1000 - tim_milli * 1000 * 1000) ) {
        ts.tv_sec = ts.tv_sec + (tim_sec+1);
        ts.tv_nsec = ts.tv_nsec + tim_milli * 1000 * 1000 - 1000 * 1000 * 1000;
    }
    else {
        ts.tv_sec += tim_sec;
        ts.tv_nsec += tim_milli * 1000 * 1000;
    }
    
    pthread_mutex_lock(&buf->mutex);
    
    rc = pthread_cond_timedwait(&buf->cond, &buf->mutex, &ts);
    if (rc == -1) {
        pthread_mutex_unlock(&buf->mutex);
        return -1;
    }
    
    pthread_mutex_unlock(&buf->mutex);
    
    return 0;
}

void YY_databuffer_dump(ST_YY_DATA_BUFFER * buf, char * tag)
{
    int i=0;
    printf("%s $$$ ", tag);
    for( i=0; i<buf->size+1; i++ ) {
        printf("%d ", buf->buffer[i]);
    }
    printf("$$$ %d %d \n", buf->write_pos, buf->read_pos);
    
    return;
}

int YY_databuffer_datasize(ST_YY_DATA_BUFFER * buf)
{
    int s = 0;
    int space = 0;
    
    space = YY_databuffer_space(buf);
    s = buf->size - space - 1;
    
    return s;
}

int YY_databuffer_space(ST_YY_DATA_BUFFER * buf)
{
    int empty = 0;
    
    pthread_mutex_lock(&buf->mutex);
    if( buf->write_pos < buf->read_pos ) {
        empty = buf->read_pos - buf->write_pos - 1;
    }
    else {
        empty = buf->read_pos + buf->size - buf->write_pos;
    }
    pthread_mutex_unlock(&buf->mutex);
    
    return empty;
}

int YY_databuffer_clear(ST_YY_DATA_BUFFER * buf)
{
    int empty = 0;
    
    pthread_mutex_lock(&buf->mutex);
    buf->read_pos = 0;
    buf->write_pos = 1;
    pthread_mutex_unlock(&buf->mutex);
    
    return empty;
}

// 返回put成功的字节(TODO:wait_flg=1的时候有bug)
int YY_databuffer_put(ST_YY_DATA_BUFFER * buf, uint8_t * input_buf, int input_len, int wait_flg)
{
    int left = input_len;
    uint8_t * src = input_buf;
    int empty = 0;
    int wait_cnt = 0;
    
    // write_pos == read_pos 的时候代表初始状态
    // write_pos + 1 == read_pos的时候代表满
    // read_pos + 1 == write_pos的时候代表空
    // 读数据从 read_pos + 1 开始读取
    // 写数据从 write_pos 开始写
    if( wait_flg > 0 ) {
        wait_cnt = 0;
    }
    else {
        wait_cnt = 3;
    }
    while(left > 0)
    {
        // 先尝试放数据
        pthread_mutex_lock(&buf->mutex);
//        YY_databuffer_dump(buf, "PUT");
        // 计算剩余空间
        if( buf->write_pos < buf->read_pos ) {
            empty = buf->read_pos - buf->write_pos - 1;
        }
        else {
            empty = buf->read_pos + buf->size - buf->write_pos;
        }
        
        if( empty > 0 ) {
            
            if( empty > left ) {
                empty = left;
            }
            
            // 实际缓冲区比size大1
            if( buf->write_pos + empty <= buf->size + 1 ) {
                
                memcpy(&(buf->buffer[buf->write_pos]), src, empty);
            }
            else {
                // 分两段copy
                int step = buf->size+1-buf->write_pos;
                
                memcpy(&(buf->buffer[buf->write_pos]), src, step);
                
                memcpy(&(buf->buffer[0]), src+step, empty-step);
            }
            buf->write_pos = (buf->write_pos + empty)% (buf->size+1);
            
            src += empty;
            left -= empty;
        }
//        YY_databuffer_dump(buf, "XXX");
        pthread_mutex_unlock(&buf->mutex);
        
        if( left > 0 && wait_cnt < 3 ) {
            // 尝试等待
            YY_databuffer_wait(buf, 1000);
        }
        
        // 最多等3秒
        wait_cnt ++;
        if( wait_cnt > 3 ) {
            break;
        }
    }
    
    return (input_len - left);
}

// 返回get成功的字节
int YY_databuffer_get(ST_YY_DATA_BUFFER * buf, uint8_t * out_buf, int out_len)
{
    uint8_t * dst = out_buf;
    
    int ready = 0;
    int real = 0;
    
    pthread_mutex_lock(&buf->mutex);
    //YY_databuffer_dump(buf, "GBEF");
    // 检测buf中有多少剩余数据可读
    if( buf->write_pos > buf->read_pos ) {
        ready = buf->write_pos - buf->read_pos - 1;
    }
    else {
        ready = buf->write_pos + buf->size - buf->read_pos;
    }
    
    if( ready > 0 ) {
        
        real = ready < out_len ? ready : out_len;
        
        // 实际缓冲区比size大1，但是read是从read_pos+1开始读取，所以是<号
        if( buf->read_pos + real < buf->size + 1 ) {
            
            memcpy(dst, &(buf->buffer[buf->read_pos+1]), real);
        }
        else {
            // 分两段copy
            int step = buf->size-buf->read_pos;
            
            memcpy(dst, &(buf->buffer[buf->read_pos+1]), step);
            
            memcpy(dst+step, &(buf->buffer[0]), real-step);
        }
        buf->read_pos = (buf->read_pos + real) % (buf->size+1);
        
        pthread_cond_signal(&buf->cond); // 通知相关线程
    }
//    printf("GAFT $$$ %d %d %d \n", buf->write_pos, buf->read_pos, real);
//
//    for( int i=0; i<real; i++ ) {
//        printf("%d ", out_buf[i]);
//    }
//    printf("\n");
    
    //YY_databuffer_dump(buf, "GAFT");
    pthread_mutex_unlock(&buf->mutex);
    
    return real;
    
}

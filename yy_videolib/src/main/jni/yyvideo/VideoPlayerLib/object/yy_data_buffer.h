//  Created by YY on 2017/6/24.
//  Copyright © 2017年 YY. All rights reserved.
//

#ifndef yy_data_buffer_h
#define yy_data_buffer_h

#include <stdint.h>
#include <pthread.h>
#include <stdio.h>

typedef struct {
    
    int write_pos;
    int read_pos;
    int size;
    
    uint8_t * buffer;
    
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    
} ST_YY_DATA_BUFFER;

void YY_databuffer_init(ST_YY_DATA_BUFFER * buf, int size);
void YY_databuffer_destroy(ST_YY_DATA_BUFFER * buf);

// 返回put成功的字节
int YY_databuffer_put(ST_YY_DATA_BUFFER * buf, uint8_t * input_buf, int input_len, int wait_flg);
// 返回get成功的字节
int YY_databuffer_get(ST_YY_DATA_BUFFER * buf, uint8_t * out_buf, int out_len);
// 返回空间大小
int YY_databuffer_space(ST_YY_DATA_BUFFER * buf);
// 返回已缓存数据大小
int YY_databuffer_datasize(ST_YY_DATA_BUFFER * buf);

// 清除缓存数据
int YY_databuffer_clear(ST_YY_DATA_BUFFER * buf);

#endif /* YY_data_buffer_h */

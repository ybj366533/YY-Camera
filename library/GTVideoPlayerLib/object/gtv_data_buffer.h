//
//  gtv_data_buffer.h
//  MixMultiPlayer
//
//  Created by gtv on 2017/6/24.
//  Copyright © 2017年 gtv. All rights reserved.
//

#ifndef gtv_data_buffer_h
#define gtv_data_buffer_h

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
    
} ST_GTV_DATA_BUFFER;

void gtv_databuffer_init(ST_GTV_DATA_BUFFER * buf, int size);
void gtv_databuffer_destroy(ST_GTV_DATA_BUFFER * buf);

// 返回put成功的字节
int gtv_databuffer_put(ST_GTV_DATA_BUFFER * buf, uint8_t * input_buf, int input_len, int wait_flg);
// 返回get成功的字节
int gtv_databuffer_get(ST_GTV_DATA_BUFFER * buf, uint8_t * out_buf, int out_len);
// 返回空间大小
int gtv_databuffer_space(ST_GTV_DATA_BUFFER * buf);
// 返回已缓存数据大小
int gtv_databuffer_datasize(ST_GTV_DATA_BUFFER * buf);

// 清除缓存数据
int gtv_databuffer_clear(ST_GTV_DATA_BUFFER * buf);

#endif /* gtv_data_buffer_h */

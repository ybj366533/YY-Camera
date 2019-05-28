//  Created by YY on 2017/4/30.
//  Copyright © 2017年 YY. All rights reserved.
//

#ifndef yy_data_queue_h
#define yy_data_queue_h

#include <stdint.h>
#include <pthread.h>
#include <stdio.h>

typedef struct ST_YY_QUEUE_ELEM_T {
    
    void *  data;
    int     extra;
    
    struct ST_YY_QUEUE_ELEM_T * prev;
    struct ST_YY_QUEUE_ELEM_T * next;
    
} ST_YY_QUEUE_ELEM;

typedef struct {
    
    int count;
    
    ST_YY_QUEUE_ELEM * head;
    ST_YY_QUEUE_ELEM * tail;
    
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    
} ST_YY_DATA_QUEUE;

void YY_dataqueue_init(ST_YY_DATA_QUEUE * queue);
void YY_dataqueue_destroy(ST_YY_DATA_QUEUE * queue, int forceFree);
void YY_dataqueue_put(ST_YY_DATA_QUEUE * queue, void * data, int extra);
void * YY_dataqueue_get(ST_YY_DATA_QUEUE * queue, int * extra);
void * YY_dataqueue_peek_first(ST_YY_DATA_QUEUE * queue, int * extra);
void * YY_dataqueue_peek_last(ST_YY_DATA_QUEUE * queue, int * extra);
int YY_dataqueue_wait(ST_YY_DATA_QUEUE * queue, int timeout);

#endif /* YY_data_queue_h */

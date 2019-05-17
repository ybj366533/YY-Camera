//
//  gtv_data_queue.h
//  gtv
//
//  Created by gtv on 2017/4/30.
//  Copyright © 2017年 gtv. All rights reserved.
//

#ifndef gtv_data_queue_h
#define gtv_data_queue_h

#include <stdint.h>
#include <pthread.h>
#include <stdio.h>

typedef struct ST_GTV_QUEUE_ELEM_T {
    
    void *  data;
    int     extra;
    
    struct ST_GTV_QUEUE_ELEM_T * prev;
    struct ST_GTV_QUEUE_ELEM_T * next;
    
} ST_GTV_QUEUE_ELEM;

typedef struct {
    
    int count;
    
    ST_GTV_QUEUE_ELEM * head;
    ST_GTV_QUEUE_ELEM * tail;
    
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    
} ST_GTV_DATA_QUEUE;

void gtv_dataqueue_init(ST_GTV_DATA_QUEUE * queue);
void gtv_dataqueue_destroy(ST_GTV_DATA_QUEUE * queue, int forceFree);
void gtv_dataqueue_put(ST_GTV_DATA_QUEUE * queue, void * data, int extra);
void * gtv_dataqueue_get(ST_GTV_DATA_QUEUE * queue, int * extra);
void * gtv_dataqueue_peek_first(ST_GTV_DATA_QUEUE * queue, int * extra);
void * gtv_dataqueue_peek_last(ST_GTV_DATA_QUEUE * queue, int * extra);
int gtv_dataqueue_wait(ST_GTV_DATA_QUEUE * queue, int timeout);

#endif /* gtv_data_queue_h */

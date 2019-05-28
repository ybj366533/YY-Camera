//  Created by YY on 2017/4/30.
//  Copyright © 2017年 YY. All rights reserved.
//

#include "yy_data_queue.h"
#include "stdlib.h"
#include <sys/time.h>

void YY_dataqueue_init(ST_YY_DATA_QUEUE * queue)
{
    queue->count = 0;
    
    queue->head = queue->tail = NULL;
    
    pthread_mutex_init(&queue->mutex, NULL);
    pthread_cond_init(&queue->cond, NULL);
    
    return;
}

void YY_dataqueue_destroy(ST_YY_DATA_QUEUE * queue, int forceFree)
{
    ST_YY_QUEUE_ELEM * elem;
    
    while (queue->head != NULL) {
        
        elem = queue->head;
        
        if( elem->data != NULL && forceFree > 0 ) {
            
            free(elem->data);
            elem->data = NULL;
        }
        
        queue->head = elem->next;
        
        free(elem);
    }
    
    pthread_mutex_destroy(&queue->mutex);
    pthread_cond_destroy(&queue->cond);
    
    return;
}

void YY_dataqueue_put(ST_YY_DATA_QUEUE * queue, void * data, int extra)
{
    // 加入队列尾部
    ST_YY_QUEUE_ELEM * elem;
    
    elem = (ST_YY_QUEUE_ELEM*) malloc(sizeof(ST_YY_QUEUE_ELEM));
    if( elem == NULL ) {
        return;
    }
    
    pthread_mutex_lock(&queue->mutex);
    
    elem->data = data;
    elem->extra = extra;
    
    if( queue->count == 0 ) {
        
        queue->head = elem;
        queue->tail = elem;
        
        elem->prev = NULL;
        elem->next = NULL;
    }
    else {
        
        queue->tail->next = elem;
        elem->prev = queue->tail;
        elem->next = NULL;
        
        queue->tail = elem;
    }
    
    queue->count ++;
    
    pthread_cond_signal(&queue->cond); // 通知相关线程
    
    pthread_mutex_unlock(&queue->mutex);
    
    return;
}

void * YY_dataqueue_peek_first(ST_YY_DATA_QUEUE * queue, int * extra)
{
    // 从队列头部获取数据
    void * data = NULL;
    ST_YY_QUEUE_ELEM * elem;
    
    pthread_mutex_lock(&queue->mutex);
    
    elem = queue->head;
    
    if( elem != NULL ) {
        
        data = elem->data;
        *extra = elem->extra;
    }
    
    pthread_mutex_unlock(&queue->mutex);
    
    return data;
}

void * YY_dataqueue_peek_last(ST_YY_DATA_QUEUE * queue, int * extra)
{
    // 从队列尾部获取数据
    void * data = NULL;
    ST_YY_QUEUE_ELEM * elem;
    
    pthread_mutex_lock(&queue->mutex);
    
    elem = queue->tail;
    
    if( elem != NULL ) {
        
        data = elem->data;
        *extra = elem->extra;
    }
    
    pthread_mutex_unlock(&queue->mutex);
    
    return data;
}

void * YY_dataqueue_get(ST_YY_DATA_QUEUE * queue, int * extra)
{
    // 从队列头部获取数据
    void * data = NULL;
    ST_YY_QUEUE_ELEM * elem;
    
    pthread_mutex_lock(&queue->mutex);
    
    elem = queue->head;
    
    if( elem != NULL ) {
        
        if( queue->head == queue->tail ) {
            
            queue->head = queue->tail = NULL;
        }
        else {
            
            queue->head = elem->next;
            
            if( queue->head != NULL ) {
                
                queue->head->prev = NULL;
            }
        }
        
        queue->count --;
        
        data = elem->data;
        *extra = elem->extra;
        
        free(elem);
    }
    
    pthread_mutex_unlock(&queue->mutex);
    
    return data;
}

int YY_dataqueue_wait(ST_YY_DATA_QUEUE * queue, int timeout)
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
    
    pthread_mutex_lock(&queue->mutex);
    
    rc = pthread_cond_timedwait(&queue->cond, &queue->mutex, &ts);
    if (rc == -1) {
        pthread_mutex_unlock(&queue->mutex);
        return -1;
    }
    
    pthread_mutex_unlock(&queue->mutex);
    
    return 0;
}

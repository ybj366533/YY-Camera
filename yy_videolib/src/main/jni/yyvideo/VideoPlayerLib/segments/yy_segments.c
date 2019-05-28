//
//  Created by YY on 2018/1/26.
//  Copyright © 2018年 YY. All rights reserved.
//

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "yy_segments.h"
#include "yy_logger.h"

static int _segrec_readline(char * line, char * name, int * dur, float * speed)
{
    int len = 0;
    int i = 0;
    char * result = NULL;
    char * value[4] = { 0, 0, 0, 0 };
    
    len = (int)strlen(line);
    while(len > 0 && line[len-1] == '\n') {
        line[len-1] = 0x00;
        len --;
    }
    if( len > 5 && line[0] == '#' && line[len-1] == '@' ) {
        
        line[0] = line[len-1] = 0x00;
        
        i ++;
        value[0] = &line[i];
        
        while(i<4) {
            
            result = strstr(value[i-1], ",");
            if( result != NULL ) {
                *result = 0x00;
                value[i] = result + 1;
            }
            
            i++;
        }
        
        if( value[0] != NULL && value[1] != NULL && value[2] != NULL && value[3] != NULL ) {
            YY_DEBUG( "result is %s - %s - %s - %s \n", value[0], value[1], value[2], value[3] );
            strncpy(name, value[1], D_SEG_FILE_LEN-1);
            *dur = atoi(value[2]);
            *speed = atof(value[3]);
            return 1;
        }
        else {
            return 0;
        }
    }
    else {
        return 0;
    }
}

static int _segrec_syncfile(HANDLE_YY_SEGMENTS_REC handle)
{
    FILE * fp = NULL;
    char line[D_SEG_FILE_LEN*2];
    int i=0;
    
    fp = fopen(handle->path, "w");
    if( fp == NULL ) {
        YY_ERROR("open file-w err %s", handle->path);
        return 0;
    }
    
    for( i=0; i<handle->count; i++ ) {
        memset(line, 0x00, sizeof(line));
        snprintf(line, D_SEG_FILE_LEN*2-1, "#1,%s,%d,%.2f@\n", handle->data[i].name, handle->data[i].duration, handle->data[i].speed);
        fputs(line, fp);
    }

    fclose(fp);
    
    return i;
}

// 打开分段管理文件
HANDLE_YY_SEGMENTS_REC YY_segrec_open(const char * path)
{
    FILE * fp = NULL;
    HANDLE_YY_SEGMENTS_REC h = NULL;
    char line[D_SEG_FILE_LEN*2];
    
    if( path == NULL ) {
        return NULL;
    }
    YY_INFO("YY_segrec_open %s %d %d ", path, strlen(path), sizeof(ST_YY_SEGMENTS_REC));
    
    h = (HANDLE_YY_SEGMENTS_REC)malloc(sizeof(ST_YY_SEGMENTS_REC));
    memset((uint8_t*)h, 0x00, sizeof(ST_YY_SEGMENTS_REC));
    strncpy(&(h->path[0]), path, D_SEG_PATH_LEN-1);
    
    fp = fopen(path, "r");
    if( fp == NULL ) {
        // 没有现成的文件，生成一个空数据
        YY_INFO("YY_segrec_open first time %llx %d ", (uint64_t)h, h->count);
        return h;
    }
    
    // 逐行读取数据
    // 每行数据格式 #1,01.mp4,1234@
    while(!feof(fp)) {
        
        memset(line, 0x00, sizeof(line));
        fgets(line, sizeof(line), fp);
        
        YY_DEBUG("line:%s", line);
        if( _segrec_readline(line, h->data[h->count].name, &(h->data[h->count].duration), &(h->data[h->count].speed)) > 0 ) {
            h->data[h->count].valid = 1;
            h->count ++;
        }
    }
    YY_INFO("YY_segrec_open finished %llx %d ", (uint64_t)h, h->count);
    fclose(fp);
    
    return h;
}

// 返回分段数量
int YY_segrec_get_count(HANDLE_YY_SEGMENTS_REC handle)
{
    if( handle == NULL ) {
        return 0;
    }
    
    return handle->count;
}

// 返回指定分段的视频名和时长
int YY_segrec_get_video(HANDLE_YY_SEGMENTS_REC handle, int index, char * name, int * duration, float * speed)
{
    if( handle == NULL ) {
        return 0;
    }
    
    if( index >= handle->count ) {
        return 0;
    }
    
    strncpy(name, handle->data[index].name, D_SEG_FILE_LEN-1);
    *duration = handle->data[index].duration;
    *speed = handle->data[index].speed;
    
    return 1;
}

// 添加分段视频
int YY_segrec_add_video(HANDLE_YY_SEGMENTS_REC handle, const char * fname, int duration, float speed)
{
    if( handle == NULL ) {
        return 0;
    }
    
    if( handle->count >= D_SEG_LIMIT_COUNT ) {
        return 0;
    }
    
    memset(handle->data[handle->count].name, 0x00, sizeof(handle->data[handle->count].name));
    strncpy(handle->data[handle->count].name, fname, D_SEG_FILE_LEN-1);
    handle->data[handle->count].duration = duration;
    handle->data[handle->count].speed = speed;
    handle->data[handle->count].valid = 1;
    
    handle->count ++;
    
    // sync file
    _segrec_syncfile(handle);
    
    return 1;
}

// 删除最后一个分段的视频
int YY_segrec_remove_last(HANDLE_YY_SEGMENTS_REC handle)
{
    if( handle == NULL ) {
        return 0;
    }
    
    if( handle->count >= D_SEG_LIMIT_COUNT || handle->count <= 0 ) {
        return 0;
    }
    
    handle->count --;
    
    // sync file
    _segrec_syncfile(handle);
    
    return 1;
}

// 关闭分段管理
void YY_segrec_close(HANDLE_YY_SEGMENTS_REC handle)
{
    // sync file
    _segrec_syncfile(handle);
    
    free(handle);
    
    return;
}


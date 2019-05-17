//
//  gtv_com_utils.h
//  gtv
//
//  Created by gtv on 2017/5/1.
//  Copyright © 2017年 gtv. All rights reserved.
//

#ifndef gtv_com_utils_h
#define gtv_com_utils_h

#include <stdio.h>
#include <stdint.h>

void gtv_milliseconds_sleep(int32_t mSec);
void gtv_dump_binary(uint8_t * p, int len);
int64_t gtv_system_current_milli();
void gtv_useconds_sleep(int32_t usec);

#endif /* gtv_com_utils_h */

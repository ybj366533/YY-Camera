//
//  Created by YY on 2017/5/1.
//  Copyright © 2017年 YY. All rights reserved.
//

#ifndef yy_com_utils_h
#define yy_com_utils_h

#include <stdio.h>
#include <stdint.h>

void YY_milliseconds_sleep(int32_t mSec);
void YY_dump_binary(uint8_t * p, int len);
int64_t YY_system_current_milli();
void YY_useconds_sleep(int32_t usec);

#endif /* YY_com_utils_h */

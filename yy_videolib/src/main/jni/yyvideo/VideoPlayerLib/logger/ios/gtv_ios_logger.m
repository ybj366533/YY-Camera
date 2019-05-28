//
//  YY_ios_logger.c
//  YY
//
//  Created by YY on 2017/5/2.
//  Copyright © 2017年 YY. All rights reserved.
//

#import <Foundation/Foundation.h>
#include "YY_ios_logger.h"

void YY_ios_print(const char * str)
{
    NSLog(@"%s", str);
}

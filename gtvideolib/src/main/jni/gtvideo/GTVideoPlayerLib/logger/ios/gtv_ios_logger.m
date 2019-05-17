//
//  gtv_ios_logger.c
//  gtv
//
//  Created by gtv on 2017/5/2.
//  Copyright © 2017年 gtv. All rights reserved.
//

#import <Foundation/Foundation.h>
#include "gtv_ios_logger.h"

void gtv_ios_print(const char * str)
{
    NSLog(@"%s", str);
}

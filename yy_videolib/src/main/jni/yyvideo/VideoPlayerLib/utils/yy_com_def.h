//
//  Created by YY on 2017/4/30.
//  Copyright © 2017年 YY. All rights reserved.
//

#ifndef yy_com_def_h
#define yy_com_def_h

#define YY_STREAM_STS_INVALID          (-1)
#define YY_STREAM_STS_RUNNING          (-2)
#define YY_STREAM_STS_CLOSED           (-3)

#define YY_COM_URL_LIMIT_LEN               (1024)
#define YY_SPSPPS_DATA_LEN                 (128)

#define D_YY_COM_FLAG_ON                   (1)
#define D_YY_COM_FLAG_OFF                  (0)

#define D_YY_AUDIO_QUEUE_OVERFLOW_MILLI    (5000)
#define D_YY_AUDIO_QUEUE_LIMIT             (400)

#define DJNALIGN(x, align) ((( x ) + (align) - 1) / (align) * (align))

#endif /* YY_com_def_h */

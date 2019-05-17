//
//  gtv_com_def.h
//  gtv
//
//  Created by gtv on 2017/4/30.
//  Copyright © 2017年 gtv. All rights reserved.
//

#ifndef gtv_com_def_h
#define gtv_com_def_h

#define GTV_STREAM_STS_INVALID          (-1)
#define GTV_STREAM_STS_RUNNING          (-2)
#define GTV_STREAM_STS_CLOSED           (-3)

#define GTV_COM_URL_LIMIT_LEN               (1024)
#define GTV_SPSPPS_DATA_LEN                 (128)

#define D_GTV_COM_FLAG_ON                   (1)
#define D_GTV_COM_FLAG_OFF                  (0)

#define D_GTV_AUDIO_QUEUE_OVERFLOW_MILLI    (5000)
#define D_GTV_AUDIO_QUEUE_LIMIT             (400)

#define DJNALIGN(x, align) ((( x ) + (align) - 1) / (align) * (align))

#endif /* gtv_com_def_h */

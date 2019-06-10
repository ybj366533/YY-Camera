package com.ybj366533.yy_camera;


interface Constants {

    AspectRatio DEFAULT_ASPECT_RATIO = AspectRatio.of(16, 9);

    int FACING_BACK = 0;
    int FACING_FRONT = 1;

    int FLASH_OFF = 0;
    int FLASH_ON = 1;
    int FLASH_TORCH = 2;
    int FLASH_AUTO = 3;
    int FLASH_RED_EYE = 4;

    int LANDSCAPE_90 = 90;
    int LANDSCAPE_270 = 270;

    int MAX_PREVIEW_WIDTH_1280 = 1280;
    int MAX_PREVIEW_HEIGHT_720 = 720;
}


#include <assert.h>
#include <string.h>
#include <pthread.h>
#include <jni.h>
#include <GLES2/gl2.h>

#include <iostream>

#include "plat_log.h"
#include "SdmTracker.h"

#include "matrix.h"
#include "sticker_player.hpp"

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

#define JNI_CLASS_MGSTREAMER        "com/ybj366533/videolib/impl/tracker/EffectTracker"

//static int logCount = 0;
//static int LOG_COUNT_MAX = 100;     // 一次直播输出的最大日志数

static JavaVM *g_jvm = NULL;

typedef struct vkrtmp_fields_t {

    jclass clazz;

} vkrtmp_fields_t;

static vkrtmp_fields_t g_clazz;

static JNIEnv *getJNIEnv(int *needsDetach) {
    JNIEnv *env = NULL;

    if ((g_jvm)->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {

        int status = (g_jvm)->AttachCurrentThread(&env, 0);

        if (status < 0) {
            LOGE("failed to attach current thread");
            return NULL;
        }

        *needsDetach = 1;
    }

    return env;
}

typedef struct {

    uint8_t magic_s[4];
    SdmTracker *sdm_tracker;
    gst::FramePlayer *frame_player;
    uint8_t magic_e[4];

} SDM_TRACKER_WRAPPER;

static int wrapper_is_valid(SDM_TRACKER_WRAPPER *wrapper) {
    if (wrapper == NULL)
        return 0;

    if (wrapper->magic_s[0] != 0xFF || wrapper->magic_s[1] != 0xFF || wrapper->magic_s[2] != 0xFF ||
        wrapper->magic_s[3] != 0xFF) {
        return 0;
    }

    if (wrapper->magic_e[0] != 0x55 || wrapper->magic_e[1] != 0x55 || wrapper->magic_e[2] != 0x55 ||
        wrapper->magic_e[3] != 0x55) {
        return 0;
    }

    return 1;
}

static void wrapper_magic_init(SDM_TRACKER_WRAPPER *wrapper) {
    if (wrapper == NULL)
        return;

    wrapper->magic_s[0] = 0xFF;
    wrapper->magic_s[1] = 0xFF;
    wrapper->magic_s[2] = 0xFF;
    wrapper->magic_s[3] = 0xFF;

    wrapper->magic_e[0] = 0x55;
    wrapper->magic_e[1] = 0x55;
    wrapper->magic_e[2] = 0x55;
    wrapper->magic_e[3] = 0x55;

    return;
}

//#ifdef __cplusplus
//extern "C" {
//#endif

static long long
SDMTracker_createTrackerJNI(JNIEnv *env, jobject thiz) {
    SDM_TRACKER_WRAPPER *wrapper = NULL;

    LOGE("SDMTracker_createTrackerJNI started ... \n");

    wrapper = (SDM_TRACKER_WRAPPER *) malloc(sizeof(SDM_TRACKER_WRAPPER));
    memset(wrapper, 0x00, sizeof(SDM_TRACKER_WRAPPER));

    wrapper_magic_init(wrapper);

    wrapper->sdm_tracker = new SdmTracker();
    wrapper->frame_player = new gst::FramePlayer();

    LOGE("SDMTracker_createTrackerJNI finished ... (%08ld) \n", (long) wrapper);

    return (long long) wrapper;
}

static int
SDMTracker_loadModelJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jstring filename) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    if (wrapper == NULL) {
        LOGE("SDMTracker_loadModelJNI addr is null.\n");
        return -99;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_loadModelJNI invalid wrapper !!!!");
        return -98;
    }

    const char *c_name = NULL;

    c_name = env->GetStringUTFChars(filename, NULL);

    if (c_name == NULL) {
        LOGE("SDMTracker_loadModelJNI params check ng (filename) ... \n");
        return -88;
    }

    LOGE("SDMTracker_loadModelJNI started %s ... \n", c_name);

    wrapper->sdm_tracker->loadModel((char *) c_name);

    if (c_name) {
        env->ReleaseStringUTFChars(filename, c_name);
    }

    return 0;
}

static void
SDMTracker_getGLESImageVertexJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jfloat centerX,
                                 jfloat centerY, jfloat angle, jfloat canvasWidth,
                                 jfloat canvasHeight, jfloatArray jout) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    uint8_t *in_data = NULL;
    float *out_data = NULL;

    if (wrapper == NULL) {
        LOGE("SDMTracker_getGLESImageVertexJNI addr is null.\n");
        return;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_getGLESImageVertexJNI invalid wrapper !!!!");
        return;
    }

    out_data = (float *) (env->GetFloatArrayElements(jout, 0));
    if (jout == NULL) {
        LOGE("SDMTracker_getGLESImageVertexJNI out_data is null.\n");
        return;
    }

    wrapper->sdm_tracker->getGlesImageVertex((float) centerX, (float) centerY, (float) angle,
                                             (float) canvasWidth, (float) canvasHeight, out_data,
                                             8);

    if (out_data) {
        (env)->ReleaseFloatArrayElements(jout, (jfloat *) out_data, 0);
    }

    return;
}

static int
SDMTracker_trackImageJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jbyteArray jdata,
                         jint size, jint width, jint height, jfloatArray jout) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    uint8_t *in_data = NULL;
    float *out_data = NULL;

    if (wrapper == NULL) {
        LOGE("SDMTracker_trackImageJNI addr is null.\n");
        return -100;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_trackImageJNI invalid wrapper !!!!");
        return -99;
    }

    in_data = (uint8_t * )(env->GetByteArrayElements(jdata, 0));
    if (in_data == NULL) {
        LOGE("SDMTracker_trackImageJNI in_data is null.\n");
        return -3;
    }

    out_data = (float *) (env->GetFloatArrayElements(jout, 0));
    if (jout == NULL) {
        LOGE("SDMTracker_trackImageJNI out_data is null.\n");
        return -4;
    }
//    LOGE("SDMTracker_trackImageJNI trackImage bef ... \n");
    wrapper->sdm_tracker->trackImage(in_data, (int) size, (int) width, (int) height, out_data);
//    LOGE("SDMTracker_trackImageJNI trackImage aft ... \n");
    if (in_data) {
        (env)->ReleaseByteArrayElements(jdata, (jbyte *) in_data, 0);
    }
    if (out_data) {
        (env)->ReleaseFloatArrayElements(jout, (jfloat *) out_data, 0);
    }

    return 0;
}

static void
SDMTracker_getRotationParamsJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jfloatArray jout) {
    // void getRotationParams(int *centerX, int *centerY, float *angle);
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    float *out_data = NULL;
    int center_x;
    int center_y;
    float angle;

    if (wrapper == NULL) {
        LOGE("SDMTracker_getRotationParamsJNI addr is null.\n");
        return;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_getRotationParamsJNI invalid wrapper !!!!");
        return;
    }

    out_data = (float *) (env->GetFloatArrayElements(jout, 0));
    if (jout == NULL) {
        LOGE("SDMTracker_getRotationParamsJNI out_data is null.\n");
        return;
    }

    wrapper->sdm_tracker->getRotationParams(&center_x, &center_y, &angle);

    out_data[0] = (float) (center_x * 1.0f);
    out_data[1] = (float) (center_y * 1.0f);
    out_data[2] = (float) (angle);

    if (out_data) {
        (env)->ReleaseFloatArrayElements(jout, (jfloat *) out_data, 0);
    }

    return;
}

static int
SDMTracker_getEnclosingBoxJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jfloatArray jout) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    float *out_data = NULL;
    float left;
    float top;
    float right;
    float bottom;

    if (wrapper == NULL) {
        LOGE("SDMTracker_getEnclosingBoxJNI addr is null.\n");
        return -1;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_getEnclosingBoxJNI invalid wrapper !!!!");
        return -1;
    }

    out_data = (float *) (env->GetFloatArrayElements(jout, 0));
    if (jout == NULL) {
        LOGE("SDMTracker_getEnclosingBoxJNI out_data is null.\n");
        return -1;
    }
    LOGE("SDMTracker_getEnclosingBoxJNI bef.\n");
    wrapper->sdm_tracker->getEnclosingBox(&left, &top, &right, &bottom);
    LOGE("SDMTracker_getEnclosingBoxJNI aft.\n");
    out_data[0] = (float) (left * 1.0f);
    out_data[1] = (float) (top * 1.0f);
    out_data[2] = (float) (right * 1.0f);
    out_data[3] = (float) (bottom * 1.0f);

    if (out_data) {
        (env)->ReleaseFloatArrayElements(jout, (jfloat *) out_data, 0);
    }

    return 0;
}

static int
SDMTracker_trackImageAJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jbyteArray jdata,
                          jint size, jint width, jint height, jint center_x, jint center_y,
                          jfloat angle, jfloatArray jout) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    uint8_t *in_data = NULL;
    float *out_data = NULL;

    if (wrapper == NULL) {
        LOGE("SDMTracker_trackImageAJNI addr is null.\n");
        return -100;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_trackImageAJNI invalid wrapper !!!!");
        return -99;
    }

    in_data = (uint8_t * )(env->GetByteArrayElements(jdata, 0));
    if (in_data == NULL) {
        LOGE("SDMTracker_trackImageAJNI in_data is null.\n");
        return -3;
    }

    out_data = (float *) (env->GetFloatArrayElements(jout, 0));
    if (jout == NULL) {
        LOGE("SDMTracker_trackImageAJNI out_data is null.\n");
        return -4;
    }
    //    LOGE("SDMTracker_trackImageJNI trackImage bef ... \n");
    wrapper->sdm_tracker->trackImage(in_data, (int) size, (int) width, (int) height, (int) center_x,
                                     (int) center_y, (double) angle, out_data);
    //    LOGE("SDMTracker_trackImageJNI trackImage aft ... \n");
    if (in_data) {
        (env)->ReleaseByteArrayElements(jdata, (jbyte *) in_data, 0);
    }
    if (out_data) {
        (env)->ReleaseFloatArrayElements(jout, (jfloat *) out_data, 0);
    }

    return 0;
}

static int
SDMTracker_getThinFacePositionJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jintArray jout) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    int left[4];
    int right[4];

    int *out_data = NULL;

    if (wrapper == NULL) {
        LOGE("SDMTracker_getThinFacePositionJNI addr is null.\n");
        return -100;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_getThinFacePositionJNI invalid wrapper !!!!");
        return -99;
    }

    out_data = (int *) (env->GetIntArrayElements(jout, 0));
    if (jout == NULL) {
        LOGE("SDMTracker_getThinFacePositionJNI out_data is null.\n");
        return -4;
    }

    wrapper->sdm_tracker->locateLeftThinFace(&left[0], &left[1], &left[2], &left[3]);
    wrapper->sdm_tracker->locateRightThinFace(&right[0], &right[1], &right[2], &right[3]);

    out_data[0] = left[0];
    out_data[1] = left[1];
    out_data[2] = left[2];
    out_data[3] = left[3];

    out_data[4] = right[0];
    out_data[5] = right[1];
    out_data[6] = right[2];
    out_data[7] = right[3];

    if (out_data) {
        (env)->ReleaseIntArrayElements(jout, (jint *) out_data, 0);
    }

    return 4;
}

static int
SDMTracker_getSmallFacePositionJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jintArray jout) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    int p[4];

    int *out_data = NULL;

    if (wrapper == NULL) {
        LOGE("SDMTracker_getSmallFacePositionJNI addr is null.\n");
        return -100;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_getSmallFacePositionJNI invalid wrapper !!!!");
        return -99;
    }

    out_data = (int *) (env->GetIntArrayElements(jout, 0));
    if (jout == NULL) {
        LOGE("SDMTracker_getSmallFacePositionJNI out_data is null.\n");
        return -4;
    }

    wrapper->sdm_tracker->locatePoint(29, &p[0], &p[1]);
    wrapper->sdm_tracker->locatePoint(62, &p[2], &p[3]);

    out_data[0] = p[0];
    out_data[1] = p[1];
    out_data[2] = p[2];
    out_data[3] = p[3];

    if (out_data) {
        (env)->ReleaseIntArrayElements(jout, (jint *) out_data, 0);
    }

    return 4;
}

static int
SDMTracker_getBigEyePositionJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jintArray jout) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    int left_x, left_y, right_x, right_y;
    int *out_data = NULL;

    if (wrapper == NULL) {
        LOGE("SDMTracker_getBigEyePositionJNI addr is null.\n");
        return -100;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_getBigEyePositionJNI invalid wrapper !!!!");
        return -99;
    }

    out_data = (int *) (env->GetIntArrayElements(jout, 0));
    if (jout == NULL) {
        LOGE("SDMTracker_getBigEyePositionJNI out_data is null.\n");
        return -4;
    }

    wrapper->sdm_tracker->locateLeftEye(&left_x, &left_y);
    wrapper->sdm_tracker->locateRightEye(&right_x, &right_y);

    out_data[0] = left_x;
    out_data[1] = left_y;
    out_data[2] = right_x;
    out_data[3] = right_y;

    if (out_data) {
        (env)->ReleaseIntArrayElements(jout, (jint *) out_data, 0);
    }

    return 4;
}

static int
SDMTracker_updateFaceRectJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jint x, jint y,
                             jint width, jint height) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    if (wrapper == NULL) {
        LOGE("SDMTracker_updateFaceRectJNI addr is null.\n");
        return -100;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_updateFaceRectJNI invalid wrapper !!!!");
        return -99;
    }

    wrapper->sdm_tracker->updateFaceRect(x, y, width, height);

    return 0;
}

static void
SDMTracker_closeTrackerJNI(JNIEnv *env, jobject thiz, jlong streamer_addr) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    LOGE("SDMTracker_closeTrackerJNI started ... \n");

    if (wrapper == NULL) {
        LOGE("SDMTracker_closeTrackerJNI addr is null.\n");
        return;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_closeTrackerJNI invalid wrapper !!!!");
        return;
    }

    if (wrapper->sdm_tracker != NULL) {
        delete wrapper->sdm_tracker;
        wrapper->sdm_tracker = NULL;
    }

    if (wrapper->frame_player != NULL) {
        delete wrapper->frame_player;
        wrapper->frame_player = NULL;
    }

    return;
}
/*
static int SDMTracker_testDrawTextureJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jint texid, jint filterInputTextureUniform, jint filterPositionAttribute, jint filterTextureCoordinateAttribute)
{
    static const GLfloat textureCoordinates[] = {
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f,
    };
    
    static const GLfloat imageVertices[] = {
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f,  1.0f,
        1.0f,  1.0f,
    };
    
    static const GLfloat imageVertices2[] = {
        -1.0f/2.0f, -1.0f/2.0f,
        1.0f/2.0f, -1.0f/2.0f,
        -1.0f/2.0f,  1.0f/2.0f,
        1.0f/2.0f,  1.0f/2.0f,
    };
    
    int textureId = texid;
    
    glActiveTexture(GL_TEXTURE2);
    glBindTexture(GL_TEXTURE_2D, textureId);
    
    glUniform1i(filterInputTextureUniform, 2);
    
    glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, 0, 0, imageVertices);
    glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, 0, 0, textureCoordinates);
    
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    
    std::string s = "a";
    textureId = gst::AndroidRenderAPI::loadPngToTexture(s);//(int)texid;
    
    glActiveTexture(GL_TEXTURE2);
    glBindTexture(GL_TEXTURE_2D, textureId);
    
    glUniform1i(filterInputTextureUniform, 2);
    
    glVertexAttribPointer(filterPositionAttribute, 2, GL_FLOAT, 0, 0, imageVertices2);
    glVertexAttribPointer(filterTextureCoordinateAttribute, 2, GL_FLOAT, 0, 0, textureCoordinates);
    
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    
    return textureId;
}
*/
// must be called on gles thread
static void
SDMTracker_startPlayStickerJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jstring foldername,
                               jint loop) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    if (wrapper == NULL) {
        LOGE("SDMTracker_startPlaySticker addr is null.\n");
        return;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_startPlaySticker invalid wrapper !!!!");
        return;
    }

    const char *c_name = NULL;

    c_name = env->GetStringUTFChars(foldername, NULL);

    if (c_name == NULL) {
        LOGE("SDMTracker_startPlaySticker params check ng (filename) ... \n");
        return;
    }

    LOGE("SDMTracker_startPlaySticker started %s ... \n", c_name);

    wrapper->frame_player->playWithLoopCount(c_name, loop);

    if (c_name) {
        env->ReleaseStringUTFChars(foldername, c_name);
    }

    return;
}

// must be called on gles thread
static void SDMTracker_stopPlayStickerJNI(JNIEnv *env, jobject thiz, jlong streamer_addr) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    if (wrapper == NULL) {
        LOGE("SDMTracker_stopPlaySticker addr is null.\n");
        return;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_stopPlaySticker invalid wrapper !!!!");
        return;
    }

    LOGE("SDMTracker_stopPlaySticker started ... \n");

    wrapper->frame_player->stop();

    return;
}

static void SDMTracker_prepareTextureJNI(JNIEnv *env, jobject thiz, jlong streamer_addr) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    if (wrapper == NULL) {
        LOGE("SDMTracker_prepareTexture addr is null.\n");
        return;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_prepareTexture invalid wrapper !!!!");
        return;
    }

    wrapper->frame_player->preload();

    return;
}

static int
SDMTracker_prepareTextureFramesJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jint count,
                                   jint ready) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    if (wrapper == NULL) {
        LOGE("SDMTracker_prepareTextureFramesJNI addr is null.\n");
        return -1;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_prepareTextureFramesJNI invalid wrapper !!!!");
        return -2;
    }

    return wrapper->frame_player->preloadFrames(count, ready);
}

static void SDMTracker_clearTextureJNI(JNIEnv *env, jobject thiz, jlong streamer_addr) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    if (wrapper == NULL) {
        LOGE("SDMTracker_clearTexture addr is null.\n");
        return;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_clearTexture invalid wrapper !!!!");
        return;
    }

    wrapper->frame_player->unload();

    return;
}

static void
SDMTracker_drawWithUniformJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jint unif, jint pos,
                              jint coor) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    if (wrapper == NULL) {
        LOGE("SDMTracker_drawWithUniform addr is null.\n");
        return;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_drawWithUniform invalid wrapper !!!!");
        return;
    }

    wrapper->frame_player->setUniformInfo(unif, pos, coor);

    // TODO:代码太难看，这里强制放大两倍
    int i = 0;
    float b[136];
    for (i = 0; i < 136; i++) {
        b[i] = wrapper->sdm_tracker->points[i] * 2.0f;
    }

    wrapper->frame_player->updateFacePoint(&(b[0]), 136);
    wrapper->frame_player->draw();

    return;
}

static void
SDMTracker_udpateCanvasSizeJNI(JNIEnv *env, jobject thiz, jlong streamer_addr, jint width,
                               jint height) {
    SDM_TRACKER_WRAPPER *wrapper = (SDM_TRACKER_WRAPPER *) streamer_addr;

    if (wrapper == NULL) {
        LOGE("SDMTracker_udpateCanvasWidth addr is null.\n");
        return;
    }

    if (wrapper_is_valid(wrapper) == 0) {
        LOGE("SDMTracker_udpateCanvasWidth invalid wrapper !!!!");
        return;
    }

    wrapper->frame_player->updateCanvasSize(width, height);

    return;
}

static void
SDMTracker_glReadPixelsJNI(JNIEnv *env, jobject thiz, jint x, jint y, jint width, jint height,
                           jint format, jint type) {
    glReadPixels(x, y, width, height, format, type, 0);
}


// ----------------------------------------------------------------------------

static JNINativeMethod g_methods[] = {

        {"_createTrackerJNI",        "()J",                     (long long *) SDMTracker_createTrackerJNI},
        {"_closeTrackerJNI",         "(J)V",                    (void *) SDMTracker_closeTrackerJNI},

        {"_loadModelJNI",            "(JLjava/lang/String;)I",  (int *) SDMTracker_loadModelJNI},
        {"_trackImageJNI",           "(J[BIII[F)I",             (int *) SDMTracker_trackImageJNI},
        {"_trackImageAJNI",          "(J[BIIIIIF[F)I",          (int *) SDMTracker_trackImageAJNI},
        {"_getRotationParamsJNI",    "(J[F)V",                  (void *) SDMTracker_getRotationParamsJNI},
        {"_getEnclosingBoxJNI",      "(J[F)I",                  (int *) SDMTracker_getEnclosingBoxJNI},

        {"_getBigEyePositionJNI",    "(J[I)I",                  (int *) SDMTracker_getBigEyePositionJNI},
        {"_getThinFacePositionJNI",  "(J[I)I",                  (int *) SDMTracker_getThinFacePositionJNI},
        {"_getSmallFacePositionJNI", "(J[I)I",                  (int *) SDMTracker_getSmallFacePositionJNI},

        {"_getGLESImageVertexJNI",   "(JFFFFF[F)V",             (void *) SDMTracker_getGLESImageVertexJNI},
        {"_updateFaceRectJNI",       "(JIIII)I",                (int *) SDMTracker_updateFaceRectJNI},

        {"_startPlayStickerJNI",     "(JLjava/lang/String;I)V", (void *) SDMTracker_startPlayStickerJNI},
        {"_stopPlayStickerJNI",      "(J)V",                    (void *) SDMTracker_stopPlayStickerJNI},
        {"_prepareTextureJNI",       "(J)V",                    (void *) SDMTracker_prepareTextureJNI},
        {"_clearTextureJNI",         "(J)V",                    (void *) SDMTracker_clearTextureJNI},
        {"_drawWithUniformJNI",      "(JIII)V",                 (void *) SDMTracker_drawWithUniformJNI},
        {"_udpateCanvasSizeJNI",     "(JII)V",                  (void *) SDMTracker_udpateCanvasSizeJNI},
        {"_prepareTextureFramesJNI", "(JII)I",                  (int *) SDMTracker_prepareTextureFramesJNI},

        {"_glReadPixelsJNI",         "(IIIIII)V",               (int *) SDMTracker_glReadPixelsJNI}

        //{"_testDrawTextureJNI", "(JIIII)I", (int*)SDMTracker_testDrawTextureJNI}
};

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    jclass clazz;

    g_jvm = vm;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);

    // 注册本地方法.Load 目标类
    clazz = (env)->FindClass(JNI_CLASS_MGSTREAMER);
    if (clazz == NULL) {
        LOGE("Native sdm registration unable to find class '%s'", JNI_CLASS_MGSTREAMER);
        return JNI_ERR;
    }
    g_clazz.clazz = (jclass) env->NewGlobalRef(clazz);

    // 注册本地native方法
    if ((env)->RegisterNatives(clazz, g_methods, NELEM(g_methods)) < 0) {
        LOGE("ERROR: sdm native registration failed\n");
        return JNI_ERR;
    }

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
}

void JNI_AttachThread() {
    if (g_jvm != NULL) {

        JNIEnv *env = NULL;

        // 防止多次attach
        if ((g_jvm)->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {

            int status = (g_jvm)->AttachCurrentThread(&env, 0);

            if (status < 0) {

            }
        }
    }

}

void JNI_detachThread() {
    if (g_jvm != NULL) {
        (g_jvm)->DetachCurrentThread();
    }
}

//#ifdef __cplusplus
//}
//#endif

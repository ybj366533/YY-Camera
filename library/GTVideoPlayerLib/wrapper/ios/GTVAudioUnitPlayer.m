#import "GTVAudioUnitPlayer.h"

#import <AVFoundation/AVFoundation.h>
#include <pthread.h>
#include <sys/time.h>

@implementation GTVAudioUnitPlayer {
    
    AudioUnit _auUnit;
    BOOL _isPaused;
}

- (id)init
{
//    NSError * error;
//    if (@available(iOS 9.0, *)) {
//        if (NO == [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayAndRecord withOptions:AVAudioSessionCategoryOptionDefaultToSpeaker error:&error]) {
//            //if (NO == [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayback error:&error]) {
//            NSLog(@"IJKAudioKit: AVAudioSession.setCategory() failed: %@\n", error ? [error localizedDescription] : @"nil");
//        }
//    } else {
//        // Fallback on earlier versions
//    }
    
    self = [super init];
    if (self) {
        
        AudioComponentDescription desc;
        desc.componentType = kAudioUnitType_Output; // we want to ouput
        desc.componentSubType = kAudioUnitSubType_RemoteIO; // we want in and ouput
        desc.componentFlags = 0; // must be zero
        desc.componentFlagsMask = 0; // must be zero
        desc.componentManufacturer = kAudioUnitManufacturer_Apple; // select provider

        AudioComponent auComponent = AudioComponentFindNext(NULL, &desc);
        if (auComponent == NULL) {
            NSLog(@"AudioUnit: AudioComponentFindNext failed");
            self = nil;
            return nil;
        }

        AudioUnit auUnit;
        OSStatus status = AudioComponentInstanceNew(auComponent, &auUnit);
        if (status != noErr) {
            NSLog(@"AudioUnit: AudioComponentInstanceNew failed");
            self = nil;
            return nil;
        }

        UInt32 flag = 1;
        status = AudioUnitSetProperty(auUnit,
                                      kAudioOutputUnitProperty_EnableIO,
                                      kAudioUnitScope_Output,
                                      0,
                                      &flag,
                                      sizeof(flag));
        if (status != noErr) {
            NSLog(@"AudioUnit: failed to set IO mode (%d)", (int)status);
        }

        /* Get the current format */
        AudioStreamBasicDescription audioFormat;
        
        audioFormat.mSampleRate			= 44100.00;
        audioFormat.mFormatID = kAudioFormatLinearPCM;
        audioFormat.mFormatFlags = (kAudioFormatFlagIsSignedInteger | kAudioFormatFlagsNativeEndian | kAudioFormatFlagIsPacked);
        audioFormat.mFramesPerPacket	= 1;
        audioFormat.mChannelsPerFrame	= 2;
        audioFormat.mBitsPerChannel		= 16;
        audioFormat.mBytesPerPacket		= 4;
        audioFormat.mBytesPerFrame		= 4;

        /* Set the desired format */
        UInt32 i_param_size = sizeof(audioFormat);
        status = AudioUnitSetProperty(auUnit,
                                      kAudioUnitProperty_StreamFormat,
                                      kAudioUnitScope_Input,
                                      0,
                                      &audioFormat,
                                      i_param_size);
        if (status != noErr) {
            NSLog(@"AudioUnit: failed to set stream format (%d)", (int)status);
            self = nil;
            return nil;
        }

        AURenderCallbackStruct callback;
        callback.inputProc = (AURenderCallback) RenderCallback;
        callback.inputProcRefCon = (__bridge void*) self;
        status = AudioUnitSetProperty(auUnit,
                                      kAudioUnitProperty_SetRenderCallback,
                                      kAudioUnitScope_Input,
                                      0, &callback, sizeof(callback));
        if (status != noErr) {
            NSLog(@"AudioUnit: render callback setup failed (%d)\n", (int)status);
            self = nil;
            return nil;
        }

        /* AU initiliaze */
        status = AudioUnitInitialize(auUnit);
        if (status != noErr) {
            NSLog(@"AudioUnit: AudioUnitInitialize failed (%d)\n", (int)status);
            self = nil;
            return nil;
        }

        _auUnit = auUnit;
    }
    
    return self;
}

- (void)dealloc
{
    [self close];
}

- (void)play
{
    if (!_auUnit)
        return;

    _isPaused = NO;
    NSError *error = nil;
    if (NO == [[AVAudioSession sharedInstance] setActive:YES error:&error]) {
        NSLog(@"AudioUnit: AVAudioSession.setActive(YES) failed: %@\n", error ? [error localizedDescription] : @"nil");
    }

    OSStatus status = AudioOutputUnitStart(_auUnit);
    if (status != noErr)
        NSLog(@"AudioUnit: AudioOutputUnitStart failed (%d)\n", (int)status);
}

- (void)pause
{
    if (!_auUnit)
        return;

    _isPaused = YES;
    OSStatus status = AudioOutputUnitStop(_auUnit);
    if (status != noErr)
        NSLog(@"AudioUnit: failed to stop AudioUnit (%d)\n", (int)status);
}

- (void)flush
{
    if (!_auUnit)
        return;

    AudioUnitReset(_auUnit, kAudioUnitScope_Global, 0);
}

- (void)stop
{
    if (!_auUnit)
        return;

    OSStatus status = AudioOutputUnitStop(_auUnit);
    if (status != noErr)
        NSLog(@"AudioUnit: failed to stop AudioUnit (%d)", (int)status);
}

- (void)close
{
    [self stop];

    if (!_auUnit)
        return;

    AURenderCallbackStruct callback;
    memset(&callback, 0, sizeof(AURenderCallbackStruct));
    AudioUnitSetProperty(_auUnit,
                         kAudioUnitProperty_SetRenderCallback,
                         kAudioUnitScope_Input, 0, &callback,
                         sizeof(callback));

    AudioComponentInstanceDispose(_auUnit);
    _auUnit = NULL;
}

static OSStatus RenderCallback(void                        *inRefCon,
                               AudioUnitRenderActionFlags  *ioActionFlags,
                               const AudioTimeStamp        *inTimeStamp,
                               UInt32                      inBusNumber,
                               UInt32                      inNumberFrames,
                               AudioBufferList             *ioData)
{
    @autoreleasepool {
        
        GTVAudioUnitPlayer* auController = (__bridge GTVAudioUnitPlayer *) inRefCon;

        if (!auController || auController->_isPaused) {
            for (UInt32 i = 0; i < ioData->mNumberBuffers; i++) {
                AudioBuffer *ioBuffer = &ioData->mBuffers[i];
                memset(ioBuffer->mData, 0x00, ioBuffer->mDataByteSize);
            }
            return noErr;
        }

        for (int i = 0; i < (int)ioData->mNumberBuffers; i++) {
            AudioBuffer *ioBuffer = &ioData->mBuffers[i];
//            (*auController.spec.callback)(auController.spec.userdata, ioBuffer->mData, ioBuffer->mDataByteSize);
            memset(ioBuffer->mData, 0x00, ioBuffer->mDataByteSize);
            [auController consumeAudioData:ioBuffer->mData withLength:ioBuffer->mDataByteSize];
        }

        return noErr;
    }
}

- (void)consumeAudioData:(uint8_t*)pData withLength:(int)len
{
    if( self.delegate != nil && [self.delegate respondsToSelector:@selector(onQueryAudioData:withBuffer:andSize:)] ) {
        [self.delegate onQueryAudioData:self withBuffer:pData andSize:len];
    }
    
    return;
}

@end

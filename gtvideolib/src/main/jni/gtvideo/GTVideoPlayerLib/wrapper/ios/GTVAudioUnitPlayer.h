#import <Foundation/Foundation.h>

@protocol GTVAudioUnitPlayerDelegate <NSObject>

- (int) onQueryAudioData:(NSObject*)player withBuffer:(uint8_t*)buf andSize:(int)len;

@end

@interface GTVAudioUnitPlayer : NSObject

@property (nonatomic, weak) id<GTVAudioUnitPlayerDelegate> delegate;

- (void)play;
- (void)pause;
- (void)flush;
- (void)stop;
- (void)close;

@end

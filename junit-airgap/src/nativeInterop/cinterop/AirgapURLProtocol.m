//
//  AirgapURLProtocol.m
//  Airgap iOS Native
//
//  Implementation of network blocking via NSURLProtocol interception
//

#import "AirgapURLProtocol.h"

// Private property key for marking requests we've already handled
static NSString * const AirgapURLProtocolHandledKey = @"AirgapURLProtocolHandled";

@implementation AirgapURLProtocol

// Store configuration and callback in static variables
static NSDictionary *_configuration = nil;
static dispatch_queue_t _configQueue = nil;
static HostBlockingCallback _blockingCallback = NULL;

+ (void)initialize {
    if (self == [AirgapURLProtocol class]) {
        _configQueue = dispatch_queue_create("com.airgap.config", DISPATCH_QUEUE_CONCURRENT);
    }
}

#pragma mark - Public API

+ (void)registerAirgapProtocol {
    [NSURLProtocol registerClass:[AirgapURLProtocol class]];
}

+ (void)unregisterAirgapProtocol {
    [NSURLProtocol unregisterClass:[AirgapURLProtocol class]];
}

+ (void)setConfigurationWithBlockByDefault:(BOOL)blockByDefault
                              allowedHosts:(nullable NSArray<NSString *> *)allowedHosts
                              blockedHosts:(nullable NSArray<NSString *> *)blockedHosts
                                  callback:(nullable HostBlockingCallback)callback {
    // Convert primitive parameters to NSDictionary for internal storage
    NSMutableDictionary *config = [NSMutableDictionary dictionary];
    config[@"blockByDefault"] = @(blockByDefault);
    config[@"allowedHosts"] = allowedHosts ?: @[];
    config[@"blockedHosts"] = blockedHosts ?: @[];

    dispatch_barrier_async(_configQueue, ^{
        _configuration = [config copy];
        _blockingCallback = callback;
    });
}

+ (nullable NSDictionary *)getConfiguration {
    __block NSDictionary *config = nil;
    dispatch_sync(_configQueue, ^{
        config = _configuration;
    });
    return config;
}

#pragma mark - NSURLProtocol Overrides

+ (BOOL)canInitWithRequest:(NSURLRequest *)request {
    // Avoid infinite loops - don't handle requests we've already processed
    if ([NSURLProtocol propertyForKey:AirgapURLProtocolHandledKey inRequest:request]) {
        return NO;
    }

    // Only intercept HTTP and HTTPS requests
    NSString *scheme = request.URL.scheme.lowercaseString;
    if (![scheme isEqualToString:@"http"] && ![scheme isEqualToString:@"https"]) {
        return NO;
    }

    // Get current configuration
    NSDictionary *config = [[self class] getConfiguration];

    // Check if this request should be blocked
    NSString *host = request.URL.host;
    if ([[self class] shouldBlockHost:host withConfiguration:config]) {
        // YES - we will handle (block) this request
        return YES;
    }

    // NO - let the system handle this request normally
    return NO;
}

+ (NSURLRequest *)canonicalRequestForRequest:(NSURLRequest *)request {
    return request;
}

- (void)startLoading {
    // If we get here, canInitWithRequest returned YES, which means this request should be blocked
    NSString *host = self.request.URL.host ?: @"<unknown>";

    // Block the request with an error
    NSError *error = [NSError errorWithDomain:@"AirgapError"
                                         code:1001
                                     userInfo:@{
        NSLocalizedDescriptionKey: [NSString stringWithFormat:@"Network request to %@ blocked by Airgap extension", host]
    }];

    [self.client URLProtocol:self didFailWithError:error];
    [self.client URLProtocolDidFinishLoading:self];
}

- (void)stopLoading {
    // Nothing to stop since we immediately fail blocked requests in startLoading
}

#pragma mark - Private Helpers

+ (BOOL)shouldBlockHost:(NSString *)host withConfiguration:(NSDictionary *)config {
    if (!config) {
        // No configuration - don't block
        return NO;
    }

    // Use the callback if available
    __block HostBlockingCallback callback = NULL;
    dispatch_sync(_configQueue, ^{
        callback = _blockingCallback;
    });

    if (callback == NULL) {
        // No callback set - don't block
        return NO;
    }

    // Call the Kotlin callback to determine if host should be blocked
    const char *hostCString = [host UTF8String];
    if (hostCString == NULL) {
        return NO;  // Can't check null host
    }

    bool shouldBlock = callback(hostCString);
    return shouldBlock ? YES : NO;
}

@end

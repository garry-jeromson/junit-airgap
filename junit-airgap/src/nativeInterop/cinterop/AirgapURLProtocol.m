//
//  AirgapURLProtocol.m
//  Airgap iOS Native
//
//  Implementation of network blocking via NSURLProtocol interception
//

#import "AirgapURLProtocol.h"

// Private property key for marking requests we've already handled
static NSString * const AirgapURLProtocolHandledKey = @"AirgapURLProtocolHandled";

@implementation AirgapURLProtocol {
    NSURLConnection *_connection;
}

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
    return [scheme isEqualToString:@"http"] || [scheme isEqualToString:@"https"];
}

+ (NSURLRequest *)canonicalRequestForRequest:(NSURLRequest *)request {
    return request;
}

- (void)startLoading {
    // Get current configuration
    NSDictionary *config = [[self class] getConfiguration];

    // Check if this request should be blocked
    NSString *host = self.request.URL.host;
    if ([self shouldBlockHost:host withConfiguration:config]) {
        // Block the request
        NSError *error = [NSError errorWithDomain:@"AirgapError"
                                             code:1001
                                         userInfo:@{
            NSLocalizedDescriptionKey: [NSString stringWithFormat:@"Network request to %@ blocked by Airgap extension", host]
        }];

        [self.client URLProtocol:self didFailWithError:error];
        [self.client URLProtocolDidFinishLoading:self];
        return;
    }

    // Allow the request - mark it and pass through
    NSMutableURLRequest *newRequest = [self.request mutableCopy];
    [NSURLProtocol setProperty:@YES forKey:AirgapURLProtocolHandledKey inRequest:newRequest];

    _connection = [NSURLConnection connectionWithRequest:newRequest delegate:self];
}

- (void)stopLoading {
    [_connection cancel];
    _connection = nil;
}

#pragma mark - NSURLConnectionDelegate (for allowed requests)

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSURLResponse *)response {
    [self.client URLProtocol:self didReceiveResponse:response cacheStoragePolicy:NSURLCacheStorageNotAllowed];
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
    [self.client URLProtocol:self didLoadData:data];
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
    [self.client URLProtocolDidFinishLoading:self];
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error {
    [self.client URLProtocol:self didFailWithError:error];
}

#pragma mark - Private Helpers

- (BOOL)shouldBlockHost:(NSString *)host withConfiguration:(NSDictionary *)config {
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

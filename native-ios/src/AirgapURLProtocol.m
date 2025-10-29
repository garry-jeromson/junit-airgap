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

// Store configuration in static variable
static NSDictionary *_configuration = nil;
static dispatch_queue_t _configQueue = nil;

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

+ (void)setConfiguration:(NSDictionary *)config {
    dispatch_barrier_async(_configQueue, ^{
        _configuration = [config copy];
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

    BOOL blockByDefault = [config[@"blockByDefault"] boolValue];
    NSArray *allowedHosts = config[@"allowedHosts"];
    NSArray *blockedHosts = config[@"blockedHosts"];

    // Check blocked hosts first (they take precedence)
    if ([self host:host matchesAnyPattern:blockedHosts]) {
        return YES;
    }

    // If blocking by default, check if host is in allowed list
    if (blockByDefault) {
        if ([self host:host matchesAnyPattern:allowedHosts]) {
            return NO;  // Explicitly allowed
        }
        return YES;  // Block by default
    }

    // If not blocking by default, allow everything not in blocked list
    return NO;
}

- (BOOL)host:(NSString *)host matchesAnyPattern:(NSArray *)patterns {
    if (!patterns || patterns.count == 0) {
        return NO;
    }

    for (NSString *pattern in patterns) {
        if ([self host:host matchesPattern:pattern]) {
            return YES;
        }
    }

    return NO;
}

- (BOOL)host:(NSString *)host matchesPattern:(NSString *)pattern {
    if (!host || !pattern) {
        return NO;
    }

    // Exact match
    if ([host isEqualToString:pattern]) {
        return YES;
    }

    // Wildcard match: "*.example.com" matches "api.example.com"
    if ([pattern hasPrefix:@"*."]) {
        NSString *suffix = [pattern substringFromIndex:1];  // Remove '*'
        if ([host hasSuffix:suffix]) {
            return YES;
        }
    }

    // Match all: "*"
    if ([pattern isEqualToString:@"*"]) {
        return YES;
    }

    return NO;
}

@end

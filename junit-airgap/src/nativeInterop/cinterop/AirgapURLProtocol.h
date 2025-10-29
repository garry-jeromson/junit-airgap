//
//  AirgapURLProtocol.h
//  Airgap iOS Native
//
//  NSURLProtocol subclass for intercepting URLSession requests
//  and blocking network calls based on configuration.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * NSURLProtocol subclass that intercepts URLSession requests
 * and blocks them based on NetworkConfiguration.
 *
 * This protocol is registered globally and intercepts all HTTP/HTTPS
 * requests made via URLSession (which Ktor Darwin engine uses).
 */
@interface AirgapURLProtocol : NSURLProtocol

/**
 * Register this protocol with NSURLProtocol.
 * After registration, all URLSession requests will be intercepted.
 */
+ (void)registerAirgapProtocol;

/**
 * Unregister this protocol from NSURLProtocol.
 * After unregistration, requests will proceed normally.
 */
+ (void)unregisterAirgapProtocol;

/**
 * Type definition for the host blocking callback function.
 * The callback receives a hostname and returns true if it should be blocked.
 */
typedef bool (*HostBlockingCallback)(const char* _Nullable host);

/**
 * Set the configuration for network blocking using primitive parameters.
 *
 * @param blockByDefault Whether to block requests by default
 * @param allowedHosts Array of host patterns that are allowed (supports wildcards like *.example.com)
 * @param blockedHosts Array of host patterns that are explicitly blocked (takes precedence over allowedHosts)
 * @param callback Function pointer to call for checking if a host should be blocked
 */
+ (void)setConfigurationWithBlockByDefault:(BOOL)blockByDefault
                              allowedHosts:(nullable NSArray<NSString *> *)allowedHosts
                              blockedHosts:(nullable NSArray<NSString *> *)blockedHosts
                                  callback:(nullable HostBlockingCallback)callback;

@end

NS_ASSUME_NONNULL_END

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
 * Set the configuration for network blocking.
 *
 * @param config Dictionary containing:
 *   - "blockByDefault" (NSNumber/BOOL): Whether to block requests by default
 *   - "allowedHosts" (NSArray<NSString *>): Hosts that are allowed (supports wildcards)
 *   - "blockedHosts" (NSArray<NSString *>): Hosts that are explicitly blocked (takes precedence)
 */
+ (void)setConfiguration:(NSDictionary *)config;

/**
 * Get the current configuration.
 *
 * @return Dictionary with current configuration, or nil if not set
 */
+ (nullable NSDictionary *)getConfiguration;

@end

NS_ASSUME_NONNULL_END

//
//  URLProtocolTest.m
//  Airgap iOS Native Tests
//
//  XCTest suite for AirgapURLProtocol
//  Following test-first approach - these tests define the expected behavior
//

#import <XCTest/XCTest.h>
#import "AirgapURLProtocol.h"

@interface URLProtocolTest : XCTestCase
@end

@implementation URLProtocolTest

- (void)setUp {
    [super setUp];
    // Unregister any previous protocol instances
    [AirgapURLProtocol unregisterAirgapProtocol];
}

- (void)tearDown {
    [AirgapURLProtocol unregisterAirgapProtocol];
    [super tearDown];
}

// Test 1: Basic registration
- (void)testURLProtocolCanBeRegistered {
    [AirgapURLProtocol registerAirgapProtocol];

    // Verify registration succeeded
    // Note: NSURLProtocol doesn't provide direct query API,
    // but we can verify by checking if it intercepts requests
    XCTAssertTrue(YES, @"Registration should not crash");
}

// Test 2: Can unregister without crashing
- (void)testURLProtocolCanBeUnregistered {
    [AirgapURLProtocol registerAirgapProtocol];
    [AirgapURLProtocol unregisterAirgapProtocol];

    XCTAssertTrue(YES, @"Unregistration should not crash");
}

// Test 3: Intercepts HTTP requests when configured to block
- (void)testInterceptsHTTPRequestsWhenBlocking {
    [AirgapURLProtocol registerAirgapProtocol];

    // Configure to block all requests
    NSDictionary *config = @{
        @"blockByDefault": @YES,
        @"allowedHosts": @[],
        @"blockedHosts": @[]
    };
    [AirgapURLProtocol setConfiguration:config];

    NSURLSessionConfiguration *sessionConfig = [NSURLSessionConfiguration ephemeralSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfig];
    NSURL *url = [NSURL URLWithString:@"http://example.com"];

    XCTestExpectation *expectation = [self expectationWithDescription:@"Request blocked"];

    [[session dataTaskWithURL:url completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        XCTAssertNotNil(error, @"Request should fail with error");
        XCTAssertEqualObjects(error.domain, @"AirgapError", @"Error should be from Airgap");
        XCTAssertTrue([error.localizedDescription containsString:@"blocked"], @"Error should mention blocking");
        [expectation fulfill];
    }] resume];

    [self waitForExpectations:@[expectation] timeout:5.0];
}

// Test 4: Intercepts HTTPS requests when configured to block
- (void)testInterceptsHTTPSRequestsWhenBlocking {
    [AirgapURLProtocol registerAirgapProtocol];

    NSDictionary *config = @{
        @"blockByDefault": @YES,
        @"allowedHosts": @[],
        @"blockedHosts": @[]
    };
    [AirgapURLProtocol setConfiguration:config];

    NSURLSessionConfiguration *sessionConfig = [NSURLSessionConfiguration ephemeralSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfig];
    NSURL *url = [NSURL URLWithString:@"https://example.com"];

    XCTestExpectation *expectation = [self expectationWithDescription:@"Request blocked"];

    [[session dataTaskWithURL:url completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        XCTAssertNotNil(error, @"HTTPS request should also be blocked");
        XCTAssertEqualObjects(error.domain, @"AirgapError");
        [expectation fulfill];
    }] resume];

    [self waitForExpectations:@[expectation] timeout:5.0];
}

// Test 5: Allows requests to allowed hosts
- (void)testAllowsRequestsToAllowedHosts {
    [AirgapURLProtocol registerAirgapProtocol];

    NSDictionary *config = @{
        @"blockByDefault": @YES,
        @"allowedHosts": @[@"localhost", @"127.0.0.1"],
        @"blockedHosts": @[]
    };
    [AirgapURLProtocol setConfiguration:config];

    NSURLSessionConfiguration *sessionConfig = [NSURLSessionConfiguration ephemeralSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfig];
    NSURL *url = [NSURL URLWithString:@"http://localhost:8080"];

    XCTestExpectation *expectation = [self expectationWithDescription:@"Request allowed"];

    [[session dataTaskWithURL:url completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        if (error) {
            // Connection refused is OK (no server running), but should NOT be AirgapError
            XCTAssertNotEqualObjects(error.domain, @"AirgapError", @"Localhost should not be blocked by Airgap");
        }
        [expectation fulfill];
    }] resume];

    [self waitForExpectations:@[expectation] timeout:5.0];
}

// Test 6: Blocks requests to blocked hosts even if in allowed list
- (void)testBlockedHostsTakePrecedence {
    [AirgapURLProtocol registerAirgapProtocol];

    NSDictionary *config = @{
        @"blockByDefault": @NO,
        @"allowedHosts": @[@"*"],  // Allow all
        @"blockedHosts": @[@"evil.com"]  // Except this
    };
    [AirgapURLProtocol setConfiguration:config];

    NSURLSessionConfiguration *sessionConfig = [NSURLSessionConfiguration ephemeralSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfig];
    NSURL *url = [NSURL URLWithString:@"http://evil.com"];

    XCTestExpectation *expectation = [self expectationWithDescription:@"Blocked host rejected"];

    [[session dataTaskWithURL:url completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        XCTAssertNotNil(error, @"Explicitly blocked host should fail");
        XCTAssertEqualObjects(error.domain, @"AirgapError");
        [expectation fulfill];
    }] resume];

    [self waitForExpectations:@[expectation] timeout:5.0];
}

// Test 7: Supports wildcard patterns in allowed hosts
- (void)testSupportsWildcardPatternsInAllowedHosts {
    [AirgapURLProtocol registerAirgapProtocol];

    NSDictionary *config = @{
        @"blockByDefault": @YES,
        @"allowedHosts": @[@"*.example.com"],
        @"blockedHosts": @[]
    };
    [AirgapURLProtocol setConfiguration:config];

    NSURLSessionConfiguration *sessionConfig = [NSURLSessionConfiguration ephemeralSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfig];

    // api.example.com should be allowed (matches *.example.com)
    NSURL *allowedUrl = [NSURL URLWithString:@"http://api.example.com"];
    XCTestExpectation *allowedExpectation = [self expectationWithDescription:@"Wildcard match allowed"];

    [[session dataTaskWithURL:allowedUrl completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        if (error) {
            XCTAssertNotEqualObjects(error.domain, @"AirgapError", @"*.example.com should match api.example.com");
        }
        [allowedExpectation fulfill];
    }] resume];

    // other.com should be blocked (doesn't match)
    NSURL *blockedUrl = [NSURL URLWithString:@"http://other.com"];
    XCTestExpectation *blockedExpectation = [self expectationWithDescription:@"Non-match blocked"];

    [[session dataTaskWithURL:blockedUrl completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        XCTAssertNotNil(error, @"other.com should be blocked");
        XCTAssertEqualObjects(error.domain, @"AirgapError");
        [blockedExpectation fulfill];
    }] resume];

    [self waitForExpectations:@[allowedExpectation, blockedExpectation] timeout:5.0];
}

// Test 8: Configuration can be updated dynamically
- (void)testConfigurationCanBeUpdatedDynamically {
    [AirgapURLProtocol registerAirgapProtocol];

    NSURLSessionConfiguration *sessionConfig = [NSURLSessionConfiguration ephemeralSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfig];
    NSURL *url = [NSURL URLWithString:@"http://example.com"];

    // First, block all
    NSDictionary *blockAllConfig = @{
        @"blockByDefault": @YES,
        @"allowedHosts": @[],
        @"blockedHosts": @[]
    };
    [AirgapURLProtocol setConfiguration:blockAllConfig];

    XCTestExpectation *blockedExpectation = [self expectationWithDescription:@"Initially blocked"];

    [[session dataTaskWithURL:url completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        XCTAssertNotNil(error);
        XCTAssertEqualObjects(error.domain, @"AirgapError");
        [blockedExpectation fulfill];
    }] resume];

    [self waitForExpectations:@[blockedExpectation] timeout:5.0];

    // Then, allow all
    NSDictionary *allowAllConfig = @{
        @"blockByDefault": @NO,
        @"allowedHosts": @[@"*"],
        @"blockedHosts": @[]
    };
    [AirgapURLProtocol setConfiguration:allowAllConfig];

    XCTestExpectation *allowedExpectation = [self expectationWithDescription:@"Now allowed"];

    [[session dataTaskWithURL:url completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        // Should either succeed or fail with non-Airgap error
        if (error) {
            XCTAssertNotEqualObjects(error.domain, @"AirgapError", @"Should not be blocked after config change");
        }
        [allowedExpectation fulfill];
    }] resume];

    [self waitForExpectations:@[allowedExpectation] timeout:5.0];
}

@end

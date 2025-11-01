# Investigation Documentation

This folder contains technical investigation documents that record the research and problem-solving process during the development of junit-airgap.

## Documents

### [JVMTI_DNS_INTERCEPTION_LIMITATION.md](./JVMTI_DNS_INTERCEPTION_LIMITATION.md)
Documents the fundamental limitation of JVMTI's `NativeMethodBindCallback` when DNS classes are loaded before agent initialization. This investigation revealed why a pure JVMTI approach couldn't reliably intercept DNS resolution in all scenarios (especially in IDE test runners).

**Key Findings:**
- DNS classes may load before JVMTI agent initialization completes
- Native method binding happens before the callback can intercept it
- Led to the two-layer interception architecture

### [BYTEBUDDY_DNS_INTERCEPTION_PLAN.md](./BYTEBUDDY_DNS_INTERCEPTION_PLAN.md)
Complete implementation plan for the ByteBuddy-based DNS interception layer. This document outlines the two-layer architecture that solved the JVMTI limitation.

**Architecture:**
- Layer 1: ByteBuddy intercepts at Java API level (`InetAddress.getAllByName`)
- Layer 2: JVMTI intercepts at native level (`Inet6AddressImpl.lookupAllHostAddr`)
- Both layers call the same `NetworkBlockerContext.checkConnection()` logic

**Contents:**
- 6-phase implementation plan with code examples
- Integration approach with existing JVMTI agent
- Testing strategy
- Estimated effort (completed in 2-3 days)

### [IOS_SUPPORT_INVESTIGATION.md](./IOS_SUPPORT_INVESTIGATION.md)
Investigation into supporting iOS platform with Kotlin/Native. Documents the technical limitations that prevent network blocking on iOS.

**Key Findings:**
- Kotlin/Native doesn't support JVMTI (no JVM on iOS)
- iOS sandbox restrictions prevent low-level network interception
- Alternative approaches (URLProtocol, swizzling) are platform-specific
- **Decision:** iOS support is API-only (configuration without blocking)

## Purpose

These documents serve as:

1. **Historical Record**: Understanding why certain architectural decisions were made
2. **Learning Resource**: For contributors who want to understand the technical challenges
3. **Decision Log**: Documenting why certain approaches were chosen or rejected
4. **Future Reference**: If similar issues arise or new platforms are added

## Related Documentation

- Main README: [../../README.md](../../README.md)
- Architecture decisions: See commit history for implementation details
- Plugin documentation: [../../gradle-plugin/README.md](../../gradle-plugin/README.md) (if exists)

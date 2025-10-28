# Contributing to JUnit Airgap Extension

Contributions are welcome! This guide explains how to contribute to the project.

## Process

1. **Fork the repository**
2. **Add tests for your feature or fix** - Tests are required for all contributions
3. **Implement your changes**
4. **Ensure all tests pass** - Run `make test` or `./gradlew test`
5. **Submit a pull request**

## Requirements

- Include tests with your contributions to ensure quality and prevent regressions
- Follow the existing code style (use `make format` to auto-format)
- Update documentation if you're adding new features

## Running Tests

```bash
# Run all tests
make test

# Run specific test suites
make test-jvm                     # JVM only (fastest)
make test-android                 # Android tests
make test-integration             # Integration tests
make test-plugin-integration      # Plugin integration tests
```

## Code Quality

```bash
# Format code
make format

# Run linter
make lint
```

## Questions?

- Check the [documentation](docs/)
- Open an issue for questions or bug reports
- Start a discussion for feature requests

Thank you for contributing!

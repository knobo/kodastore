# Contributing to KodaStore

Thank you for your interest in contributing to KodaStore! This guide will help you get started.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone git@github.com:YOUR_USERNAME/kodastore.git`
3. Create a branch: `git checkout -b my-feature`
4. Make your changes
5. Run the tests: `./gradlew test`
6. Push and open a pull request

## Development Setup

**Prerequisites:**
- JDK 21+
- Docker (for PostgreSQL)
- Gradle 9+ (or use the included wrapper)

**Start the database:**
```bash
docker compose up -d
```

**Build and run:**
```bash
./gradlew :infrastructure:bootRun
```

**Run tests:**
```bash
./gradlew test
```

## Project Structure

```
domain/           Pure Kotlin, no framework dependencies
application/      Use cases, depends on domain
infrastructure/   Spring Boot, PostgreSQL, REST API
examples/         Example applications using KodaStore
```

The hexagonal architecture means:
- **domain/** should never depend on Spring or any framework
- **application/** depends only on domain
- **infrastructure/** is where all framework code lives

## What to Work On

Check the [issue tracker](https://github.com/knobo/kodastore/issues) for open issues. Some areas where help is especially welcome:

- gRPC API alongside REST
- Subscription engine using PostgreSQL logical replication
- Snapshot support for long-lived streams
- Client libraries (Kotlin, Java, TypeScript)
- Projection support with transactional guarantees
- Performance benchmarks and optimization
- Documentation and tutorials

**Please open an issue before starting large changes** so we can discuss the approach.

## Pull Request Guidelines

- Keep PRs focused on a single change
- Include tests for new functionality
- Update documentation if needed
- Follow the existing code style
- Write a clear PR description explaining what and why

## Code Style

- Kotlin with standard conventions
- Prefer immutable data (`val`, `data class`)
- Domain module: no annotations, no framework types
- Infrastructure: Spring conventions

## Running the Example

```bash
# Terminal 1: Start KodaStore
./gradlew :infrastructure:bootRun

# Terminal 2: Start example order service
./gradlew :examples:order-service:bootRun
```

## Community

- Join our [Discord](https://discord.gg/CVX5e8cZKN) for discussion
- Be respectful and constructive
- Ask questions — there are no bad ones

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

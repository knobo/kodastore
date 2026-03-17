# Contributing to KodaStore

Thank you for your interest in contributing to KodaStore! Every contribution matters — whether it's code, a bug report, or a question that helps us improve the documentation.

## The Best Way to Start

You don't need to write code to make a valuable contribution. Here's how to get involved:

1. **Install and try it.** Run KodaStore locally, build something with it, and see how it feels. First-hand experience is the foundation of every good contribution.

2. **Open issues.** Found something confusing? Hit a rough edge? Have an idea for how something could work better? Open an issue. The best features and fixes come from people actually using the software. A well-written bug report or feature request is just as valuable as a pull request.

3. **Join the conversation.** Drop by [Discord](https://discord.gg/CVX5e8cZKN) and tell us what you're building, ask questions, or share feedback. Every perspective helps shape the project.

This is an early-stage project — your input right now has an outsized impact on where it goes.

## Submitting Code

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

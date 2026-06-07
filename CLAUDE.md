# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a learning repository named "notebook" containing a Spring Boot Java application (`examples/`) used for algorithm experimentation and study. The test classes (`src/test/java/io/github/dekkerding/examples/`) contain various algorithm implementations like Count, Bubbling, Bucket, Hill, Insert, Merge, etc.

## Build and Development

The project uses Gradle with the wrapper included. Work from the `examples/` directory for all Gradle commands.

```bash
cd examples
./gradlew build           # Build the project
./gradlew test            # Run all tests
./gradlew bootRun         # Run the Spring Boot application
./gradlew clean           # Clean build artifacts
```

## Architecture

- **Language**: Java 8 (target compatibility)
- **Framework**: Spring Boot 2.6.14
- **Build**: Gradle with wrapper
- **Package**: `io.github.dekkerding.examples`
- **Entry Point**: `ExamplesApplication.java`

### Key Dependencies
- Spring Boot Starter (AOP, Test)
- Spring Data Redis
- Jackson Databind (with JSR310 for Java 8 date/time)
- Lombok (compileOnly/annotationProcessor)

### Monitoring

The project uses a custom `@Monitoring` annotation for test execution monitoring. See `MonitoringAspect.java` and `Monitoring.java` for the AOP implementation.

### Test Structure

Algorithm implementations are written as test methods in classes under `src/test/java/io/github/dekkerding/examples/`. Many algorithms use a `Random.randomList()` helper for test data generation.

## Repository Structure

```
notebook/
├── examples/          # Spring Boot Java application (main module)
├── interface/         # Empty (reserved for future use)
├── notes/             # Learning notes and reference materials
└── openspec/          # OpenSpec configuration (currently unused)
```

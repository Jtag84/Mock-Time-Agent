
![](logo-banner.png)

# Mock-Time-Agent

<!-- TOC -->
* [Mock-Time-Agent](#mock-time-agent)
  * [Introduction](#introduction)
  * [Why Mock-Time-Agent?](#why-mock-time-agent)
  * [Features](#features)
  * [Use Cases](#use-cases)
  * [How It Works](#how-it-works)
  * [Building the Agent](#building-the-agent)
  * [Usage Instructions](#usage-instructions)
  * [Testing](#testing)
    * [Quick Test with Agent's Own Main Class](#quick-test-with-agents-own-main-class)
    * [Testing with a Spring Boot Application](#testing-with-a-spring-boot-application)
<!-- TOC -->

## Introduction

`Mock-Time-Agent` is a Java agent that allows for the simulation of different system times within a JVM based
application.
It offers precise control, enabling specific packages to operate under altered time,
while essential third-party libraries, like AWS, remain synced with the actual system time.
This approach offers advantages over tools like [libfaketime](https://github.com/wolfcw/libfaketime),
which globally affect the system time and may not work consistently across different operating systems, such as macOS.

## Why Mock-Time-Agent?

While alternatives like system clock changes or tools like libfaketime can be impractical and globally invasive,
Mock-Time-Agent provides a selective and reliable solution. It allows you to specify time alterations at the package
or class level, ensuring unaffected operation of services like AWS that require the current system time
for proper functionality.

## Features

* Time simulation for JVM based applications.
* Fine-grained control over time alteration with package-level precision.
* No impact on third-party libraries requiring the actual system time, such as AWS services.
* Platform-independent solution that works reliably, including on macOS.

## Use Cases

`Mock-Time-Agent` is useful for:

* Testing time-sensitive features without altering the system clock.
* Running scheduled tasks in a controlled time environment.
* Development and testing of time-based behavior without affecting external services.

## How It Works

`Mock-Time-Agent` works by modifying the bytecode at runtime, adding a specified time offset to calls
to `LocalDate.now()`,
`LocalDateTime.now()`, `System.currentTimeMillis()`, and other time-retrieval methods. For example,
if you configure the agent to simulate a time 5 days in the future, it will intercept and adjust these method calls
accordingly.

## Building the Agent

To build Mock-Time-Agent, use the following Gradle command which creates a shadow JAR containing all dependencies:

`./gradlew build`

The agent JAR is generated at: `app/build/libs/mock-time-agent.jar`

## Usage Instructions

Before starting your application, define the `MOCK_START_TIME` environment variable:

```shell
export MOCK_START_TIME="YYYY-MM-DD hh:mm:ss"
```

Optionally, define which packages to include or exclude for time alteration using `MOCK_TIME_INCLUDE`
and `MOCK_TIME_EXCLUDE`:

* `MOCK_TIME_INCLUDE`: Semicolon-separated list of packages to be affected by time change.
* `MOCK_TIME_EXCLUDE`: Semicolon-separated list of packages to exclude from time change. Takes precedence
  over `MOCK_TIME_INCLUDE`.

Launch your Java application with the `Mock-Time-Agent` using the `-javaagent` flag:

```shell
java -javaagent:<path-to-mock-time-agent.jar> -jar <your-application>.jar
```

## Testing

### Quick Test with Agent's Own Main Class

The agent JAR includes a main class for quick testing:

```shell
export MOCK_START_TIME="2025-12-31 23:59:59"
java -javaagent:app/build/libs/mock-time-agent.jar -jar app/build/libs/mock-time-agent.jar
```

or alternatively:

```shell
MOCK_START_TIME="2025-12-31 23:59:59" java -javaagent:app/build/libs/mock-time-agent.jar -jar app/build/libs/mock-time-agent.jar
```

Expected output:

```
******** Mock-Time-Agent Initialization ********

Start date: 2025-12-31T23:59:59-05:00[America/New_York]
packagesToInclude: []
packagesToExclude: []
Version: v2.0.1

****** Mock-Time-Agent Initialization Done *****

Mock Time Agent 
Usage: 
   export MOCK_START_TIME="<YYYY-MM-DD hh:mm:ss>"
   export MOCK_TIME_INCLUDE="com.example;io.github.jtag84.mocktimeagent;org.example"
   export MOCK_TIME_EXCLUDE="com.exclude;com.google"
   java -javaagent:mock-time-agent.jar -jar jarToRun

Waiting 2 seconds ...
test Clock.fixed(LocalDate.of(2022, Month.MAY, 4).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.systemDefault()).instant(): 2022-05-04T00:00:00Z
test Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(14)).instant(): 2026-01-01T19:00:01.085612Z
test Clock.system(ZoneId.systemDefault()).instant(): 2026-01-01T05:00:01.086108Z
test Clock.systemDefaultZone().instant(): 2026-01-01T05:00:01.086197Z
test Clock.systemUTC().instant(): 2026-01-01T05:00:01.086217Z
test Clock.tick(Clock.systemDefaultZone(), Duration.ofSeconds(2)).instant(): 2026-01-01T05:00:00Z
test Clock.tickMillis(ZoneId.systemDefault()).instant(): 2026-01-01T05:00:01.086478Z
test Clock.tickMinutes(ZoneId.systemDefault()).instant(): 2026-01-01T04:59:42.542478Z
test Clock.tickSeconds(ZoneId.systemDefault()).instant(): 2026-01-01T05:00:00.542478Z
test new Date(): Thu Jan 01 00:00:01 EST 2026
test Instant.now(): 2026-01-01T05:00:01.091737Z
test LocalDate.now(): 2026-01-01
test LocalDateTime.now(): 2026-01-01T01:00:01.091820
test LocalDateTime.now(Clock.systemDefaultZone()): 2026-01-01T00:00:01.091836
test LocalTime.now(): 01:00:01.091890
test LocalTime.now(Clock.systemUTC()): 05:00:01.091915
test MonthDay.now(): --01-01
test OffsetDateTime.now(): 2026-01-01T01:00:01.092355-04:00
test OffsetTime.now(): 01:00:01.092488-04:00
test System.currentTimeMillis(): 1767243601092 -> 2026-01-01T00:00:01.092 
test Year.now(): 2026
test YearMonth.now(): 2026-01
test ZonedDateTime.now(): 2026-01-01T00:00:01.097073-05:00[America/New_York]
```

### Testing with a Spring Boot Application

Modify time for a Spring Boot project, including scheduled tasks, without affecting AWS services:

```shell
MOCK_START_TIME="2025-12-31 23:59:59" MOCK_TIME_INCLUDE="io.github.jtag84.mocktimeagent;org.springframework" MOCK_TIME_EXCLUDE="io.github.jtag84.mocktimeagent.MyAwsServiceImpl" java -javaagent:<path-to-mock-time-agent.jar> -jar spring-boot-app-example.jar
```

This setup excludes MyAwsServiceImpl to avoid AWS service disruptions, while the included classes and packages will
simulate the configured future time. Logging timestamps (`ch.qos.logback`) and scheduled tasks (`org.springframework`)
will reflect the time adjustment.

Remember to replace placeholders like `path-to-mock-time-agent.jar` and your-application.jar with actual paths
before running the commands. The inclusion of `ch.qos.logback` affects log timestamps, so if this is not desirable,
you can omit it. The `org.springframework` package is included to ensure that any scheduled tasks within
the Spring Boot application respect the simulated time.






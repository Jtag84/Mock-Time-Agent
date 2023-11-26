# Mock-Time-Agent

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
export MOCK_START_TIME="2023-01-03 12:34:56"
java -javaagent:app/build/libs/mock-time-agent.jar -jar app/build/libs/mock-time-agent.jar
```

or alternatively:

```shell
MOCK_START_TIME="2023-01-03 12:34:56" java -javaagent:app/build/libs/mock-time-agent.jar -jar app/build/libs/mock-time-agent.jar
```

Expected output:

```
******** Mock-Time-Agent Initialization ********

Start date: 2023-01-03T12:34:56
version: unspecified
packagesToInclude: []
packagesToExclude: []

****** Mock-Time-Agent Initialization Done *****

Mock Time Agent 
Usage: 
   export MOCK_START_TIME="<YYYY-MM-DD hh:mm:ss>"
   export MOCK_TIME_INCLUDE="com.example;io.github.jtag84.mocktimeagent;org.example"
   export MOCK_TIME_EXCLUDE="com.exclude;com.google"
   java -javaagent:mock-time-agent.jar -jar jarToRun

test new Date(): Tue Jan 03 12:34:56 EST 2023
test Instant.now(): 2023-01-03T17:34:56.086263Z
test LocalDate.now(): 2023-01-03
test LocalDateTime.now(): 2023-01-03T12:34:56.086745
test LocalTime.now(): 12:34:56.086769
test MonthDay.now(): --01-03
test OffsetDateTime.now(): 2023-01-03T12:34:56.086980-05:00
test OffsetTime.now(): 12:34:56.087051-05:00
test System.currentTimeMillis(): 1672767296087
test Year.now(): 2023
test YearMonth.now(): 2023-01
test ZonedDateTime.now(): 2023-01-03T12:34:56.087100-05:00[America/New_York]
```

## Testing with a Spring Boot Application

Modify time for a Spring Boot project, including scheduled tasks, without affecting AWS services:

```shell
MOCK_START_TIME="2023-01-03 11:59:00" MOCK_TIME_INCLUDE="io.github.jtag84.mocktimeagent;org.springframework" MOCK_TIME_EXCLUDE="io.github.jtag84.mocktimeagent.MyAwsServiceImpl" java -javaagent:<path-to-mock-time-agent.jar> -jar spring-boot-app-example.jar
```

This setup excludes MyAwsServiceImpl to avoid AWS service disruptions, while the included classes and packages will
simulate the configured future time. Logging timestamps (`ch.qos.logback`) and scheduled tasks (`org.springframework`)
will reflect the time adjustment.

Remember to replace placeholders like `path-to-mock-time-agent.jar` and your-application.jar with actual paths
before running the commands. The inclusion of `ch.qos.logback` affects log timestamps, so if this is not desirable,
you can omit it. The `org.springframework` package is included to ensure that any scheduled tasks within
the Spring Boot application respect the simulated time.






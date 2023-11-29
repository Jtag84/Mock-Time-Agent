/*
 * Copyright (c) 2023 Clément Vasseur. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.github.jtag84.mocktimeagent

import java.time.*
import java.util.*

private const val TWO_SECONDS_IN_MS = 2_000L

fun main() {
    printUsage()

    println("Waiting 2 seconds ...")
    Thread.sleep(TWO_SECONDS_IN_MS)

    println(
        """
        |test new Date(): ${Date()}
        |test Instant.now(): ${Instant.now()}
        |test LocalDate.now(): ${LocalDate.now()}
        |test LocalDateTime.now(): ${LocalDateTime.now()}
        |test LocalTime.now(): ${LocalTime.now()}
        |test MonthDay.now(): ${MonthDay.now()}
        |test OffsetDateTime.now(): ${OffsetDateTime.now()}
        |test OffsetTime.now(): ${OffsetTime.now()}
        |test System.currentTimeMillis(): ${System.currentTimeMillis()}
        |test Year.now(): ${Year.now()}
        |test YearMonth.now(): ${YearMonth.now()}
        |test ZonedDateTime.now(): ${ZonedDateTime.now()}
        """.trimMargin()
    )
}

fun printUsage() {
    println(
        """
        |Mock Time Agent 
        |Usage: 
        |   export MOCK_START_TIME="<YYYY-MM-DD hh:mm:ss>"
        |   export MOCK_TIME_INCLUDE="com.example;io.github.jtag84.mocktimeagent;org.example"
        |   export MOCK_TIME_EXCLUDE="com.exclude;com.google"
        |   java -javaagent:mock-time-agent.jar -jar jarToRun
        |
        """.trimMargin()
    )
}
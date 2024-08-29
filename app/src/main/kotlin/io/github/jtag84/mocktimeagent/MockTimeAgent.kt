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

import java.lang.instrument.Instrumentation
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

object MockTimeAgent {
    @JvmStatic
    fun agentmain(args: String?, instrumentation: Instrumentation) {
        premain(args, instrumentation)
    }

    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        println("******** Mock-Time-Agent Initialization ********\n")

        try {
            val startDate = System.getenv("MOCK_START_TIME")
                ?.let { LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.systemDefault()) }
                ?: throw IllegalArgumentException("MOCK_START_TIME needs to be defined <yyyy-MM-dd HH:mm:ss>")

            val packagesToInclude = getStringListParameter("MOCK_TIME_INCLUDE")
            val packagesToExclude = getStringListParameter("MOCK_TIME_EXCLUDE")

            val transformer = MockTimeTransformer(startDate, packagesToInclude, packagesToExclude)

            val version = getAgentVersion()

            println(
                """
                    |Start date: $startDate
                    |packagesToInclude: $packagesToInclude
                    |packagesToExclude: $packagesToExclude
                    |Version: $version
                """.trimMargin()
            )

            instrumentation.addTransformer(transformer)
        } catch (t: Throwable) {
            t.printStackTrace()
            printUsage()
            exitProcess(1)
        }

        println("\n****** Mock-Time-Agent Initialization Done *****\n")
    }

    private fun getStringListParameter(environmentVariableName: String): List<String> =
        System.getenv(environmentVariableName)?.split(";") ?: emptyList()

    private fun getAgentVersion(): String? {
        val resources = Thread.currentThread().contextClassLoader.getResources("META-INF/MANIFEST.MF")
        while (resources.hasMoreElements()) {
            val url = resources.nextElement()
            val manifest = java.util.jar.Manifest(url.openStream())
            if (manifest.mainAttributes.getValue("Implementation-Title") == "Mock Time Agent") {
                return manifest.mainAttributes.getValue("Implementation-Version")
            }
        }
        return null
    }
}
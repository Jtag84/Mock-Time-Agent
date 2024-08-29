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

import javassist.ClassPool
import javassist.CtClass
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import javassist.expr.NewExpr
import java.io.ByteArrayInputStream
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*


class MockTimeTransformer(
    startDate: ZonedDateTime,
    packagesToInclude: Collection<String>,
    packagesToExclude: Collection<String>
) : ClassFileTransformer {
    private val packagesToInclude = toJavassistPackageNames(packagesToInclude)
    private val packagesToExclude = toJavassistPackageNames(packagesToExclude)
    private val nowEditor: ExprEditor

    private val now = ZonedDateTime.now()
    private val nanoSecondsOffset = ChronoUnit.NANOS.between(now, startDate)
    private val milliSecondsOffset = ChronoUnit.MILLIS.between(now, startDate)

    private val clockMethodsToIntercept = setOf(
        "systemUTC", "systemDefaultZone", "system",
        "tickSeconds", "tickMinutes", "tickMillis",
    )

    init {
        nowEditor = object : ExprEditor() {
            override fun edit(newExpr: NewExpr) {
                if (newExpr.className == Date::class.java.name) {
                    newExpr.replace("\$_ = java.util.Date.from(java.time.Instant.now().plusNanos(${nanoSecondsOffset}L));")
                }
            }

            override fun edit(methodCall: MethodCall) {
                when {
                    methodCall.className == System::class.java.name && methodCall.methodName == "currentTimeMillis" -> {
                        val replacement = "\$_ = (\$proceed(\$\$) + ${milliSecondsOffset}L);"
                        methodCall.replace(replacement)
                    }
                    // Capturing calls to now(..) but skipping calls to now(Clock) since we're already modifying
                    // the Clock this would lead to doubling the time adjustment
                    methodCall.className.startsWith("java.time.")
                            && methodCall.methodName == "now"
                            && methodCall.withNoClockParameter() -> {
                        val alteredLocalDateTime = "java.time.LocalDateTime.now(\$\$).plusNanos(${nanoSecondsOffset}L)"
                        val replacement =
                            when (methodCall.className) {
                                Instant::class.java.name,
                                LocalDateTime::class.java.name,
                                OffsetDateTime::class.java.name,
                                OffsetTime::class.java.name,
                                LocalTime::class.java.name,
                                ZonedDateTime::class.java.name -> "\$_ = \$proceed(\$\$).plusNanos(${nanoSecondsOffset}L);"
                                LocalDate::class.java.name -> "\$_ = java.time.LocalDate.from($alteredLocalDateTime);"
                                MonthDay::class.java.name -> "\$_ = java.time.MonthDay.from($alteredLocalDateTime);"
                                Year::class.java.name -> "\$_ = java.time.Year.from($alteredLocalDateTime);"
                                YearMonth::class.java.name -> "\$_ = java.time.YearMonth.from($alteredLocalDateTime);"
                                else -> return
                            }
                        methodCall.replace(replacement)
                    }
                    methodCall.className == Clock::class.java.name && methodCall.methodName in clockMethodsToIntercept -> {
                        val replacement = "\$_ = java.time.Clock.offset(\$proceed(\$\$), java.time.Duration.ofNanos(${nanoSecondsOffset}L));"
                        methodCall.replace(replacement)
                    }
                }
            }

            private fun MethodCall.withNoClockParameter() =
                this.method.parameterTypes
                .map(CtClass::getName)
                .none { parameterName -> parameterName == "java.time.Clock" }
        }
    }

    private fun toJavassistPackageNames(packages: Collection<String>): Set<String> =
        packages.map { it.replace(".", "/") }.toSet()

    override fun transform(
        loader: ClassLoader?,
        className: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        if (className.shouldNotBeTransformed()) {
            return classfileBuffer
        }

        return try {
            val classPool = ClassPool.getDefault()
            val ctClass = classPool.makeClass(ByteArrayInputStream(classfileBuffer))

            ctClass.declaredBehaviors.forEach { behavior ->
                behavior.instrument(nowEditor)
            }

            ctClass.toBytecode()
        } catch (e: Throwable) {
            e.printStackTrace()
            classfileBuffer
        }
    }

    private fun String?.shouldNotBeTransformed(): Boolean {
        val isFromPackagesToInclude = packagesToInclude.isEmpty() || this?.let { className ->
            packagesToInclude.any { className.startsWith(it) }
        } ?: false

        val isFromPackagesToExclude by lazy {
            this?.let { className ->
                packagesToExclude.any { className.startsWith(it) }
            } ?: true
        }

        return isFromPackagesToInclude.not() || isFromPackagesToExclude
    }
}

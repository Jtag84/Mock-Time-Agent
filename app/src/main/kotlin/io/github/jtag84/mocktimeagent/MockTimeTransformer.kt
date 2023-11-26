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
    startDate: LocalDateTime,
    packagesToInclude: Collection<String>,
    packagesToExclude: Collection<String>
) : ClassFileTransformer {
    private val packagesToInclude = toJavassistPackageNames(packagesToInclude)
    private val packagesToExclude = toJavassistPackageNames(packagesToExclude)
    private val nowEditor: ExprEditor

    private val now = LocalDateTime.now()
    private val nanoSecondsOffset = ChronoUnit.NANOS.between(now, startDate)
    private val milliSecondsOffset = ChronoUnit.MILLIS.between(now, startDate)
    private val daysOffset = ChronoUnit.DAYS.between(now.toLocalDate(), startDate.toLocalDate())
    private val monthsOffset = ChronoUnit.MONTHS.between(YearMonth.from(now), YearMonth.from(startDate))
    private val yearsOffset = ChronoUnit.YEARS.between(Year.from(now), Year.from(startDate))

    init {
        nowEditor = object : ExprEditor() {
            override fun edit(newExpr: NewExpr) {
                if (newExpr.className == Date::class.java.name) {
                    newExpr.replace("\$_ = java.util.Date.from(java.time.LocalDateTime.now().plusNanos(${nanoSecondsOffset}L).atZone(java.time.ZoneId.systemDefault()).toInstant());")
                }
            }

            override fun edit(m: MethodCall) {
                when {
                    m.className == System::class.java.name && m.methodName == "currentTimeMillis" -> {
                        val s = "\$_ = (\$proceed(\$\$) + ${milliSecondsOffset}L);"
                        m.replace(s)
                    }

                    m.methodName == "now" -> {
                        val replacement =
                            when (m.className) {
                                Instant::class.java.name,
                                LocalDateTime::class.java.name,
                                OffsetDateTime::class.java.name,
                                OffsetTime::class.java.name,
                                LocalTime::class.java.name,
                                ZonedDateTime::class.java.name -> "\$_ = \$proceed(\$\$).plusNanos(${nanoSecondsOffset}L);"
                                LocalDate::class.java.name -> "\$_ = \$proceed($$).plusDays(${daysOffset}L);"
                                MonthDay::class.java.name -> "\$_ = java.time.MonthDay.from(java.time.LocalDate.now(\$\$).plusDays(${daysOffset}L));"
                                Year::class.java.name -> "\$_ = \$proceed(\$\$).plusYears(${yearsOffset}L);"
                                YearMonth::class.java.name -> "\$_ = \$proceed(\$\$).plusMonths(${monthsOffset}L);"
                                else -> return
                            }
                        m.replace(replacement)
                    }
                }
            }
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

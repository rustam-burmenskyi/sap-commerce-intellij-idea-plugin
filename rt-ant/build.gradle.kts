/*
 * This file is part of "hybris integration" plugin for Intellij IDEA.
 * Copyright (C) 2014-2016 Alexander Bartash <AlexanderBartash@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

plugins {
    id("org.jetbrains.intellij")
}

intellij {
    version = platformVersion
}
// Should be 1.7 otherwise it will not work in Hybris 5.X which require java 1.7.
java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}

sourceSets.main {
    java.srcDirs(
        file("src")
    )
}

dependencies {
    compileOnly("org.apache.ant:ant:$antVersion")
}

val jar: Jar by tasks
jar.archiveFileName.set("rt-ant.jar")
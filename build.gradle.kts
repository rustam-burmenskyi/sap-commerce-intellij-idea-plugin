/*
 * This file is part of "SAP Commerce Developers Toolset" plugin for Intellij IDEA.
 * Copyright (C) 2019-2020 EPAM Systems <hybrisideaplugin@epam.com>
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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.6.5"
}

sourceSets.main {
    java.srcDirs(
        file("src"),
        file("gen")
    )
    resources.srcDir(file("resources"))
}

allprojects {
    apply {
        plugin("java")
    }

    repositories {
        jcenter()
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

intellij {
    type = "IU"
    version = "LATEST-EAP-SNAPSHOT"
    pluginName = pluginName_
    downloadSources = false
    updateSinceUntilBuild = intellijUpdateSinceUntilBuild

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    setPlugins(*platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty).toTypedArray())
}

tasks {

    buildSearchableOptions {
        enabled = false
    }

    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = sourceVersion
            apiVersion = kotlinApiVersion
            languageVersion = kotlinApiVersion
        }
    }

    runIde {
        jvmArgs = listOf(intellijJvmArgs)
    }

    patchPluginXml {
        version(pluginVersion)
        sinceBuild(pluginSinceBuild)
        untilBuild(pluginUntilBuild)
    }

    runPluginVerifier {
        ideVersions(pluginVerifierIdeVersions)
    }

    clean {
        doFirst {
            delete("out")
        }
    }
}

dependencies {

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinReflect")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("com.wutka:dtdparser:$dtdparserVersion")
    implementation("commons-io:commons-io:$commonsIOVersion")
    implementation("com.google.code.findbugs:jsr305:$findbugsVersion")
    implementation("org.apache.maven:maven-model:$mavenModelVersion")
    implementation("commons-codec:commons-codec:$commonsCodecVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLang3Version")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
    implementation("org.apache.commons:commons-collections4:$commonsCollections4Version")

    implementation("org.apache.solr:solr-solrj:$solrjVersion") {
        exclude("org.slf4j", "slf4j-api")
        exclude("org.apache.httpcomponents", "httpclient")
        exclude("org.apache.httpcomponents", "httpcore")
        exclude("org.apache.httpcomponents", "httpmime")
    }

    implementation(project(":rt-ant"))
}


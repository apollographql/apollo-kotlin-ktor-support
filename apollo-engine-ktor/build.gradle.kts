@file:OptIn(ExperimentalWasmDsl::class, ExperimentalKotlinGradlePluginApi::class)

import com.gradleup.librarian.gradle.Librarian
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

Librarian.module(project)

kotlin {
  jvm()
  macosX64()
  macosArm64()
  iosArm64()
  iosX64()
  iosSimulatorArm64()
  watchosArm32()
  watchosArm64()
  watchosDeviceArm64()
  watchosSimulatorArm64()
  tvosArm64()
  tvosX64()
  tvosSimulatorArm64()
  js(IR) {
    nodejs()
  }
  wasmJs {
    nodejs()
  }

  sourceSets {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
      group("common") {
        group("noWasm") {
          group("concurrent") {
            group("apple")
            withJvm()
          }
          withJvm()
          withJs()
        }
        group("jsCommon") {
          group("js") {
            withJs()
          }
          group("wasmJs") {
            withWasmJs()
          }
        }
      }
    }

    commonMain {
      dependencies {
        implementation(kotlin("stdlib"))
        api(libs.apollo.runtime)
        implementation(libs.kotlinx.coroutines.core)
        api(libs.ktor.client.core)
        api(libs.ktor.client.websockets)
      }
    }
    
    commonTest {
      dependencies {
        implementation(libs.apollo.engine.tests)
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.kotlinx.coroutines.core)
      }
    }

    jvmMain {
      dependencies {
        api(libs.ktor.client.okhttp)
      }
    }

    appleMain {
      dependencies {
        api(libs.ktor.client.darwin)
      }
    }
  }
}

import com.gradleup.librarian.gradle.Librarian

plugins {
  id("base")
}

buildscript {
  dependencies {
    classpath(libs.kotlin.gradle.plugin)
    classpath(libs.librarian.gradle.plugin)
  }
}

Librarian.root(project)
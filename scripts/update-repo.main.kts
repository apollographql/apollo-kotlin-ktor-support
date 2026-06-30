#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://dl.google.com/android/maven2/")
@file:Repository("https://storage.googleapis.com/gradleup/m2")
//@file:Repository("file://~/.m2/repository")
@file:DependsOn("com.gradleup.librarian:librarian-cli:0.2.2-SNAPSHOT-7f4d4f5f59d9cbba4c8fc717af3ec9fde178ff12")

import com.gradleup.librarian.repo.librarianSetVersion
import com.gradleup.librarian.repo.updateRepo

updateRepo(
    setVersion = {
        librarianSetVersion()
    },
    setVersionInDocs = {
        file("Writerside/v.list") {
            replaceRegex(Regex("(name=\"latest_version\" .* value=\")[^\"]\"")) { "${it.groupValues.get(1)}\"" }
        }
    },
)

package org.jetbrains.idea.inspections

import org.gradle.api.Project

val Project.projectVersion: String get() = this.findProperty("version") as String
val Project.projectGroup: String get() = this.findProperty("group") as String

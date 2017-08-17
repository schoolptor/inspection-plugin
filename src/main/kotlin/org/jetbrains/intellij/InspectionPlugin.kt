package org.jetbrains.intellij

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.quality.CodeQualityExtension
import org.gradle.api.plugins.quality.internal.AbstractCodeQualityPlugin
import org.gradle.api.tasks.SourceSet
import java.io.File
import java.util.concurrent.Callable

open class InspectionPlugin : AbstractCodeQualityPlugin<Inspection>() {

    companion object {
        private val LOG: Logger = Logging.getLogger(InspectionPlugin::class.java)

        val DEFAULT_IDEA_VERSION = "ideaIC:2017.2"
    }

    private val inspectionExtension: InspectionPluginExtension get() = extension as InspectionPluginExtension

    override fun getToolName(): String = "IDEA Inspections"

    override fun getTaskType(): Class<Inspection> = Inspection::class.java

    private val shortName = "inspections"

    override fun getConfigurationName(): String = shortName

    override fun getTaskBaseName(): String = shortName

    override fun getReportName(): String = shortName

    override fun createExtension(): CodeQualityExtension {
        val extension = project.extensions.create(shortName, InspectionPluginExtension::class.java, project)
        extension.ideaVersion = DEFAULT_IDEA_VERSION

        extension.configDir = project.file("config/inspections")
        extension.config = project.resources.text.fromFile(Callable<File> { File(extension.configDir, "inspections.xml") })
        return extension
    }

    override fun beforeApply() {
        super.beforeApply()

        project.repositories.maven { it.setUrl("https://www.jetbrains.com/intellij-repository/releases") }
        project.tasks.create("unzip", UnzipTask::class.java)
    }

    override fun configureTaskDefaults(task: Inspection, baseName: String) {
        val configuration = project.configurations.getAt(shortName)

        val unzipTask = project.tasks.getAt("unzip") as UnzipTask
        task.setShouldRunAfter(listOf(unzipTask))

        configureDefaultDependencies(configuration)
        configureTaskConventionMapping(task)
        configureReportsConventionMapping(task, baseName)
    }

    private fun configureDefaultDependencies(configuration: Configuration) {
        LOG.info("XXXXXXXXXXX Configuring default dependencies for configuration ${configuration.name}")
        if (configuration.dependencies.isEmpty()) {
            configuration.dependencies.add(project.dependencies.create("com.jetbrains.intellij.idea:${inspectionExtension.ideaVersion}"))
            configuration.dependencies.add(project.dependencies.create(project.fileTree(mapOf("dir" to "lib/idea/lib", "include" to "*.jar"))))
        }
        LOG.info(configuration.dependencies.joinToString(separator = ",\n") {
            when (it) {
                is SelfResolvingDependency -> it.resolve().joinToString(prefix = "File set: ", limit = 3) { it.absolutePath }
                else -> it.toString()
            }
        })
    }

    private fun configureTaskConventionMapping(task: Inspection) {
        LOG.info("XXXXXXXXXXX Configuring task ${task.name} convention mapping")
        val taskMapping = task.conventionMapping
        taskMapping.map("config") { inspectionExtension.config }
        taskMapping.map("configProperties") { inspectionExtension.configProperties }
        taskMapping.map("ignoreFailures") { inspectionExtension.isIgnoreFailures }
        taskMapping.map("showViolations") { inspectionExtension.isShowViolations }
        taskMapping.map("maxErrors") { inspectionExtension.maxErrors }
        taskMapping.map("maxWarnings") { inspectionExtension.maxWarnings }
    }

    private fun configureReportsConventionMapping(task: Inspection, baseName: String) {
        task.reports.all { report ->
            val reportMapping = AbstractCodeQualityPlugin.conventionMappingOf(report)
            reportMapping.map("enabled") { true }
            reportMapping.map("destination") { File(inspectionExtension.reportsDir, "$baseName.${report.name}") }
        }
    }

    override fun configureForSourceSet(sourceSet: SourceSet, task: Inspection) {
        task.description = "Run IDEA inspections for " + sourceSet.name + " classes"
        task.classpath = sourceSet.output.plus(
                sourceSet.compileClasspath
        ).plus(
                project.fileTree(mapOf("dir" to "lib/idea/lib", "include" to "*.jar"))
        )
        task.setSourceSet(sourceSet.allSource)
    }
}

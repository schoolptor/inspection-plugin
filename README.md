[![JetBrains incubator project](http://jb.gg/badges/incubator-plastic.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![TeamCity (simple build status)](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/ProjectsWrittenInKotlin_InspectionPlugin.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=ProjectsWrittenInKotlin_InspectionPlugin&branch_Kotlin=%3Cdefault%3E&tab=buildTypeStatusDiv)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

# IDEA inspection plugin

This plugin is intended to run IDEA inspections during Gradle build.

Current status: alpha version 0.1.3 available.

## Usage

* Add `maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }` to your buildscript repositories (temporary location)
* Add `classpath 'org.jetbrains.intellij.plugins:inspection-plugin:0.1.3'` to your buildscript dependencies
* Apply plugin `'org.jetbrains.intellij.inspections'` to your gradle module

This adds one inspection plugin task per source root, 
normally its name is `inspectionsMain` for `main` root
and `inspectionsTest` for `test` root respectively.

Also you should specify IDEA version to use, e.g.

```groovy
inspections {
    toolVersion "ideaIC:2017.2.6"
}
``` 

In this example inspections will be taken from IDEA CE version 2017.2.6. 
Plugin works at least with IDEA CE versions 2017.2, 2017.2.x, 2017.3, 2017.3.x 
(last two supported by inspection plugin 0.1.3 or later).

And the last necessary thing is configuration file, 
which is located (by default) in file `config/inspections/inspections.xml`.
The simplest possible format is the following:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<inspections>
    <inheritFromIdea profileName="Project_Default.xml"/>
    <errors/>
    <warnings/>
    <infos/>
</inspections>
```

In this case inspection configuration will be inherited from IDEA,
inspection configuration will be read from file `.idea/inspectionProfiles/Project_Default.xml`.
If this file does not exist in your project, then default inspection severities will be in use.
If `profileName` is not given, `Project_Default.xml` will be used by default.

To run inspections, execute from terminal: `gradlew inspectionsMain`.
This will download IDEA artifact to gradle cache,
unzip it to cached temporary dir and launch all inspections.
You will see inspection messages in console as well as in report XML located in `build/reports/inspections/main.xml`.

You can find example usage in `sample` project subdirectory.

## Additional options

You can specify additional options in `inspections` closure, e.g.:

```groovy
inspections {
    maxErrors = 5
    maxWarnings = 20
    ignoreFailures = true
    quiet = true
    config = "inspections.xml"
}
```

The meaning of the parameters is the following:

* `maxErrors`: after exceeding the given number of inspection diagnostics with "error" severity, inspection task stops and fails (1 by default)
* `maxWarnings`: after exceeding the given number of inspection diagnostics with "warning" severity, inspection task stops and fails (100 by default)
* `ignoreFailures`: inspection task never fails (false by default)
* `quiet`: do not report inspection messages to console, only to XML file (false by default)
* `config`: configuration file location

If you with to change location of report file, you should specify it in closure for particular task, e.g.

```groovy
inspectionsMain {
    reports {
        xml {
            destination "reportFileName"
        }
        html {
            // Available from version 0.1.2
            destination "reportFileName"
        }
    }
}
```

## Bugs and Problems

You can report issues on the relevant tab: https://github.com/mglukhikh/inspection-plugin/issues

It's quite probable that plugin does not work yet in some environment.
It may result in various exceptions during IDEA configuration process. 
If you found such a case, please execute:

```
gradlew --stop
gradlew --info --stacktrace inspectionsMain > inspections.log
```

and attach `inspections.log` to the issue. 
Also it's very helpful to specify Gradle version, OS and 
IDEA version used in inspection plugin (which is set in `toolVersion` parameter).

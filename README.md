# Container-JFR-Core

Core library providing a convenience wrapper and headless stubs for managing
JFR with JDK Mission Control API

## Requirements
Build:
- Gradle
- JDK11+
- JMC 8.0.0-SNAPSHOT artefacts

## Build

The gradle build expects to be able to find Java Mission Control (JMC) 8
artefacts in the local Maven repository. To ensure these are available, clone
the JMC project at the [JMC homepage](https://hg.openjdk.java.net/jmc/jmc7)
and follow its build instructions. Run `mvn install` in the jmc project root to
install its artefacts to the local repository. After this is complete, the
project in this repository may be built locally.

`./gradlew build` to compile this core library and publish the artifacts to the
local Maven repository for consumption by other Maven/Gradle projects.
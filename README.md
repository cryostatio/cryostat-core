# TODO

## Build

### Requirements

- JDK11+
- JMC 7.1.0 artefacts

The gradle build expects to be able to find Java Mission Control (JMC) 7
artefacts in the local Maven repository. To ensure these are available, clone
the JMC project at the [JMC homepage](https://hg.openjdk.java.net/jmc/jmc7)
and follow its build instructions. Run `mvn install` in the jmc project root to
install its artefacts to the local repository. After this is complete, the
project in this repository may be built locally.

### Instructions

`./gradlew build` to compile this core library and publish the artifacts to the
local Maven repository for consumption by other Maven/Gradle projects.

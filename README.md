# Cryostat-Core

![Build Status](https://github.com/cryostatio/cryostat-core/actions/workflows/ci.yaml/badge.svg)

Core library providing a convenience wrapper and headless stubs for managing
JFR with JDK Mission Control API

## Requirements
Build:
- Maven
- JDK11+

## Build

`mvn install` to compile this core library and publish the artifacts to the
local Maven repository for consumption by other projects.

Consumers of this build may pull it from the GitHub Packages registry. This
registry requires authentication.

Add or merge the following configuration into your `$HOME/.m2/settings.xml`,
creating the file if it does not exist:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github-cryostat-core</id>
      <username>$MY_GITHUB_USERNAME</username>
      <password>$MY_GITHUB_ACCESSTOKEN</password>
    </server>
  </servers>
</settings>
```

The token must have the `read:packages` permission. It is recommended that this
is the *only* permission the token has.

Then, add the following to your build's `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github-cryostat-core</id>
    <url>https://maven.pkg.github.com/cryostatio/cryostat-core</url>
  </repository>
</repositories>
```

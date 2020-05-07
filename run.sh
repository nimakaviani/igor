#!/bin/bash
set -ex

SPRING_CONFIG_NAME=igor SPRING_PROFILES_ACTIVE=local SPRING_CONFIG_LOCATION="/Users/nkaviani/workspaces/spinnaker/igor/igor-web/config/" /usr/bin/java  -Xms64m -Xdock:name=Gradle -Xdock:icon=/Users/nima/dev/spinnaker/gate/media/gradle.icns -Dorg.gradle.appname=gradlew -classpath /Users/nkaviani/workspaces/spinnaker/igor/gradle/wrapper/gradle-wrapper.jar:springboot.jks  org.gradle.wrapper.GradleWrapperMain

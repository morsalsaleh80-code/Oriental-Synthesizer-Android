#!/usr/bin/env bash
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$DIR"
JAVA_OPTS="${JAVA_OPTS:-}"
JAVACMD="${JAVACMD:-java}"
exec "$JAVACMD" $JAVA_OPTS -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"

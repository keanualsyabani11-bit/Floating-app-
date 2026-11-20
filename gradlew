#!/bin/sh
APP_BASE_NAME=`basename "$0"`
APP_HOME="`pwd`"
DEFAULT_JVM_OPTS="-Xmx512m -Xms256m"

die () { echo; echo "$*"; echo; exit 1; } >&2
warn () { echo "$*"; } >&2

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
    [ ! -x "$JAVACMD" ] && die "ERROR: JAVA_HOME invalid: $JAVA_HOME"
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: no java found"
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" $DEFAULT_JVM_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"

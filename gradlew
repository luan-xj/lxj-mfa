#!/usr/bin/env sh

# LXJ-MFA Gradle Wrapper (轻量封装)
# 首次运行会自动下载 Gradle 8.9 到 ~/.gradle/wrapper/dists

APP_HOME=$( cd "$(dirname "$0")" && pwd )

if [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=java
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper-main.jar:$APP_HOME/gradle/wrapper/gradle-wrapper-shared.jar"

exec "$JAVA" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"

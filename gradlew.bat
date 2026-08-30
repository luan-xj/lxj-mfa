@echo off
rem LXJ-MFA Gradle Wrapper (轻量封装)
rem 首次运行会自动下载 Gradle 8.9 到 ~/.gradle/wrapper/dists

set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper-main.jar;%APP_HOME%gradle\wrapper\gradle-wrapper-shared.jar

if defined JAVA_HOME (set JAVA="%JAVA_HOME%\bin\java") else (set JAVA=java)

%JAVA% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

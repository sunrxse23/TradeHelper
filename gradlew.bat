@echo off
set DIR=%~dp0
if "%DIR%"=="" set DIR=.
java -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*

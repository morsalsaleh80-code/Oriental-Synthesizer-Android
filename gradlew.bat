@echo off
setlocal
set DIR=%~dp0
set JAVA_OPTS=%JAVA_OPTS%
set JAVACMD=%JAVACMD% java
"%JAVACMD%" %JAVA_OPTS% -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*

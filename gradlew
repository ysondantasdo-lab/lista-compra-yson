#!/usr/bin/env sh

DIRNAME=$(dirname "$0")
ls "$DIRNAME" > /dev/null
exec java -Xmx64m -jar "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" "$@"

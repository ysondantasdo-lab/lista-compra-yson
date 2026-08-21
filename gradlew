#!/usr/bin/env sh

# Executável padrão do Gradle para sistemas Linux/macOS (usado pelo CodeMagic)
DIRNAME=$(dirname "$0")
ls "$DIRNAME" > /dev/null
exec "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" "$@"

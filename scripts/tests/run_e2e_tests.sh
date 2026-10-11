#!/bin/bash
set -e

# Change directory to project root if run from elsewhere
cd "$(dirname "$0")/../.."

echo "=== 1. Preparing directories ==="
mkdir -p scripts/tests/bin

echo "=== 2. Compiling mock stubs and shadow classes ==="
find scripts/tests/mocks -name "*.java" > compile_mocks.txt
javac -d scripts/tests/bin @compile_mocks.txt
rm compile_mocks.txt

echo "=== 3. Resolving Android SDK and Gradle Classpath ==="
SDK_DIR=$(grep 'sdk.dir' local.properties | cut -d'=' -f2 | xargs)
ANDROID_JAR=$(ls -1d "$SDK_DIR"/platforms/android-* | sort -V | tail -n1)/android.jar
echo "Using Android SDK Platform JAR: $ANDROID_JAR"

# Run Gradle printClasspath task to extract all dependency paths dynamically
GRADLE_CLASSPATH=$(./gradlew -q -I scripts/tests/init.gradle :app:printClasspath | grep "CLASSPATH_ENTRY:" | cut -d':' -f2- | tr '\n' ':')

echo "=== 4. Compiling E2ETestSuite and E2ETestRunner ==="
javac -d scripts/tests/bin \
  -cp "scripts/tests/bin:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/:app/build/tmp/kotlin-classes/debug/:$GRADLE_CLASSPATH:$ANDROID_JAR" \
  scripts/tests/E2ETestSuite.java scripts/tests/E2ETestRunner.java

echo "=== 5. Running Headless E2E Test Suite ==="
java -cp "scripts/tests/bin:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/:app/build/tmp/kotlin-classes/debug/:$GRADLE_CLASSPATH" \
  scripts.tests.E2ETestRunner

echo "=== 6. Testing G-code slot routing and Anycubic upload preparation ==="
javac -d scripts/tests/bin \
  -cp "scripts/tests/bin:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/:app/build/tmp/kotlin-classes/debug/:$GRADLE_CLASSPATH" \
  scripts/tests/GCodeToolMapperTest.java \
  scripts/tests/AnycubicGCodeProcessorTest.java
java -cp "scripts/tests/bin:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/:app/build/tmp/kotlin-classes/debug/:$GRADLE_CLASSPATH" scripts.tests.GCodeToolMapperTest
java -cp "scripts/tests/bin:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/:app/build/tmp/kotlin-classes/debug/:$GRADLE_CLASSPATH" scripts.tests.AnycubicGCodeProcessorTest

echo "=== 7. Testing native lithophane STL output ==="
javac -d scripts/tests/bin \
  -cp "scripts/tests/bin:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/" \
  scripts/tests/LithophaneStlWriterTest.java
java -cp "scripts/tests/bin:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/" \
  scripts.tests.LithophaneStlWriterTest

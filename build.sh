#!/usr/bin/env sh
# Build, test or run the Mortgage Calculator. Needs JDK 17 or newer.
#
#   ./build.sh          compile and package build/mortgage-calculator.jar
#   ./build.sh test     compile and run the unit tests
#   ./build.sh run ...  build, then run with the given arguments
#   ./build.sh clean    remove the build directory
set -eu
cd "$(dirname "$0")"

JAR=build/mortgage-calculator.jar

compile() {
    rm -rf build/classes
    mkdir -p build/classes
    javac -encoding UTF-8 -Xlint:all -Werror -d build/classes $(find src -name '*.java')
    jar --create --file "$JAR" --main-class com.sebastian.mortgage.Main -C build/classes .
}

case "${1:-build}" in
    build)
        compile
        echo "Built $JAR - run it with: java -jar $JAR"
        ;;
    test)
        compile
        rm -rf build/test-classes
        mkdir -p build/test-classes
        javac -encoding UTF-8 -Xlint:all -Werror -cp build/classes -d build/test-classes $(find test -name '*.java')
        java -cp build/classes:build/test-classes com.sebastian.mortgage.MortgageCalculatorTest
        ;;
    run)
        shift
        compile
        java -jar "$JAR" "$@"
        ;;
    clean)
        rm -rf build
        ;;
    *)
        echo "usage: $0 [build|test|run [args...]|clean]" >&2
        exit 2
        ;;
esac

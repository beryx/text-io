#!/bin/bash
set -ev
./gradlew --no-daemon build javadoc asciidoc

if [ "${TRAVIS_PULL_REQUEST}" == "false" -a "${TRAVIS_BRANCH}" == "master" ]; then
  if [ "`git ls-remote origin gh-pages`" == "" ]; then
    ./gradlew --no-daemon publishGhPages --rerun-tasks --info --stacktrace -PghPageType=init
  fi
  ./gradlew --no-daemon publishGhPages --rerun-tasks --info --stacktrace -PghPageType=latest
  ./gradlew --no-daemon publishGhPages --rerun-tasks --info --stacktrace -PghPageType=version
fi

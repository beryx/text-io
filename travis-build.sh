#!/bin/bash
set -ev
./gradlew --no-daemon --info --stacktrace build javadoc asciidoc

if [ "${TRAVIS_PULL_REQUEST}" == "false" -a "${TRAVIS_BRANCH}" == "master" ]; then
  if [ "`git ls-remote origin gh-pages`" == "" ]; then
    ./gradlew --no-daemon --rerun-tasks --info --stacktrace publishGhPages -PghPageType=init
  fi
  ./gradlew --no-daemon --rerun-tasks --info --stacktrace publishGhPages -PghPageType=latest
  ./gradlew --no-daemon --rerun-tasks --info --stacktrace publishGhPages -PghPageType=version
fi

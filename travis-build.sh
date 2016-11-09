#!/bin/bash
set -ev
./gradlew build asciidoc

if [ "${TRAVIS_PULL_REQUEST}" == "false" -a "${TRAVIS_BRANCH}" == "master" ]; then
  if [ "`git ls-remote origin gh-pages`" == "" ]; then
    ./gradlew publishGhPages --info --stacktrace --rerun-tasks -PghPageType=init
  fi
  ./gradlew publishGhPages --info --stacktrace --rerun-tasks -PghPageType=latest
  ./gradlew publishGhPages --info --stacktrace --rerun-tasks -PghPageType=version
fi

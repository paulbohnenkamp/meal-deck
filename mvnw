#!/usr/bin/env sh
set -eu
MAVEN_VERSION=3.9.16
MAVEN_SHA512=831a8591fe20c8243b1dbe7d71e3244f31d1665b0804b2e825e38cbbe5ce0cafb8338851f90780735568773e0a6cd07bbec107cda0b896b008b861075358b6f6
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_HOME="$ROOT_DIR/.mvn/download/apache-maven-$MAVEN_VERSION"
ARCHIVE="$ROOT_DIR/.mvn/download/apache-maven-$MAVEN_VERSION-bin.tar.gz"
URL="https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$ROOT_DIR/.mvn/download"
  if [ ! -f "$ARCHIVE" ]; then
    echo "Downloading Apache Maven $MAVEN_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$URL" -o "$ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "$URL" -O "$ARCHIVE"
    else
      echo "curl or wget is required to bootstrap Maven." >&2
      exit 1
    fi
  fi
  if command -v shasum >/dev/null 2>&1; then
    ACTUAL=$(shasum -a 512 "$ARCHIVE" | awk '{print $1}')
  else
    ACTUAL=$(sha512sum "$ARCHIVE" | awk '{print $1}')
  fi
  [ "$ACTUAL" = "$MAVEN_SHA512" ] || { echo "Maven archive checksum mismatch." >&2; exit 1; }
  tar -xzf "$ARCHIVE" -C "$ROOT_DIR/.mvn/download"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"

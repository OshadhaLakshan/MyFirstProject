#!/usr/bin/env bash
set -euo pipefail
# Java 21 tutorial prerequisites; leave repository source and manifests unchanged.
LAB_JDK_DIR=/workspace/tools/jdk-21.0.8+9
LAB_JDK_ARCHIVE=OpenJDK21U-jdk_x64_linux_hotspot_21.0.8_9.tar.gz
LAB_JDK_SHA256=f2dc5418092c43003db8f9005c4a286e1c0104fea96ccdd49e8ebd037cac9219
mkdir -p /workspace/tools
if [ ! -x "$LAB_JDK_DIR/bin/javac" ]; then
    LAB_JDK_STAGE=$(mktemp -d /workspace/tools/jdk-install.XXXXXX)
    trap 'rm -rf "$LAB_JDK_STAGE"' EXIT
    curl --fail --silent --show-error --location --retry 2 --max-time 180 \
      "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.8%2B9/$LAB_JDK_ARCHIVE" \
      --output "$LAB_JDK_STAGE/$LAB_JDK_ARCHIVE"
    (cd "$LAB_JDK_STAGE"; printf '%s  %s\n' "$LAB_JDK_SHA256" "$LAB_JDK_ARCHIVE" | sha256sum --check -)
    tar -xzf "$LAB_JDK_STAGE/$LAB_JDK_ARCHIVE" -C /workspace/tools
    rm -rf "$LAB_JDK_STAGE"
    trap - EXIT
fi
export JAVA_HOME="$LAB_JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
javac -version
# Compile and execute a real Java program outside any repository.
LAB_JAVA_CHECK=$(mktemp -d /tmp/lab-java-check.XXXXXX)
cat > "$LAB_JAVA_CHECK/SetupCheck.java" <<'JAVA'
public class SetupCheck {
    public static void main(String[] args) {
        if (Runtime.version().feature() != 21) throw new AssertionError("Java 21 required");
        System.out.println("Java 21 compile/run verified");
    }
}
JAVA
javac --release 21 -d "$LAB_JAVA_CHECK" "$LAB_JAVA_CHECK/SetupCheck.java"
java -cp "$LAB_JAVA_CHECK" SetupCheck
rm -rf "$LAB_JAVA_CHECK"

# Maven is required by the uploaded Java lab project.
LAB_MAVEN_DIR=/workspace/tools/apache-maven-3.9.11
LAB_MAVEN_ARCHIVE=apache-maven-3.9.11-bin.tar.gz
LAB_MAVEN_SHA512=bcfe4fe305c962ace56ac7b5fc7a08b87d5abd8b7e89027ab251069faebee516b0ded8961445d6d91ec1985dfe30f8153268843c89aa392733d1a3ec956c9978
if [ ! -x "$LAB_MAVEN_DIR/bin/mvn" ]; then
    LAB_MAVEN_STAGE=$(mktemp -d /workspace/tools/maven-install.XXXXXX)
    trap 'rm -rf "$LAB_MAVEN_STAGE"' EXIT
    curl --fail --silent --show-error --location --retry 2 --max-time 90 \
      "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/$LAB_MAVEN_ARCHIVE" \
      --output "$LAB_MAVEN_STAGE/$LAB_MAVEN_ARCHIVE"
    (cd "$LAB_MAVEN_STAGE"; printf '%s  %s\n' "$LAB_MAVEN_SHA512" "$LAB_MAVEN_ARCHIVE" | sha512sum --check -)
    tar -xzf "$LAB_MAVEN_STAGE/$LAB_MAVEN_ARCHIVE" -C /workspace/tools
    rm -rf "$LAB_MAVEN_STAGE"
    trap - EXIT
fi
# Maven does not automatically use HTTP_PROXY/HTTPS_PROXY. Configure the
# platform egress route outside the checkout, without copying credentials.
if [ ! -f /workspace/tools/maven-settings.xml ]; then
    python3 - <<'PYCONFIG'
from pathlib import Path
import os, urllib.parse, html
proxy = urllib.parse.urlparse(os.environ.get('HTTPS_PROXY') or os.environ.get('HTTP_PROXY') or '')
if proxy.username or proxy.password:
    raise SystemExit('Authenticated proxy requires supported runtime binding; do not copy credentials.')
proxies = ''
if proxy.hostname:
    proxies = f'<proxies><proxy><id>cloud-egress</id><active>true</active><protocol>http</protocol><host>{html.escape(proxy.hostname)}</host><port>{proxy.port or 80}</port><nonProxyHosts>localhost|127.0.0.1</nonProxyHosts></proxy></proxies>'
Path('/workspace/tools/maven-settings.xml').write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"><localRepository>/workspace/tools/m2</localRepository>' + proxies + '</settings>\n')
PYCONFIG
fi
"$LAB_MAVEN_DIR/bin/mvn" -version
# Build the checked-out lab without changing source, manifests or lockfiles.
if [ -f /workspace/MyFirstProject/pom.xml ]; then
    cd /workspace/MyFirstProject
elif [ -f /workspace/lab1-work/submission/pom.xml ]; then
    cd /workspace/lab1-work/submission
fi
if [ -f pom.xml ]; then
    "$LAB_MAVEN_DIR/bin/mvn" -B -ntp -s /workspace/tools/maven-settings.xml \
      -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts compile
fi

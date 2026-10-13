# Start the Java lab in the cloud

Use the existing isolated checkout at `/workspace/MyFirstProject`. Do not create a Git worktree unless explicitly requested. Do not modify application source, tests, manifests or lockfiles as part of environment setup.

## Activate and build

If the tools are missing, run `bash /workspace/MyFirstProject/setup/install-java21.sh`. The installer verifies the Java and Maven archive checksums and creates Maven proxy configuration outside the repository. It does not require new package credentials.

```sh
export JAVA_HOME=/workspace/tools/jdk-21.0.8+9
export PATH="$JAVA_HOME/bin:/workspace/tools/apache-maven-3.9.11/bin:$PATH"
cd /workspace/MyFirstProject
java -version
javac -version
mvn -B -ntp -s /workspace/tools/maven-settings.xml -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts compile
```

Maven uses the supplied cloud proxy through the settings file and the system Java trust store; TLS verification remains enabled. Generated `target/` files are ignored.

## Start and verify

Start one server in a tool session, retaining that session so you can stop the process you started:

```sh
java -cp target/classes chat.ChatServer 5000
```

The default server uses ten workers for Part 5. The Part 4 50-client load test needs a separate server run in thread-per-client mode:

```sh
java -cp target/classes chat.ChatServer 5000 --thread-per-client
```

Check startup in another tool session with a real protocol request:

```python
import socket, re
with socket.create_connection(('localhost', 5000), timeout=5) as s:
    s.settimeout(5)
    s.sendall(b'SetupProbe\n/stats\n/quit\n')
    with s.makefile('r', encoding='utf-8') as stream:
        lines = [line.strip() for line in stream]
    assert any(re.fullmatch(r'stats: [0-9]+ messages', line) for line in lines), lines
    print('Chat join, /stats and /quit verified')
```

## Other checks

Use a separate terminal/tool session for each client. From the repository root:

```sh
java -cp target/classes chat.ChatClient localhost 5000 Alice
java -cp target/classes chat.CounterDemo
java -cp target/classes chat.LoadTester
java -cp target/classes chat.LoadTester --clients 12 --hold
```

For the default load test, require `Server counted: 50,000 (lost: 0)` with no connection/write/timeout warnings; the tester's zero exit code alone does not prove success. The hold test is for the ten-worker pool: ten handlers start, remaining clients wait, and releasing the idle clients lets queued handlers run.

Stop a server you started before switching experiments. Never stop unrelated user processes. If a port is occupied, choose another free port and pass it consistently to all programs. Use local requests for validation; do not create localhost preview links.

Live processes are not retained across cloud environment publication. Restart required services in each new task. Dependency files and compiled outputs may be retained, but changed source needs rebuilding. Completed report, submission ZIP, logs and experiment source variants are linked from README.md and stored under `deliverables/`.

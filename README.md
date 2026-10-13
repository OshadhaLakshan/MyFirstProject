# MyFirstProject
Java Tutorial

## SCS2314 Lab 1: Java socket chat server

Completed lab project using Java 21 and Maven. The source includes a single-threaded echo server, a threaded echo server, and a chat server with a thread-safe client list, atomic message counter, and a ten-worker pool.

### Build and run

From this repository root, using JDK 21 and Maven:

```sh
mvn compile
java -cp target/classes chat.ChatServer 5000
```

Start a client in another terminal:

```sh
java -cp target/classes chat.ChatClient localhost 5000 Alice
```

For the 50-client Part 4 load test, restart the server in thread-per-client mode:

```sh
java -cp target/classes chat.ChatServer 5000 --thread-per-client
```

Then run `java -cp target/classes chat.LoadTester` in another terminal. The default ten-worker server is for the Part 5 pool demonstration. Stop the previous server before starting another on port 5000.

### Assignment files

- [Three-page report](deliverables/SCS2314_Lab1_Report.pdf) and [editable report text](deliverables/SCS2314_Lab1_Report.md).
- [Submission ZIP](deliverables/YOUR_INDEX_NUMBER.zip), containing `src/`, `pom.xml`, and the PDF report; no `target/` folder.
- [Run instructions](deliverables/RUN_INSTRUCTIONS.md).
- [Experiment evidence](deliverables/Lab1_Experiment_Evidence.zip), containing actual logs and the original/intermediate experiment source versions.
- [Artifact checksums](deliverables/artifact-sha256.json).

Add your student index to the report and rename the submission ZIP before submitting. The reported measurements were obtained through automated execution in the cloud; the unsafe and intermediate versions are preserved in the evidence archive. All three fully corrected load tests counted 50,000 messages without exceptions. Maven compilation, the thread-pool behavior, and abrupt-disconnection cleanup were verified.

### Cloud setup

[setup/install-java21.sh](setup/install-java21.sh) reproduces the checksum-verified Java 21 and Maven tool installation in the Codex cloud workspace. It uses the supplied cloud proxy and system Java trust store, and leaves application source unchanged. Toolchains, dependency caches, and generated build output are excluded from Git.

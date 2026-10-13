# SCS2314 Lab 1 - completed project

Add your index number to the report and rename YOUR_INDEX_NUMBER.zip before submitting.
The ZIP contains src/, pom.xml and a three-page report. It excludes target/.

## Build

Install JDK 21 and Maven, extract the submission ZIP, and open a terminal in the folder containing pom.xml.
Run `mvn compile`. Alternatively, import that folder into Eclipse using File -> Import -> Maven -> Existing Maven Projects.

## Run

Use a separate terminal for each process. Stop the previous server before starting another on port 5000.

- Part 1 server: `java -cp target/classes echo.EchoServer 5000`
- Part 2 server: `java -cp target/classes echo.ThreadedEchoServer 5000`
- Echo client: `java -cp target/classes echo.EchoClient localhost 5000`
- Chat server, ten-worker pool (Part 5): `java -cp target/classes chat.ChatServer 5000`
- Chat server, one thread per client (Part 4): `java -cp target/classes chat.ChatServer 5000 --thread-per-client`
- Chat clients: `java -cp target/classes chat.ChatClient localhost 5000 Alice` (use Bob and Carol in other terminals)
- Corrected counter: `java -cp target/classes chat.CounterDemo`
- Part 4 load test: `java -cp target/classes chat.LoadTester`
- Part 5 hold test: `java -cp target/classes chat.LoadTester --clients 12 --hold`

The 50-client Part 4 load test should use the thread-per-client mode. The ten-worker pool queues clients and changes the experiment.

## Evidence

Lab1_Experiment_Evidence.zip is a separate optional review archive, not the required submission.
It includes actual server/test logs and the original, unsafe, list-fixed and fully fixed experiment sources.
The measurements were generated in the cloud with automated TCP peers and the supplied Java programs.
The report's Markdown version is supplied separately for editing; if you edit the report, regenerate/update its PDF in the submission ZIP.

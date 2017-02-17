# memcached-server
Simple Java based memcached server based on Netty and Guava cache




Simple MemcacheD Server prototype and LoadTest that support only GET and SET commands for memcache text protocol

Content
    server - Netty + Guava based memcached server
    client - simple load test, based on spymemcached library. It runs multiple get and set commands in parallel against
        memcached server(s) using multiple threads. It verifies the results and performs basic performance measurements

Prerequisites
    1) java8
    2) maven

Build / Installation
    From project root folder (memcached-server) run the following maven command:
        $ mvn install

Running Basic Tests in Single Server Mode - 2 terminal windows needed
    1) terminal1:: start server: from server folder execute:
        $ mvn exec:exec
            server start task has GC output enabled - so it is useful to see GC activity during load tests
    2) terminal2:: run tests: open other terminal window and from client folder execute:
        $ mvn exec:exec
            tests will run approximately ~1minute
    3) to stop server after you are done with the tests press ^C in terminal1

Running Basic Tests in Distributed Mode (2 servers) - 3 terminal windows needed
    1) terminal1:: start server1: from server folder execute:
        $ mvn exec:exec -Dport="11211"
            it will start first server on port 11211
    2) terminal2:: start server2: from server folder execute:
        $ mvn exec:exec -Dport="11212"
            it will start second server on port 11212
    3) terminal3:: run tests: from client folder execute:
        $ mvn exec:exec -Dservers="localhost:11211,localhost:11212"
            it will start tests distributing load between 2 servers

Running Distributed Tests in Distributed Server Mode (2 servers, 2 test clients) - 4 terminal windows needed
    1) terminal1:: start server1: from server folder execute:
        $ mvn exec:exec -Dport="11211"
            it will start first server on port 11211
    2) terminal2:: start server2: from server folder execute:
        $ mvn exec:exec -Dport="11212"
            it will start second server on port 11212
    3) terminal3:: run tests on a first client: from client folder execute:
        $ mvn exec:exec -Dservers="localhost:11211,localhost:11212"
            it will start tests distributing load between 2 servers
    4) terminal4:: run tests on a second client: Make sure you started tests at least 0.3sec after first client started
       to avoid key collisions. From client folder execute:
        $ mvn exec:exec -Dservers="localhost:11211,localhost:11212"
            it will start tests distributing load between 2 servers

Changing Test Parameters
    Most of test parameters (e.g. number of tasks, threads etc.) are defined in constants
    inside MemcacheClientLoadTest.java
    If you change them you need to rebuild client: $ mvn install

Changing Server Log Level
    To enable server debug logs set <Root level="debug"> inside server/src/main/resources/log4j2.xml
    You will need to rebuild the server then: $ mvn install
    NOTE: if you change log level to "debug" it will affect server performance as debug mode is pretty verbose
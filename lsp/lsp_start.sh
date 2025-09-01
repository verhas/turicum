#!/usr/bin/env bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar /Users/verhasp/github/turicum/lsp/target/turicum-lsp-server-*-shaded.jar

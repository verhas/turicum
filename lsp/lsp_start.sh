#!/usr/bin/env bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar /Users/verhasp/github/turicum/lsp/target/turicum-lsp-server-*-shaded.jar
#java -jar /Users/verhasp/github/turicum/lsp/target/turicum-lsp-server-*-shaded.jar 2>> /Users/verhasp/github/turicum/lsp/lsp_err.txt

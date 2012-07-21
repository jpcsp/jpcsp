#!/bin/sh
java -Xmx1024m -Xss1m -XX:MaxPermSize=128m -XX:ReservedCodeCacheSize=64m -Djava.library.path=lib/linux-amd64 -jar bin/jpcsp.jar $@

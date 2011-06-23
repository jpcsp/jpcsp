#!/bin/sh
java -Xmx1024m -XX:MaxPermSize=128m -XX:ReservedCodeCacheSize=64m -Djava.library.path=lib/linux-x86 -jar bin/Jpcsp.jar

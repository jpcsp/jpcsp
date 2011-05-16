#!/bin/sh\n
java -Xmx1024m -cp "bin/*;lib/*;lib/amd-64/*" -Djava.library.path=lib/amd-64 jpcsp.MainGUI
#!/bin/sh\n
java -Xmx1024m -cp "bin/*;lib/*;lib/linux-x86/*" -Djava.library.path=lib/linux-x86 jpcsp.MainGUI
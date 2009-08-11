#!/bin/gawk -f
# prints the instructions from a prxtool disassembly
# usage: cat HelloJpcsp\ asm.txt | ./getins.awk | sort | uniq -c | sort

BEGIN {
	# make regex ignore case
	IGNORECASE=1
}

/:/ {
	if ($5 != "-" &&
		$5 != "" &&
		$5 != "Address" &&
		$5 !~ /0x/)
		print $5
}

END {
}

## The CoProcessors ##

The MIPS archicteture defines three coprocessors (CP0, CP1 and CP2).

**Coprocessor 0** - is incorporated on CPU chip, manager of virtual memory system and handler of exceptions.
> Sometimes is AKA SCC (system control coprocessor).

**Coprocessor 1** - is reserved for the on-chip, the floating-point unit (FPU).

**Coprocessor 2** - Is reserved for future uses.

### CP0 - System Control ###

Main functions (or feature)

Translate the virtual address to physical (MMU function), manages exceptions, controls cache system, error control,
handle the change modes: **kernel, supervisor** and **user** mode.

#### Registers (CP0R0 - CP0R31) ####

|0 = [Index](Index.md) Programmable pointer into TLB array|
|:--------------------------------------------------------|
|1 = [Random](Random.md) Pseudorandom pointer into TLB array (read only)|
|2 = [EntryLo0](EntryLo0.md) Low half of TLB entry for even virtual address (VPN)|
|3 = [EntryLo1](EntryLo1.md) Low half of TLB entry for odd virtual address (VPN)|
|4 = [Context](Context.md) Pointer to kernel virtual page table entry (PTE) in 32-bit addressing mode|
|5 = [PageMask](PageMask.md) TLB Page Mask|
|6 = [Wired](Wired.md) Number of wired TLB entries|
|7 = [Reserved](Reserved.md)|
|8 = [BadVAddr](BadVAddr.md) Bad virtual address|
|9 = [Count](Count.md) Timer Count|
|10= [EntryHi](EntryHi.md) High half of TLB entry|
|11= [Compare](Compare.md) Timer Compare|
|12= [SR](SR.md) Status register|
|13= [Cause](Cause.md) Cause of last exception|
|14= [EPC](EPC.md) Exception Program Counter|
|15= [PRId](PRId.md) Processor Revision Identifier|
|16= [Config](Config.md) Configuration register|
|17= [LLAddr](LLAddr.md) Load Linked Address|
|18= [WatchLo](WatchLo.md) Memory reference trap address low bits|
|19= [WatchHi](WatchHi.md) Memory reference trap address high bits|
|20= [XContext](XContext.md) Pointer to kernel virtual PTE table in 64-bit addressing mode|
|21–25= [Reserved](Reserved.md)|
|26= [ECC](ECC.md) Secondary-cache error checking and correcting (ECC) and Primary parity|
|27= [CacheErr](CacheErr.md) Cache Error and Status register|
|28= [TagLo](TagLo.md) Cache Tag register|
|29= [TagHi](TagHi.md) Cache Tag register|
|30= [ErrorEPC](ErrorEPC.md) Error Exception Program Counter|
|31= [Reserved](Reserved.md)|

#### The CP0 Instructions ####
  * DMFC0 - Doubleword Move From CP0
  * DMTC0 - Doubleword Move To CP0
  * MTC0 - Move to CP0
  * MFC0 - Move from CP0
  * TLBR - Read Indexed TLB Entry
  * TLBWI - Write Indexed TLB Entry
  * TLBWR - Write Random TLB Entry
  * TLBP - Probe TLB for Matching Entry
  * CACHE - Cache Operation
  * ERET - Exception Return



### CP1 - FPU ###

Main functions (or feature)

Floating-point operations, follow the standard ANSI/IEEE 754-1985, uses the main cpu instructions to performs
some operations.
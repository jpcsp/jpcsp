/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.nec78k0.sfr;

import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.NUMBER_SPECIAL_FUNCTION_REGISTERS;
import static jpcsp.nec78k0.Nec78k0Processor.PSW_ADDRESS;
import static jpcsp.nec78k0.Nec78k0Processor.SFR_ADDRESS;
import static jpcsp.nec78k0.Nec78k0Processor.SP_ADDRESS;
import static jpcsp.util.Utilities.hasFlag;

/**
 * @author gid15
 *
 */
public class Nec78k0SfrNames {
	private static final String[][] sfr1Names = new String[NUMBER_SPECIAL_FUNCTION_REGISTERS][8];
	private static final String[] sfr8Names = new String[NUMBER_SPECIAL_FUNCTION_REGISTERS];
	private static final String[] sfr16Names = new String[NUMBER_SPECIAL_FUNCTION_REGISTERS];
	private static final boolean[] hasSfr1Names = new boolean[NUMBER_SPECIAL_FUNCTION_REGISTERS];

	static {
		init();
	}

	private static void init() {
		// SFR names when accessing as 1, 8 or 16 bits
		addSfrName(0xFF00, "P0");
		addSfrName(0xFF01, "P1");
		addSfrName(0xFF02, "P2");
		addSfrName(0xFF03, "P3");
		addSfrName(0xFF04, "P4");
		addSfrName(0xFF05, "P5");
		addSfrName(0xFF06, "P6");
		addSfrName(0xFF07, "P7");
		addSfrName(0xFF08, "ADCR", null, "ADCRH");
		addSfrName(0xFF0A, "RXB6");
		addSfrName(0xFF0B, "TXB6");
		addSfrName(0xFF0C, "P12");
		addSfrName(0xFF0D, "P13");
		addSfrName(0xFF0E, "P14");
		addSfrName(0xFF0F, "SIO10");
		addSfrName(0xFF10, "TM00", null, null);
		addSfrName(0xFF12, "CR000", null, null);
		addSfrName(0xFF14, "CR010", null, null);
		addSfrName(0xFF16, "TM50");
		addSfrName(0xFF17, "CR50");
		addSfrName(0xFF18, "CMP00");
		addSfrName(0xFF19, "CMP10");
		addSfrName(0xFF1A, "CMP01");
		addSfrName(0xFF1B, "CMP11");
		addSfrName(SP_ADDRESS, "SP", null, null);
		addSfrName(PSW_ADDRESS, "PSW", new String[] { "CY", "ISP", null, "RBS0", "AC", "RBS1", "Z", "IE" });
		addSfrName(0xFF1F, "TM51");
		addSfrName(0xFF20, "PM0");
		addSfrName(0xFF21, "PM1");
		addSfrName(0xFF22, "PM2");
		addSfrName(0xFF23, "PM3");
		addSfrName(0xFF24, "PM4");
		addSfrName(0xFF25, "PM5");
		addSfrName(0xFF26, "PM6");
		addSfrName(0xFF27, "PM7");
		addSfrName(0xFF28, "ADM", new String[] { "ADCE", "LV0", "LV1", "FR0", "FR1", "FR2", null, "ADCS" });
		addSfrName(0xFF29, "ADS", new String[] { "ADS0", "ADS1", "ADS2" });
		addSfrName(0xFF2C, "PM12");
		addSfrName(0xFF2E, "PM14");
		addSfrName(0xFF2F, "ADPC", new String[] { "ADPC0", "ADPC1", "ADPC2", "ADPC3" });
		addSfrName(0xFF30, "PU0");
		addSfrName(0xFF31, "PU1");
		addSfrName(0xFF32, "PU2");
		addSfrName(0xFF33, "PU3");
		addSfrName(0xFF34, "PU4");
		addSfrName(0xFF35, "PU5");
		addSfrName(0xFF36, "PU6");
		addSfrName(0xFF37, "PU7");
		addSfrName(0xFF3C, "PU12");
		addSfrName(0xFF3E, "PU14");
		addSfrName(0xFF40, "CKS", new String[] { "CCS0", "CCS1", "CCS2", "CCS3", "CLOE" });
		addSfrName(0xFF41, "CR51");
		addSfrName(0xFF43, "TMC51", new String[] { "TOE51", "TMC51", "LVR51", "LVS51", null, null, "TMC516", "TCE51" });
		addSfrName(0xFF48, "EGP");
		addSfrName(0xFF49, "EGN");
		addSfrName(0xFF4A, "SIO11");
		addSfrName(0xFF4C, "SOTB11");
		addSfrName(0xFF4F, "ISC", new String[] { "ISC0", "ISC1" });
		addSfrName(0xFF50, "ASIM6", new String[] { "ISRM6", "SL6", "CL6", "PS60","PS61", "RXE6", "TXE6", "POWER6" });
		addSfrName(0xFF53, "ASIS6");
		addSfrName(0xFF55, "ASIF6");
		addSfrName(0xFF56, "CKSR6");
		addSfrName(0xFF57, "BRGC6");
		addSfrName(0xFF58, "ASICL6", new String[] { "TXDLV6", "DIR6", "SBL60", "SBL61", "SBL62", "SBTT6", "SBRT6", "SBRF6" }); 
		addSfrName(0xFF60, "SDR0", "SDR0L", "SDR0H");
		addSfrName(0xFF62, "MDA0L", "MDA0LL", "MDA0LH");
		addSfrName(0xFF64, "MDA0H", "MDA0HL", "MDA0HH");
		addSfrName(0xFF66, "MDB0", "MDB0L", "MDB0H");
		addSfrName(0xFF68, "DMUC0", new String[] { "DMUSEL0", null, null, null, null, null, null, "DMUE" });
		addSfrName(0xFF69, "DMUC0", new String[] { "TOEN0", "TOLEV0", "TMMD00", "TMMD01", "CKS00", "CKS01", "CKS02", "TMHE0" });
		addSfrName(0xFF6A, "TCL50", new String[] { "TCL500", "TCL501", "TCL502" });
		addSfrName(0xFF6B, "TMC50", new String[] { "TOE50", "TMC501", "LVR50", "LVS50", null, null, "TMC506", "TCE50" });
		addSfrName(0xFF6C, "TMHMD1", new String[] { "TOEN1", "TOLEV1", "TMMD10", "TMMD11", "CKS10", "CKS11", "CKS12", "TMHE1" });
		addSfrName(0xFF6D, "TMCYC1", new String[] { "NRZ1", "NRZB1", "RMC1" });
		addSfrName(0xFF6E, "KRM");
		addSfrName(0xFF6F, "WTM");
		addSfrName(0xFF70, "ASIM0", new String[] { null, "SL0", "CL0", "PS00", "PS01", "RXE0", "TXE0", "POWER0" });
		addSfrName(0xFF71, "BRGC0");
		addSfrName(0xFF72, "RXB0");
		addSfrName(0xFF73, "ASIS0");
		addSfrName(0xFF74, "TXS0");
		addSfrName(0xFF80, "CSIM10", new String[] { "CSOT10", null, null, null, "DIR10", null, "TRMD10", "CSIE10" });
		addSfrName(0xFF81, "CSIC10", new String[] { "CKS100", "CKS101", "CKS102", "DAP10", "CKP10" });
		addSfrName(0xFF84, "SOTB10");
		addSfrName(0xFF88, "CSIM11", new String[] { "CSOT11", null, null, null, "DIR11", "SSE11", "TRMD11", "CSIE11" });
		addSfrName(0xFF89, "CSIC11", new String[] { "CKS110", "CKS111", "CKS112", "DAP11", "CKP11" });
		addSfrName(0xFF8C, "TCL51", new String[] { "TCL510", "TCL511", "TCL512" });
		addSfrName(0xFF90, "CSIMA0", new String[] { null, "DIR0", "RXEA0", "TXEA0", "MASTER0", "ATM0", "ATE0", "CSIAE0" });
		addSfrName(0xFF91, "CSIS0", new String[] { "TSF0", "ERRF0", "ERRE0", "BUSYLV0", "BUSYE0", "STBE0", "CKS00" });
		addSfrName(0xFF92, "CSIT0", new String[] { "ATSTA0", "ATSTP0" });
		addSfrName(0xFF93, "BRGCA0");
		addSfrName(0xFF94, "ADTP0");
		addSfrName(0xFF95, "ADTI0");
		addSfrName(0xFF96, "SIOA0");
		addSfrName(0xFF97, "ADTC0");
		addSfrName(0xFF99, "WDTE");
		addSfrName(0xFF9F, "OSCCTL", new String[] { "AMPH", null, null, null, "OSCSELS", "EXCLKS", "OSCSEL", "EXCLK" });
		addSfrName(0xFFA0, "RCM", new String[] { "RSTOP", "LSRSTOP", null, null, null, null, null, "RSTS" });
		addSfrName(0xFFA1, "MCM", new String[] { "MCM0", "MCS", "XSEL" });
		addSfrName(0xFFA2, "MOC", new String[] { null, null, null, null, null, null, null, "MSTOP" });
		addSfrName(0xFFA3, "OSTC", new String[] { "MOST16", "MOST15", "MOST14", "MOST13", "MOST11" });
		addSfrName(0xFFA4, "OSTS");
		addSfrName(0xFFA5, "IIC0");
		addSfrName(0xFFA6, "IICC0", new String[] { "SPT0", "STT0", "ACKE0", "WTIM0", "SPIE0", "WREL0", "LREL0", "IICE0" });
		addSfrName(0xFFA7, "SVA0");
		addSfrName(0xFFA8, "IICCL0", new String[] { "CL00", "CL01", "DFC0", "SMC0", "DAD0", "CLD0" });
		addSfrName(0xFFA9, "IICX0", new String[] { "CLX0" });
		addSfrName(0xFFAA, "IICS0", new String[] { "SPD0", "STD0", "ACKD0", "TRC0", "COI0", "EXC0", "ALD0", "MSTS0" });
		addSfrName(0xFFAB, "IICF0", new String[] { "IICRSV", "STCEN", null, null, null, null, "IICBSY", "STCF" });
		addSfrName(0xFFAC, "RESF");
		addSfrName(0xFFB0, "TM01", null, null);
		addSfrName(0xFFB2, "CR001", null, null);
		addSfrName(0xFFB4, "CR011", null, null);
		addSfrName(0xFFB6, "TMC01", new String[] { "OVF01", "TMC011", "TMC012", "TMC013" });
		addSfrName(0xFFB7, "PRM01", new String[] { "PRM010", "PRM011", null, null, "ES010", "ES011", "ES110", "ES111" });
		addSfrName(0xFFB8, "CRC01", new String[] { "CRC010", "CRC011", "CRC012" });
		addSfrName(0xFFB9, "TOC01", new String[] { "TOE01", "TOC011", "LVR01", "LVS01", "TOC014", "OSPE01", "OSPT01" });
		addSfrName(0xFFBA, "TMC00", new String[] { "OVF00", "TMC001", "TMC002", "TMC003" });
		addSfrName(0xFFBB, "PRM00", new String[] { "PRM000", "PRM001", null, null, "ES000", "ES001", "ES100", "ES101" });
		addSfrName(0xFFBC, "CRC00", new String[] { "CRC000", "CRC001", "CRC002" });
		addSfrName(0xFFBD, "TOC00", new String[] { "TOE00", "TOC001", "LVR00", "LVS00", "TOC004", "OSPE00", "OSPT00" });
		addSfrName(0xFFBE, "LVIM", new String[] { "LVIF", "LVIMD", null, null, "LVISEL", null, null, "LVION" });
		addSfrName(0xFFBF, "LVIS", new String[] { "LVIS0", "LVIS1", "LVIS2", "LVIS3" });
		addSfrName(0xFFC0, "PFCMD");
		addSfrName(0xFFCA, "FLPMC", new String[] { "FLSPM0", "FLSPM1", "FWEPR", "FWEDIS" });
		addSfrName(0xFFE0, "IF0", "IF0L", "IF0H", new String[] { "LVIIF", "PIF0", "PIF1", "PIF2", "PIF3", "PIF4", "PIF5", "SREIF6", "SRIF6", "STIF6", "CSIIF10", "TMIFH1", "TMIFH0", "TMIF50", "TMIF000", "TMIF010" });
		addSfrName(0xFFE2, "IF1", "IF1L", "IF1H", new String[] { "ADIF", "SRIF0", "WTIIF", "TMIF51", "KRIF", "WTIF", "PIF6", "PIF7", "IICIF0", "CSIIF11", "TMIF001", "TMIF011", "ACSIIF" });
		addSfrName(0xFFE4, "MK0", "MK0L", "MK0H", new String[] { "LVIMK", "PMK0", "PMK1", "PMK2", "PMK3", "PMK4", "PMK5", "SREMK6", "SRMK6", "STMK6", "CSIMK10", "TMMKH1", "TMMKH0", "TMMK50", "TMMK000", "TMMK010" });
		addSfrName(0xFFE6, "MK1", "MK1L", "MK1H", new String[] { "ADMK", "SRMK0", "WTIMK", "TMMK51", "KRMK", "WTMK", "PMK6", "PMK7", "IICMK0", "CSIMK11", "TMMK001", "TMMK011", "ACSIMK" });
		addSfrName(0xFFE8, "PR0", "PR0L", "PR0H", new String[] { "LVIPR", "PPR0", "PPR1", "PPR2", "PPR3", "PPR4", "PPR5", "SREPR6", "SRPR6", "STPR6", "CSIPR10", "TMPRH1", "TMPRH0", "TMPR50", "TMPR000", "TMPR010" });
		addSfrName(0xFFEA, "PR1", "PR1L", "PR1H", new String[] { "ADPR", "SRPR0", "WTIPR", "TMPR51", "KRPR", "WTPR", "PPR6", "PPR7", "IICPR0", "CSIPR11", "TMPR001", "TMPR011", "ACSIPR" });
		addSfrName(0xFFF0, "IMS");
		addSfrName(0xFFF3, "BANK");
		addSfrName(0xFFF4, "IXS");
		addSfrName(0xFFFB, "PCC", new String[] { "PCC0", "PCC1", "PCC2", null, "CSS", "CLS", "XSTART" });

		// Default names
		final String[] defaultBitNames = { "0", "1", "2", "3", "4", "5", "6", "7" };
		for (int i = 0; i < sfr8Names.length; i++) {
			if (sfr8Names[i] == null) {
				sfr8Names[i] = String.format("0x%04X", SFR_ADDRESS + i);
			}
			if (sfr16Names[i] == null) {
				sfr16Names[i] = sfr8Names[i];
			}
			for (int j = 0; j < 8; j++) {
				if (sfr1Names[i][j] == null) {
					sfr1Names[i][j] = defaultBitNames[j];
				}
			}
		}
	}

	private static void addSfrName(int addr, String name) {
		final int n = addr - SFR_ADDRESS;
		sfr8Names[n] = name;
	}

	private static void addSfrName(int addr, String name, String[] bitNames) {
		final int n = addr - SFR_ADDRESS;
		sfr8Names[n] = name;
		System.arraycopy(bitNames, 0, sfr1Names[n], 0, bitNames.length);
		hasSfr1Names[n] = true;
	}

	private static void addSfrName(int addr, String sfr16Name, String sfr8Name0, String sfr8Name1) {
		final int n = addr - SFR_ADDRESS;
		sfr16Names[n] = sfr16Name;
		sfr8Names[n] = sfr8Name0;
		sfr8Names[n + 1] = sfr8Name1;
	}

	private static void addSfrName(int addr, String sfr16Name, String sfr8Name0, String sfr8Name1, String[] bitNames) {
		final int n = addr - SFR_ADDRESS;
		sfr16Names[n] = sfr16Name;
		sfr8Names[n] = sfr8Name0;
		sfr8Names[n + 1] = sfr8Name1;
		System.arraycopy(bitNames, 0, sfr1Names[n], 0, 8);
		hasSfr1Names[n] = true;
		System.arraycopy(bitNames, 8, sfr1Names[n + 1], 0, bitNames.length - 8);
		hasSfr1Names[n + 1] = true;
	}

	public static String getSfr8Name(int addr) {
		return sfr8Names[addr - SFR_ADDRESS];
	}

	public static String getSfr16Name(int addr) {
		return sfr16Names[addr - SFR_ADDRESS];
	}

	public static boolean hasSfr1Name(int addr) {
		return hasSfr1Names[addr - SFR_ADDRESS];
	}

	public static String getSfr1Name(int addr, int bit) {
		return sfr1Names[addr - SFR_ADDRESS][bit];
	}

	public static String getSfr1Names(int addr, int value) {
		StringBuilder s = new StringBuilder();

		value &= 0xFF;
		for (int i = 0; value != 0; i++, value >>= 1) {
			if (hasFlag(value, 0x01)) {
				if (s.length() > 0) {
					s.append("|");
				}
				s.append(getSfr1Name(addr, i));
			}
		}

		// If returning some names, including between parenthesis
		if (s.length() > 0) {
			s.insert(0, '(');
			s.append(')');
		}

		return s.toString();
	}
}

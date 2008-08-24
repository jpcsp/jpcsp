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
package jpcsp.graphics;

import static jpcsp.graphics.GeCommands.*;

/**
 *
 * @author hli
 */
public class GeDecoder {

    private int intArgument(int word) {
        return (word & 0x00FFFFFF);
    }
    
    private float floatArgument(int word) {
        return Float.intBitsToFloat(word << 8);
    }
       
    public <P extends GeCommands> void process(P that, int insn) {

        int command = (insn >>> 24);

        switch (command) {
            case NOP: break;
            case VADDR: break;
            case IADDR: break;
            case PRIM: break;
            case BEZIER: break;
            case SPLINE: break;
            case BBOX: break;
            case JUMP: break;
            case BJUMP: break;
            case CALL: break;
            case RET: break;
            case END: break;
            case SIGNAL: break;
            case FINISH: break;
            case BASE: break;
            case VTYPE: break;
            case OFFSET_ADDR: break;
            case ORIGIN_ADDR: break;
            case REGION1: break;
            case REGION2: break;
            case LTE: break;
            case LTE0: break;
            case LTE1: break;
            case LTE2: break;
            case LTE3: break;
            case CPE: break;
            case BCE: break;
            case TME: break;
            case FGE: break;
            case DTE: break;
            case ABE: break;
            case ATE: break;
            case ZTE: break;
            case STE: break;
            case AAE: break;
            case PCE: break;
            case CTE: break;
            case LOE: break;
            case BOFS: break;
            case BONE: break;
            case MW0: break;
            case MW1: break;
            case MW2: break;
            case MW3: break;
            case MW4: break;
            case MW5: break;
            case MW6: break;
            case MW7: break;
            case PSUB: break;
            case PPRIM: break;
            case PFACE: break;
            case WMS: break;
            case WORLD: break;
            case VMS: break;
            case VIEW: break;
            case PMS: break;
            case PROJ: break;
            case TMS: break;
            case TMATRIX: break;
            case XSCALE: break;
            case YSCALE: break;
            case ZSCALE: break;
            case XPOS: break;
            case YPOS: break;
            case ZPOS: break;
            case USCALE: break;
            case VSCALE: break;
            case UOFFSET: break;
            case VOFFSET: break;
            case OFFSETX: break;
            case OFFSETY: break;
            case SHADE: break;
            case RNORM: break;
            case CMAT: break;
            case EMC: break;
            case AMC: break;
            case DMC: break;
            case SMC: break;
            case AMA: break;
            case SPOW: break;
            case ALC: break;
            case ALA: break;
            case LMODE: break;
            case LT0: break;
            case LT1: break;
            case LT2: break;
            case LT3: break;
            case LXP0: break;
            case LYP0: break;
            case LZP0: break;
            case LXP1: break;
            case LYP1: break;
            case LZP1: break;
            case LXP2: break;
            case LYP2: break;
            case LZP2: break;
            case LXP3: break;
            case LYP3: break;
            case LZP3: break;
            case LXD0: break;
            case LYD0: break;
            case LZD0: break;
            case LXD1: break;
            case LYD1: break;
            case LZD1: break;
            case LXD2: break;
            case LYD2: break;
            case LZD2: break;
            case LXD3: break;
            case LYD3: break;
            case LZD3: break;
            case LCA0: break;
            case LLA0: break;
            case LQA0: break;
            case LCA1: break;
            case LLA1: break;
            case LQA1: break;
            case LCA2: break;
            case LLA2: break;
            case LQA2: break;
            case LCA3: break;
            case LLA3: break;
            case LQA3: break;
            case SLE0: break;
            case SLE1: break;
            case SLE2: break;
            case SLE3: break;
            case SLF0: break;
            case SLF1: break;
            case SLF2: break;
            case SLF3: break;
            case ALC0: break;
            case DLC0: break;
            case SLC0: break;
            case ALC1: break;
            case DLC1: break;
            case SLC1: break;
            case ALC2: break;
            case DLC2: break;
            case SLC2: break;
            case ALC3: break;
            case DLC3: break;
            case SLC3: break;
            case FFACE: break;
            case FBP: break;
            case FBW: break;
            case ZBP: break;
            case ZBW: break;
            case TBP0: break;
            case TBP1: break;
            case TBP2: break;
            case TBP3: break;
            case TBP4: break;
            case TBP5: break;
            case TBP6: break;
            case TBP7: break;
            case TBW0: break;
            case TBW1: break;
            case TBW2: break;
            case TBW3: break;
            case TBW4: break;
            case TBW5: break;
            case TBW6: break;
            case TBW7: break;
            case CBP: break;
            case CBPH: break;
            case TRXSBP: break;
            case TRXSBW: break;
            case TRXDBP: break;
            case TRXDBW: break;
            case TSIZE0: break;
            case TSIZE1: break;
            case TSIZE2: break;
            case TSIZE3: break;
            case TSIZE4: break;
            case TSIZE5: break;
            case TSIZE6: break;
            case TSIZE7: break;
            case TMAP: break;
            case TEXTURE_ENV_MAP_MATRIX: break;
            case TMODE: break;
            case TPSM: break;
            case CLOAD: break;
            case CMODE: break;
            case TFLT: break;
            case TWRAP: break;
            case TBIAS: break;
            case TFUNC: break;
            case TEC: break;
            case TFLUSH: break;
            case TSYNC: break;
            case FFAR: break;
            case FDIST: break;
            case FCOL: break;
            case TSLOPE: break;
            case PSM: break;
            case CLEAR: break;
            case SCISSOR1: break;
            case SCISSOR2: break;
            case NEARZ: break;
            case FARZ: break;
            case CTST: break;
            case CREF: break;
            case CMSK: break;
            case ATST: break;
            case STST: break;
            case SOP: break;
            case ZTST: break;
            case ALPHA: break;
            case SFIX: break;
            case DFIX: break;
            case DTH0: break;
            case DTH1: break;
            case DTH2: break;
            case DTH3: break;
            case LOP: break;
            case ZMSK: break;
            case PMSKC: break;
            case PMSKA: break;
            case TRXKICK: break;
            case TRXPOS: break;
            case TRXDPOS: break;
            case TRXSIZE: break;
        }    
    }
}

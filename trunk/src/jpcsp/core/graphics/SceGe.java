/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/pspge_8h.html


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
package jpcsp.core.graphics;

public class SceGe {
    
    public int sceGeListEnQueue(int list, int stall, int cbid,int arg){
        /*
         * a(0), a(1), a(2), a(3);
         * */
        
        VideoEngine ve = VideoEngine.getEngine(null,true, true); //maybe we pass the gl object from psp_display
        DisplayList displayList = new DisplayList(list, stall, cbid);
        ve.executeList(displayList);
        return 0; //if all rigth???
    }

    /*
1	0x71fcd1d6	 	sceGeInit	 
2	0x5baa5439	 		 
3	0x9f2c2948	 	sceGeEnd	 
4	0x8f185df7	 	sceGeEdramInit	 
5	0x1f6752ad	unsigned int	sceGeEdramGetSize	void
6	0xe47e40e4	void *	sceGeEdramGetAddr	void
7	0xb77905ea	 	sceGeEdramSetAddrTranslation	 
8	0xc576e897	 		 
9	0xb415364d	 	sceGeGetReg	 
10	0x51c8bb60	 		 
11	0xdc93cfef	unsigned int	sceGeGetCmd	int cmd
12	0x57c8945b	int	sceGeGetMtx	int type,void *matrix
13	0x438a385a	 	sceGeSaveContext	 
14	0x51d44c58	 		 
15	0x0bf608fb	int	sceGeRestoreContext	const PspGeContext *context
16	0xab49e76a	int	sceGeListEnQueue	const void *list,void *stall,int cbid,void *arg
17	0x5a0103e6	 		 
18	0x1c0d95a6	int	sceGeListEnQueueHead	const void *list,void *stall,int cbid,void *arg
19	0x5fb86ab0	int	sceGeListDeQueue	int qid
20	0xe0d68148	int	sceGeListUpdateStallAddr	int qid,void *stall
21	0x03444eb4	int	sceGeListSync	int qid,int syncType
22	0xb287bd61	int	sceGeDrawSync	int syncType
23	0xb448ec0d	 	sceGeBreak	 
24	0x4c06e472	 	sceGeContinue	 
25	0x67b01d8e	 		 
26	0xa4fc06a4	int	sceGeSetCallback	PspGeCallbackData *cb
27	0x05db22ce	int	sceGeUnsetCallback	int cbid
28	0x9acff59d	 		 
29	0x3efc0d64	 		 
30	0x9da4a75f	 		 
31	0xe66cb92e	 		 
32	0x114e1745	 		 
33	0xaec21518	 		 
34	0x7b481502	 		 
35	0xbad6e1ca
*/
}

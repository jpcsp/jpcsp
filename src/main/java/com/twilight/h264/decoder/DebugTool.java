package com.twilight.h264.decoder;

public class DebugTool {

	public static final boolean DEBUG_MODE = false;
	public static int logCount = 0;

	public static void dumpDebugFrameData(H264Context h, String msg) {
		if(!DEBUG_MODE) return;
		dumpDebugFrameData(h, msg, true);
	}

	public static void dumpDebugFrameData(H264Context h, String msg, boolean incrementCounter) {
		if(!DEBUG_MODE) return;

		try {
			
			if(incrementCounter)
				logCount++;
						
			System.out.println("Dumping Decoder State("+msg+"): "+logCount+", Frame: "+h.frame_num);			

			// Dump all data inside decoder
			System.out.print("ctx.non_zero_count_cache: ");
			for(int i=0;i<h.non_zero_count_cache.length;i++) {
				System.out.print(","+h.non_zero_count_cache[i]);
			} // for
			System.out.println();

			//for(int j=0;j<h.non_zero_count.length;j++) {
			int j = h.mb_xy;
			if(h.non_zero_count != null)
			if(j>=0 && j<h.non_zero_count.length) {
				System.out.print("ctx.non_zero_count["+j+"]: ");
				for(int i=0;i<h.non_zero_count[j].length;i++) {
					System.out.print(","+h.non_zero_count[j][i]);
				} // for i
				System.out.println();
			} // for j

			// Dump all data inside decoder
			System.out.print("edge_emu_buffer: ");
			for(int i=0;i<(h.s.width+64)*2*21 && logCount == 9537;i++) {
				System.out.print(","+h.s.allocated_edge_emu_buffer[h.s.edge_emu_buffer_offset + i]);
			} // for
			System.out.println();
			
			System.out.print("ctx.mv_cache[0]: ");
			for(int i=0;i<40;i++) {
				System.out.print(","+h.mv_cache[0][i][0]+","+h.mv_cache[0][i][1]);
			} // for
			System.out.println();

			System.out.print("ctx.mv_cache[1]: ");
			for(int i=0;i<40;i++) {
				System.out.print(","+h.mv_cache[1][i][0]+","+h.mv_cache[1][i][1]);
			} // for
			System.out.println();

			System.out.print("ctx.mvd_cache[0]: ");
			for(int i=0;i<40;i++) {
				System.out.print(","+h.mvd_cache[0][i][0]+","+h.mvd_cache[0][i][1]);
			} // for
			System.out.println();

			System.out.print("ctx.mvd_cache[1]: ");
			for(int i=0;i<40;i++) {
				System.out.print(","+h.mvd_cache[1][i][0]+","+h.mvd_cache[1][i][1]);
			} // for
			System.out.println();

			if(h.mvd_table[0] != null) {
				System.out.print("ctx.mvd_table[0]: ");
				for(int i=0;i<40;i++) {
					System.out.print(","+h.mvd_table[0][i][0]+","+h.mvd_table[0][i][1]);
				} // for
				System.out.println();
	
				System.out.print("ctx.mvd_table[1]: ");
				for(int i=0;i<40;i++) {
					System.out.print(","+h.mvd_table[1][i][0]+","+h.mvd_table[1][i][1]);
				} // for
				System.out.println();
			} // if
			
			System.out.print("ctx.ref_cache[0]: ");
			for(int i=0;i<40;i++) {
				System.out.print(","+h.ref_cache[0][i]+","+h.ref_cache[0][i]);
			} // for
			System.out.println();
			
			System.out.print("ctx.ref_cache[1]: ");
			for(int i=0;i<40;i++) {
				System.out.print(","+h.ref_cache[1][i]+","+h.ref_cache[1][i]);
			} // for
			System.out.println();

			System.out.print("error_status_table: ");
			if(h.s.error_status_table != null)
			for(int i=0;i</*h.s.error_status_table.length*/32 && h.s.error_status_table.length > 32;i++) {
				System.out.print(","+h.s.error_status_table[i]);
			} // for
			System.out.println();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
		} // try
	}

	public static void printDebugString(String msg) {
		if(!DEBUG_MODE) return;

		try {
			
			if(logCount==0) logCount++;
			
			System.out.print(msg);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
		} // try
	}
	
	public static void dumpFrameData(AVFrame frame) {
		if(!DEBUG_MODE) return;
		
		try {
			
			if(logCount==0) logCount++;

			System.out.println("****** DUMPING FRAME DATA ******");			
			int j = 0;
			for(int i=0;i</*2000*/100/*frame.data_base[0].length-frame.data_offset[0]*/;i++) {
				if(i%40 == 0) {
					System.out.println();
					System.out.print("["+j+"]: ");					
					j++;
				} // if
				System.out.print(""+frame.data_base[0][frame.data_offset[0]+i/*/*+13338-1024+8*512*/]+",");
			} // for
			System.out.println();
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
		} // try
		
	}
	
}

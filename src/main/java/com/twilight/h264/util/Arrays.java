package com.twilight.h264.util;

public class Arrays {

	public static boolean equals(Object array1,
            Object array2) {
		if(array1 == null || array2 == null) return false;
		int[] a1 = (int[])array1;
		int[] a2 = (int[])array2;
		if(a1.length != a2.length) return false;
		for(int i=0;i<a1.length;i++) {
			if(a1!=a2)
				return false;
		} // for
		return true;
	}
	
	public static void fill(int[] arr, int startIdxIncl, int endIdxExcl, int val) {
		for(int i=startIdxIncl;i<endIdxExcl;i++)
			arr[i] = val;
	}

	public static void fill(int[] arr, int val) {
		for(int i=0;i<arr.length;i++)
			arr[i] = val;
	}

	public static void fill(short[] arr, int startIdxIncl, int endIdxExcl, short val) {
		for(int i=startIdxIncl;i<endIdxExcl;i++)
			arr[i] = val;
	}

	public static void fill(short[] arr, short val) {
		for(int i=0;i<arr.length;i++)
			arr[i] = val;
	}
	
}

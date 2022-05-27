package com.twilight.h264.decoder;

import com.twilight.h264.util.*;

public class ImageUtils {
	
	public static final int PIX_FMT_BE        =1; ///< Pixel format is big-endian.
	public static final int PIX_FMT_PAL       =2; ///< Pixel format has a palette in data[1], values are indexes in this palette.
	public static final int PIX_FMT_BITSTREAM =4; ///< All values of a component are bit-wise packed end to end.
	public static final int PIX_FMT_HWACCEL   =8; ///< Pixel format is an HW accelerated format.

	// For JAVA .H264 Decoder, we support only Software Accelerated Format (PIX_FMT_YUV420P) 
	public static AVPixFmtDescriptor[] av_pix_fmt_descriptors 
		= new AVPixFmtDescriptor[] { null, new PixFmtYUV420P() }; 
	
//[PIX_FMT_NB] = {
//		    [PIX_FMT_YUV420P] = {
//		        .name = "yuv420p",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 1,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,0,1,0,7},        /* U */
//		            {2,0,1,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_YUYV422] = {
//		        .name = "yuyv422",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,1,0,7},        /* Y */
//		            {0,3,2,0,7},        /* U */
//		            {0,3,4,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_RGB24] = {
//		        .name = "rgb24",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,2,1,0,7},        /* R */
//		            {0,2,2,0,7},        /* G */
//		            {0,2,3,0,7},        /* B */
//		        },
//		    },
//		    [PIX_FMT_BGR24] = {
//		        .name = "bgr24",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,2,1,0,7},        /* B */
//		            {0,2,2,0,7},        /* G */
//		            {0,2,3,0,7},        /* R */
//		        },
//		    },
//		    [PIX_FMT_YUV422P] = {
//		        .name = "yuv422p",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,0,1,0,7},        /* U */
//		            {2,0,1,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_YUV444P] = {
//		        .name = "yuv444p",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,0,1,0,7},        /* U */
//		            {2,0,1,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_YUV410P] = {
//		        .name = "yuv410p",
//		        .nb_components= 3,
//		        .log2_chroma_w= 2,
//		        .log2_chroma_h= 2,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,0,1,0,7},        /* U */
//		            {2,0,1,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_YUV411P] = {
//		        .name = "yuv411p",
//		        .nb_components= 3,
//		        .log2_chroma_w= 2,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,0,1,0,7},        /* U */
//		            {2,0,1,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_GRAY8] = {
//		        .name = "gray",
//		        .nb_components= 1,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		        },
//		        .flags = PIX_FMT_PAL,
//		    },
//		    [PIX_FMT_MONOWHITE] = {
//		        .name = "monow",
//		        .nb_components= 1,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,0,0},        /* Y */
//		        },
//		        .flags = PIX_FMT_BITSTREAM,
//		    },
//		    [PIX_FMT_MONOBLACK] = {
//		        .name = "monob",
//		        .nb_components= 1,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,7,0},        /* Y */
//		        },
//		        .flags = PIX_FMT_BITSTREAM,
//		    },
//		    [PIX_FMT_PAL8] = {
//		        .name = "pal8",
//		        .nb_components= 1,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,0,7},
//		        },
//		        .flags = PIX_FMT_PAL,
//		    },
//		    [PIX_FMT_YUVJ420P] = {
//		        .name = "yuvj420p",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 1,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,0,1,0,7},        /* U */
//		            {2,0,1,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_YUVJ422P] = {
//		        .name = "yuvj422p",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,0,1,0,7},        /* U */
//		            {2,0,1,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_YUVJ444P] = {
//		        .name = "yuvj444p",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,0,1,0,7},        /* U */
//		            {2,0,1,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_XVMC_MPEG2_MC] = {
//		        .name = "xvmcmc",
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_XVMC_MPEG2_IDCT] = {
//		        .name = "xvmcidct",
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_UYVY422] = {
//		        .name = "uyvy422",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,2,0,7},        /* Y */
//		            {0,3,1,0,7},        /* U */
//		            {0,3,3,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_UYYVYY411] = {
//		        .name = "uyyvyy411",
//		        .nb_components= 3,
//		        .log2_chroma_w= 2,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,3,2,0,7},        /* Y */
//		            {0,5,1,0,7},        /* U */
//		            {0,5,4,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_BGR8] = {
//		        .name = "bgr8",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,6,1},        /* B */
//		            {0,0,1,3,2},        /* G */
//		            {0,0,1,0,2},        /* R */
//		        },
//		        .flags = PIX_FMT_PAL,
//		    },
//		    [PIX_FMT_BGR4] = {
//		        .name = "bgr4",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,3,1,0,0},        /* B */
//		            {0,3,2,0,1},        /* G */
//		            {0,3,4,0,0},        /* R */
//		        },
//		        .flags = PIX_FMT_BITSTREAM,
//		    },
//		    [PIX_FMT_BGR4_BYTE] = {
//		        .name = "bgr4_byte",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,3,0},        /* B */
//		            {0,0,1,1,1},        /* G */
//		            {0,0,1,0,0},        /* R */
//		        },
//		        .flags = PIX_FMT_PAL,
//		    },
//		    [PIX_FMT_RGB8] = {
//		        .name = "rgb8",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,6,1},        /* R */
//		            {0,0,1,3,2},        /* G */
//		            {0,0,1,0,2},        /* B */
//		        },
//		        .flags = PIX_FMT_PAL,
//		    },
//		    [PIX_FMT_RGB4] = {
//		        .name = "rgb4",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,3,1,0,0},       /* R */
//		            {0,3,2,0,1},       /* G */
//		            {0,3,4,0,0},       /* B */
//		        },
//		        .flags = PIX_FMT_BITSTREAM,
//		    },
//		    [PIX_FMT_RGB4_BYTE] = {
//		        .name = "rgb4_byte",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,0,1,3,0},        /* R */
//		            {0,0,1,1,1},        /* G */
//		            {0,0,1,0,0},        /* B */
//		        },
//		        .flags = PIX_FMT_PAL,
//		    },
//		    [PIX_FMT_NV12] = {
//		        .name = "nv12",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 1,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,1,1,0,7},        /* U */
//		            {1,1,2,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_NV21] = {
//		        .name = "nv21",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 1,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,1,1,0,7},        /* V */
//		            {1,1,2,0,7},        /* U */
//		        },
//		    },
//		    [PIX_FMT_ARGB] = {
//		        .name = "argb",
//		        .nb_components= 4,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,3,1,0,7},        /* A */
//		            {0,3,2,0,7},        /* R */
//		            {0,3,3,0,7},        /* G */
//		            {0,3,4,0,7},        /* B */
//		        },
//		    },
//		    [PIX_FMT_RGBA] = {
//		        .name = "rgba",
//		        .nb_components= 4,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,3,1,0,7},        /* R */
//		            {0,3,2,0,7},        /* G */
//		            {0,3,3,0,7},        /* B */
//		            {0,3,4,0,7},        /* A */
//		        },
//		    },
//		    [PIX_FMT_ABGR] = {
//		        .name = "abgr",
//		        .nb_components= 4,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,3,1,0,7},        /* A */
//		            {0,3,2,0,7},        /* B */
//		            {0,3,3,0,7},        /* G */
//		            {0,3,4,0,7},        /* R */
//		        },
//		    },
//		    [PIX_FMT_BGRA] = {
//		        .name = "bgra",
//		        .nb_components= 4,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,3,1,0,7},        /* B */
//		            {0,3,2,0,7},        /* G */
//		            {0,3,3,0,7},        /* R */
//		            {0,3,4,0,7},        /* A */
//		        },
//		    },
//		    [PIX_FMT_GRAY16BE] = {
//		        .name = "gray16be",
//		        .nb_components= 1,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,1,0,15},       /* Y */
//		        },
//		        .flags = PIX_FMT_BE,
//		    },
//		    [PIX_FMT_GRAY16LE] = {
//		        .name = "gray16le",
//		        .nb_components= 1,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,1,0,15},       /* Y */
//		        },
//		    },
//		    [PIX_FMT_YUV440P] = {
//		        .name = "yuv440p",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 1,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,0,1,0,7},        /* U */
//		            {2,0,1,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_YUVJ440P] = {
//		        .name = "yuvj440p",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 1,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,0,1,0,7},        /* U */
//		            {2,0,1,0,7},        /* V */
//		        },
//		    },
//		    [PIX_FMT_YUVA420P] = {
//		        .name = "yuva420p",
//		        .nb_components= 4,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 1,
//		        .comp = {
//		            {0,0,1,0,7},        /* Y */
//		            {1,0,1,0,7},        /* U */
//		            {2,0,1,0,7},        /* V */
//		            {3,0,1,0,7},        /* A */
//		        },
//		    },
//		    [PIX_FMT_VDPAU_H264] = {
//		        .name = "vdpau_h264",
//		        .log2_chroma_w = 1,
//		        .log2_chroma_h = 1,
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_VDPAU_MPEG1] = {
//		        .name = "vdpau_mpeg1",
//		        .log2_chroma_w = 1,
//		        .log2_chroma_h = 1,
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_VDPAU_MPEG2] = {
//		        .name = "vdpau_mpeg2",
//		        .log2_chroma_w = 1,
//		        .log2_chroma_h = 1,
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_VDPAU_WMV3] = {
//		        .name = "vdpau_wmv3",
//		        .log2_chroma_w = 1,
//		        .log2_chroma_h = 1,
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_VDPAU_VC1] = {
//		        .name = "vdpau_vc1",
//		        .log2_chroma_w = 1,
//		        .log2_chroma_h = 1,
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_VDPAU_MPEG4] = {
//		        .name = "vdpau_mpeg4",
//		        .log2_chroma_w = 1,
//		        .log2_chroma_h = 1,
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_RGB48BE] = {
//		        .name = "rgb48be",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,5,1,0,15},       /* R */
//		            {0,5,3,0,15},       /* G */
//		            {0,5,5,0,15},       /* B */
//		        },
//		        .flags = PIX_FMT_BE,
//		    },
//		    [PIX_FMT_RGB48LE] = {
//		        .name = "rgb48le",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,5,1,0,15},       /* R */
//		            {0,5,3,0,15},       /* G */
//		            {0,5,5,0,15},       /* B */
//		        },
//		    },
//		    [PIX_FMT_RGB565BE] = {
//		        .name = "rgb565be",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,0,3,4},        /* R */
//		            {0,1,1,5,5},        /* G */
//		            {0,1,1,0,4},        /* B */
//		        },
//		        .flags = PIX_FMT_BE,
//		    },
//		    [PIX_FMT_RGB565LE] = {
//		        .name = "rgb565le",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,2,3,4},        /* R */
//		            {0,1,1,5,5},        /* G */
//		            {0,1,1,0,4},        /* B */
//		        },
//		    },
//		    [PIX_FMT_RGB555BE] = {
//		        .name = "rgb555be",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,0,2,4},        /* R */
//		            {0,1,1,5,4},        /* G */
//		            {0,1,1,0,4},        /* B */
//		        },
//		        .flags = PIX_FMT_BE,
//		    },
//		    [PIX_FMT_RGB555LE] = {
//		        .name = "rgb555le",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,2,2,4},        /* R */
//		            {0,1,1,5,4},        /* G */
//		            {0,1,1,0,4},        /* B */
//		        },
//		    },
//		    [PIX_FMT_RGB444BE] = {
//		        .name = "rgb444be",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,0,0,3},        /* R */
//		            {0,1,1,4,3},        /* G */
//		            {0,1,1,0,3},        /* B */
//		        },
//		        .flags = PIX_FMT_BE,
//		    },
//		    [PIX_FMT_RGB444LE] = {
//		        .name = "rgb444le",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,2,0,3},        /* R */
//		            {0,1,1,4,3},        /* G */
//		            {0,1,1,0,3},        /* B */
//		        },
//		    },
//		    [PIX_FMT_BGR565BE] = {
//		        .name = "bgr565be",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,0,3,4},        /* B */
//		            {0,1,1,5,5},        /* G */
//		            {0,1,1,0,4},        /* R */
//		        },
//		        .flags = PIX_FMT_BE,
//		    },
//		    [PIX_FMT_BGR565LE] = {
//		        .name = "bgr565le",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,2,3,4},        /* B */
//		            {0,1,1,5,5},        /* G */
//		            {0,1,1,0,4},        /* R */
//		        },
//		    },
//		    [PIX_FMT_BGR555BE] = {
//		        .name = "bgr555be",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,0,2,4},       /* B */
//		            {0,1,1,5,4},       /* G */
//		            {0,1,1,0,4},       /* R */
//		        },
//		        .flags = PIX_FMT_BE,
//		     },
//		    [PIX_FMT_BGR555LE] = {
//		        .name = "bgr555le",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,2,2,4},        /* B */
//		            {0,1,1,5,4},        /* G */
//		            {0,1,1,0,4},        /* R */
//		        },
//		    },
//		    [PIX_FMT_BGR444BE] = {
//		        .name = "bgr444be",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,0,0,3},       /* B */
//		            {0,1,1,4,3},       /* G */
//		            {0,1,1,0,3},       /* R */
//		        },
//		        .flags = PIX_FMT_BE,
//		     },
//		    [PIX_FMT_BGR444LE] = {
//		        .name = "bgr444le",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,2,0,3},        /* B */
//		            {0,1,1,4,3},        /* G */
//		            {0,1,1,0,3},        /* R */
//		        },
//		    },
//		    [PIX_FMT_VAAPI_MOCO] = {
//		        .name = "vaapi_moco",
//		        .log2_chroma_w = 1,
//		        .log2_chroma_h = 1,
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_VAAPI_IDCT] = {
//		        .name = "vaapi_idct",
//		        .log2_chroma_w = 1,
//		        .log2_chroma_h = 1,
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_VAAPI_VLD] = {
//		        .name = "vaapi_vld",
//		        .log2_chroma_w = 1,
//		        .log2_chroma_h = 1,
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_YUV420P16LE] = {
//		        .name = "yuv420p16le",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 1,
//		        .comp = {
//		            {0,1,1,0,15},        /* Y */
//		            {1,1,1,0,15},        /* U */
//		            {2,1,1,0,15},        /* V */
//		        },
//		    },
//		    [PIX_FMT_YUV420P16BE] = {
//		        .name = "yuv420p16be",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 1,
//		        .comp = {
//		            {0,1,1,0,15},        /* Y */
//		            {1,1,1,0,15},        /* U */
//		            {2,1,1,0,15},        /* V */
//		        },
//		        .flags = PIX_FMT_BE,
//		    },
//		    [PIX_FMT_YUV422P16LE] = {
//		        .name = "yuv422p16le",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,1,0,15},        /* Y */
//		            {1,1,1,0,15},        /* U */
//		            {2,1,1,0,15},        /* V */
//		        },
//		    },
//		    [PIX_FMT_YUV422P16BE] = {
//		        .name = "yuv422p16be",
//		        .nb_components= 3,
//		        .log2_chroma_w= 1,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,1,0,15},        /* Y */
//		            {1,1,1,0,15},        /* U */
//		            {2,1,1,0,15},        /* V */
//		        },
//		        .flags = PIX_FMT_BE,
//		    },
//		    [PIX_FMT_YUV444P16LE] = {
//		        .name = "yuv444p16le",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,1,0,15},        /* Y */
//		            {1,1,1,0,15},        /* U */
//		            {2,1,1,0,15},        /* V */
//		        },
//		    },
//		    [PIX_FMT_YUV444P16BE] = {
//		        .name = "yuv444p16be",
//		        .nb_components= 3,
//		        .log2_chroma_w= 0,
//		        .log2_chroma_h= 0,
//		        .comp = {
//		            {0,1,1,0,15},        /* Y */
//		            {1,1,1,0,15},        /* U */
//		            {2,1,1,0,15},        /* V */
//		        },
//		        .flags = PIX_FMT_BE,
//		    },
//		    [PIX_FMT_DXVA2_VLD] = {
//		        .name = "dxva2_vld",
//		        .log2_chroma_w = 1,
//		        .log2_chroma_h = 1,
//		        .flags = PIX_FMT_HWACCEL,
//		    },
//		    [PIX_FMT_Y400A] = {
//		        .name = "y400a",
//		        .nb_components= 2,
//		        .comp = {
//		            {0,1,1,0,7},        /* Y */
//		            {0,1,2,0,7},        /* A */
//		        },
//		    },
//		};
	

	public static void av_image_copy(/* uint8_t * */int[][] dst_base,
			int[] dst_offset, 
			int[] dst_linesizes,
			/* const uint8_t * */int[][] src_base, 
			int[] src_offset,
			int[] src_linesizes,
			/* enum PixelFormat */int pix_fmt, int width, int height) {
		AVPixFmtDescriptor desc = av_pix_fmt_descriptors[pix_fmt];

		// No H/W Accel
		// if (desc.flags & PIX_FMT_HWACCEL)
		// return;

		if ((desc.flags & PIX_FMT_PAL) != 0) {
			av_image_copy_plane(dst_base[0], dst_offset[0], dst_linesizes[0], src_base[0], src_offset[0],
					src_linesizes[0], width, height);
			/* copy the palette */
			System.arraycopy(src_base[1], src_offset[1], dst_base[1], dst_offset[1], 4 * 256);
			// memcpy(dst_data[1], src_data[1], 4*256);
		} else {
			int i, planes_nb = 0;

			for (i = 0; i < desc.nb_components; i++)
				planes_nb = Math.max(planes_nb, desc.comp[i].plane + 1);

			for (i = 0; i < planes_nb; i++) {
				int h = height;
				int bwidth = av_image_get_linesize(pix_fmt, width, i);
				if (i == 1 || i == 2) {
					h = -((-height) >> desc.log2_chroma_h);
				}
				av_image_copy_plane(dst_base[i], dst_offset[i], dst_linesizes[i], src_base[i], src_offset[i],
						src_linesizes[i], bwidth, h);
			}
		}
	}

	public static void av_image_copy_plane(/* uint8_t * */int[] dst,
			int _dst_offset,
			int dst_linesize,
			/* const uint8_t * */int[] src, 
			int _src_offset,
			int src_linesize, int bytewidth,
			int height) {
		int dst_offset = _dst_offset;
		int src_offset = _src_offset;
		if (dst == null || src == null)

			return;
		for (; height > 0; height--) {
			//memcpy(dst, src, bytewidth);
			System.arraycopy(src, src_offset, dst, dst_offset, bytewidth);
			dst_offset += dst_linesize;
			src_offset += src_linesize;
		}
	}

	public static int av_image_get_linesize(/*enum PixelFormat*/int pix_fmt, int width, int plane)
	{
	    AVPixFmtDescriptor desc = av_pix_fmt_descriptors[pix_fmt];
	    int max_step[] = new int[4];       /* max pixel step for each plane */
	    int max_step_comp[] = new int[4];       /* the component for each plane which has the max pixel step */
	    int s;

	    if ((desc.flags & PIX_FMT_BITSTREAM)!=0)
	        return (width * (desc.comp[0].step_minus1+1) + 7) >> 3;

	    av_image_fill_max_pixsteps(max_step, max_step_comp, desc);
	    s = (max_step_comp[plane] == 1 || max_step_comp[plane] == 2) ? desc.log2_chroma_w : 0;
	    return max_step[plane] * (((width + (1 << s) - 1)) >> s);
	}
	
	public static void av_image_fill_max_pixsteps(int[] max_pixsteps,
			int[] max_pixstep_comps, AVPixFmtDescriptor pixdesc) {
		int i;
		// memset(max_pixsteps, 0, 4*sizeof(max_pixsteps[0]));
		Arrays.fill(max_pixsteps, 0, 4, 0);
		if (max_pixstep_comps != null)
			// memset(max_pixstep_comps, 0, 4*sizeof(max_pixstep_comps[0]));
			Arrays.fill(max_pixstep_comps, 0, 4, 0);

		for (i = 0; i < 4; i++) {
			AVComponentDescriptor comp = (pixdesc.comp[i]);
			if ((comp.step_minus1 + 1) > max_pixsteps[comp.plane]) {
				max_pixsteps[comp.plane] = comp.step_minus1 + 1;
				if (max_pixstep_comps != null)
					max_pixstep_comps[comp.plane] = i;
			}
		}
	}
	
	
}

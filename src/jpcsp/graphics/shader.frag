// INSERT VERSION

// INSERT DEFINES

#extension GL_EXT_gpu_shader4 : enable
#if USE_UBO
#   extension GL_ARB_uniform_buffer_object : enable
#endif
#if __VERSION__ >= 130
#   extension GL_ARB_compatibility : enable
#endif

#if USE_UBO
	UBO_STRUCTURE
#else
    uniform bool      texEnable;
    uniform ivec2     texEnvMode;
    uniform int       texMapMode;
    uniform float     colorDoubling;
    uniform bool      ctestEnable;
    uniform int       ctestFunc;
    uniform ivec3     ctestRef;
    uniform ivec3     ctestMsk;
    #if USE_NATIVE_CLUT
        uniform int   clutShift;
        uniform int   clutMask;
        uniform int   clutOffset;
        uniform int   texPixelFormat;
    #endif
#endif
uniform sampler2D tex;

#if USE_NATIVE_CLUT
    uniform usampler2D utex;
    uniform sampler2D clut;
#endif

vec4 getFragColor()
{
    vec4 Cp = gl_Color;
    vec4 Cs = gl_SecondaryColor;
    vec4 Cd = vec4(vec3(colorDoubling), 1.0);

    if (texEnable)
    {
#if USE_NATIVE_CLUT
        uint Ci;
        int clutIndex;
        vec4 Ct;
        switch (texPixelFormat)
        {
            case 4:  // TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
            case 5:  // TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
            case 6:  // TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
            case 7:  // TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
                // Indexed texture (using a CLUT)
                // The index is stored in the RED component of the texture
                Ci = texture2DProj(utex, gl_TexCoord[0].xyz).r;
                clutIndex = int((Ci >> uint(clutShift)) & uint(clutMask)) + clutOffset;
                // The CLUT is defined as a Nx1 texture
                Ct = texelFetch(clut, ivec2(clutIndex, 0), 0);
                break;

            case 11: // RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650
                // Indexed texture (using a CLUT)
                // The index is stored in the color components of the texture
                // and must be transformed into 16-bit (BGR5650)
                Ct.rgb = texture2DProj(tex, gl_TexCoord[0].xyz).rgb * vec3(31.0, 63.0, 31.0);
                Ci = uint(Ct.r) | (uint(Ct.g) << 5u) | (uint(Ct.b) << 11u);
                clutIndex = int((Ci >> uint(clutShift)) & uint(clutMask)) + clutOffset;
                // The CLUT is defined as a Nx1 texture
                Ct = texelFetch(clut, ivec2(clutIndex, 0), 0);
                break;

            case 12: // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR5551
                // Indexed texture (using a CLUT)
                // The index is stored in the color components of the texture
                // and must be transformed into 16-bit (ABGR5551)
                Ct = texture2DProj(tex, gl_TexCoord[0].xyz) * vec4(31.0, 31.0, 31.0, 1.0);
                Ci = uint(Ct.r) | (uint(Ct.g) << 5u) | (uint(Ct.b) << 10u) | (uint(Ct.a) << 15u);
                clutIndex = int((Ci >> uint(clutShift)) & uint(clutMask)) + clutOffset;
                // The CLUT is defined as a Nx1 texture
                Ct = texelFetch(clut, ivec2(clutIndex, 0), 0);
                break;

            case 13: // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR4444
                // Indexed texture (using a CLUT)
                // The index is stored in the color components of the texture
                // and must be transformed into 16-bit (ABGR4444)
                Ct = texture2DProj(tex, gl_TexCoord[0].xyz) * 15.0;
                Ci = uint(Ct.r) | (uint(Ct.g) << 4u) | (uint(Ct.b) << 8u) | (uint(Ct.a) << 12u);
                clutIndex = int((Ci >> uint(clutShift)) & uint(clutMask)) + clutOffset;
                // The CLUT is defined as a Nx1 texture
                Ct = texelFetch(clut, ivec2(clutIndex, 0), 0);
                break;

            case 14: // RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888
                // Indexed texture (using a CLUT)
                // The index is stored in the color components of the texture
                // and must be transformed into 32-bit (ABGR8888)
                Ct = texture2DProj(tex, gl_TexCoord[0].xyz) * 255.0;
                Ci = uint(Ct.r) | (uint(Ct.g) << 8u) | (uint(Ct.b) << 16u) | (uint(Ct.a) << 24u);
                clutIndex = int((Ci >> uint(clutShift)) & uint(clutMask)) + clutOffset;
                // The CLUT is defined as a Nx1 texture
                Ct = texelFetch(clut, ivec2(clutIndex, 0), 0);
                break;

        	case 0:  // TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
        	case 1:  // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
        	case 2:  // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
        	case 3:  // TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
        	case 8:  // TPSM_PIXEL_STORAGE_MODE_DXT1
        	case 9:  // TPSM_PIXEL_STORAGE_MODE_DXT3
        	case 10: // TPSM_PIXEL_STORAGE_MODE_DXT5
        	default:
                // Non-indexed texture
                Ct = texture2DProj(tex, gl_TexCoord[0].xyz);
                break;
        }
#else
        vec4 Ct = texture2DProj(tex, gl_TexCoord[0].xyz);
#endif

        if (texEnvMode[1] == 0) // RGB
        {
            Ct.a = 1.0;
        }

        switch (texEnvMode[0])
        {
        case 0: // MODULATE
            Cp = Cp * Ct;
            break;
        case 1: // DECAL
            Cp.rgb = mix(Cp.rgb, Ct.rgb, Ct.a);
            break;
        case 2: // BLEND
            Cp.rgb = mix(Cp.rgb, gl_TextureEnvColor[0].rgb, Ct.rgb);
            Cp.a   = Cp.a * Ct.a;
            break;
        case 3: // REPLACE
            Cp.rgb = Ct.rgb;
            if (texEnvMode[1] != 0) Cp.a = Ct.a;
            break;
        case 4: // ADD
            Cp.rgb = Cp.rgb + Ct.rgb;
            Cp.a   = Cp.a   * Ct.a;
            break;
        }

        return clamp(Cd * (Cp + Cs), 0.0, 1.0);
    }
    else
    {
        return clamp(Cp + Cs, 0.0, 1.0);
    }
}

void main()
{
    vec4  Cf = getFragColor();

    if (ctestEnable)
    {
        ivec3 Cs = ivec3(Cf.rgb * 255.0);
        switch (ctestFunc)
        {
        case 0:
            discard;
        case 2:
            if ((Cs & ctestMsk) == (ctestRef & ctestMsk)) break;
            discard;
        case 3:
            if ((Cs & ctestMsk) != (ctestRef & ctestMsk)) break;
            discard;
        default:
            break;
        }
    }

	// TODO: ColorMask implementation has to mix the color
	// from the already rendered buffer with the fragment color:
	//   ivec4 Cs = ivec4(Cf * 255.0)
	//   ivec4 Cd = color from rendered buffer (how to access it?)
	//   Cf = ((Cd & colorMask) | (Cs & ~colorMask)) / 255.0

    gl_FragColor = Cf;
}

// INSERT VERSION

// INSERT DEFINES

#if USE_BIT_OPERATORS
    #extension GL_EXT_gpu_shader4 : enable
#endif
#if USE_UBO
    #extension GL_ARB_uniform_buffer_object : enable
#endif
#if __VERSION__ >= 130 && defined(GL_ARB_compatibility)
    #extension GL_ARB_compatibility : enable
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


///////////////////////////////////////////////////////////////
// Decode Indexed Texture & Compute Fragment Color
///////////////////////////////////////////////////////////////

#if USE_NATIVE_CLUT
#if !USE_DYNAMIC_DEFINES || (TEX_PIXEL_FORMAT >= 4 && TEX_PIXEL_FORMAT <= 7)
// Indexed texture (using a CLUT)
// The index is stored in the RED component of the texture
vec4 getIndexedTextureRED()
{
    uint Ci = texture2DProj(utex, gl_TexCoord[0].xyz).r;
    #if !USE_DYNAMIC_DEFINES || CLUT_INDEX_HINT == 0
        int clutIndex = int((Ci >> uint(clutShift)) & uint(clutMask)) + clutOffset;
    #elif CLUT_INDEX_HINT == 1
        // RE_CLUT_INDEX_RED_ONLY
        int clutIndex = int(Ci & uint(clutMask));
    #else
        // RE_CLUT_INDEX_GREEN_ONLY
        // RE_CLUT_INDEX_BLUE_ONLY
        // RE_CLUT_INDEX_ALPHA_ONLY
        int clutIndex = int((Ci >> uint(clutShift)) & uint(clutMask));
    #endif
    // The CLUT is defined as a Nx1 texture
    return texelFetch(clut, ivec2(clutIndex, 0), 0);
}
#endif


#if !USE_DYNAMIC_DEFINES || TEX_PIXEL_FORMAT == 11
// Indexed texture (using a CLUT)
// The index is stored in the color components of the texture
// and must be transformed into 16-bit (BGR5650)
vec4 getIndexedTexture5650()
{
    vec3 Ct = texture2DProj(tex, gl_TexCoord[0].xyz).rgb * vec3(31.0, 63.0, 31.0);
    #if !USE_DYNAMIC_DEFINES || CLUT_INDEX_HINT == 0
        uint Ci = uint(Ct.r) | (uint(Ct.g) << 5u) | (uint(Ct.b) << 11u);
        int clutIndex = int((Ci >> uint(clutShift)) & uint(clutMask)) + clutOffset;
    #elif CLUT_INDEX_HINT == 1
        // RE_CLUT_INDEX_RED_ONLY
        int clutIndex = int(Ct.r);
    #elif CLUT_INDEX_HINT == 2
        // RE_CLUT_INDEX_GREEN_ONLY
        int clutIndex = int(Ct.g);
    #elif CLUT_INDEX_HINT == 3
        // RE_CLUT_INDEX_BLUE_ONLY
        int clutIndex = int(Ct.b);
    #elif CLUT_INDEX_HINT == 4
        // RE_CLUT_INDEX_ALPHA_ONLY
        int clutIndex = 0;
    #endif
    // The CLUT is defined as a Nx1 texture
    return texelFetch(clut, ivec2(clutIndex, 0), 0);
}
#endif


#if !USE_DYNAMIC_DEFINES || TEX_PIXEL_FORMAT == 12
// Indexed texture (using a CLUT)
// The index is stored in the color components of the texture
// and must be transformed into 16-bit (ABGR5551)
vec4 getIndexedTexture5551()
{
    vec4 Ct = texture2DProj(tex, gl_TexCoord[0].xyz) * vec4(31.0, 31.0, 31.0, 1.0);
    #if !USE_DYNAMIC_DEFINES || CLUT_INDEX_HINT == 0
        uint Ci = uint(Ct.r) | (uint(Ct.g) << 5u) | (uint(Ct.b) << 10u) | (uint(Ct.a) << 15u);
        int clutIndex = int((Ci >> uint(clutShift)) & uint(clutMask)) + clutOffset;
    #elif CLUT_INDEX_HINT == 1
        // RE_CLUT_INDEX_RED_ONLY
        int clutIndex = int(Ct.r);
    #elif CLUT_INDEX_HINT == 2
        // RE_CLUT_INDEX_GREEN_ONLY
        int clutIndex = int(Ct.g);
    #elif CLUT_INDEX_HINT == 3
        // RE_CLUT_INDEX_BLUE_ONLY
        int clutIndex = int(Ct.b);
    #elif CLUT_INDEX_HINT == 4
        // RE_CLUT_INDEX_ALPHA_ONLY
        int clutIndex = int(Ct.a);
    #endif
    // The CLUT is defined as a Nx1 texture
    return texelFetch(clut, ivec2(clutIndex, 0), 0);
}
#endif


#if !USE_DYNAMIC_DEFINES || TEX_PIXEL_FORMAT == 13
// Indexed texture (using a CLUT)
// The index is stored in the color components of the texture
// and must be transformed into 16-bit (ABGR4444)
vec4 getIndexedTexture4444()
{
    vec4 Ct = texture2DProj(tex, gl_TexCoord[0].xyz) * vec4(15.0);
    #if !USE_DYNAMIC_DEFINES || CLUT_INDEX_HINT == 0
        uint Ci = uint(Ct.r) | (uint(Ct.g) << 4u) | (uint(Ct.b) << 8u) | (uint(Ct.a) << 12u);
        int clutIndex = int((Ci >> uint(clutShift)) & uint(clutMask)) + clutOffset;
    #elif CLUT_INDEX_HINT == 1
        // RE_CLUT_INDEX_RED_ONLY
        int clutIndex = int(Ct.r);
    #elif CLUT_INDEX_HINT == 2
        // RE_CLUT_INDEX_GREEN_ONLY
        int clutIndex = int(Ct.g);
    #elif CLUT_INDEX_HINT == 3
        // RE_CLUT_INDEX_BLUE_ONLY
        int clutIndex = int(Ct.b);
    #elif CLUT_INDEX_HINT == 4
        // RE_CLUT_INDEX_ALPHA_ONLY
        int clutIndex = int(Ct.a);
    #endif
    // The CLUT is defined as a Nx1 texture
    return texelFetch(clut, ivec2(clutIndex, 0), 0);
}
#endif


#if !USE_DYNAMIC_DEFINES || TEX_PIXEL_FORMAT == 14
// Indexed texture (using a CLUT)
// The index is stored in the color components of the texture
// and must be transformed into 32-bit (ABGR8888)
vec4 getIndexedTexture8888()
{
    vec4 Ct = texture2DProj(tex, gl_TexCoord[0].xyz) * vec4(255.0);
    #if !USE_DYNAMIC_DEFINES || CLUT_INDEX_HINT == 0
        uint Ci = uint(Ct.r) | (uint(Ct.g) << 8u) | (uint(Ct.b) << 16u) | (uint(Ct.a) << 24u);
        int clutIndex = int((Ci >> uint(clutShift)) & uint(clutMask)) + clutOffset;
    #elif CLUT_INDEX_HINT == 1
        // RE_CLUT_INDEX_RED_ONLY
        int clutIndex = int(Ct.r);
    #elif CLUT_INDEX_HINT == 2
        // RE_CLUT_INDEX_GREEN_ONLY
        int clutIndex = int(Ct.g);
    #elif CLUT_INDEX_HINT == 3
        // RE_CLUT_INDEX_BLUE_ONLY
        int clutIndex = int(Ct.b);
    #elif CLUT_INDEX_HINT == 4
        // RE_CLUT_INDEX_ALPHA_ONLY
        int clutIndex = int(Ct.a);
    #endif
    // The CLUT is defined as a Nx1 texture
    return texelFetch(clut, ivec2(clutIndex, 0), 0);
}
#endif
#endif


// Non-indexed texture
vec4 getNonIndexedTexture()
{
    return texture2DProj(tex, gl_TexCoord[0].xyz);
}


vec4 getTextureColor(in vec4 Cp, in vec4 Cs)
{
    #if USE_NATIVE_CLUT
        #if !USE_DYNAMIC_DEFINES
            vec4 Ct;
            switch (texPixelFormat)
            {
                case 4:  // TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
                case 5:  // TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
                case 6:  // TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
                case 7:  // TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
                    Ct = getIndexedTextureRED(); break;
                case 11: // RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650
                    Ct = getIndexedTexture5650(); break;
                case 12: // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR5551
                    Ct = getIndexedTexture5551(); break;
                case 13: // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR4444
                    Ct = getIndexedTexture4444(); break;
                case 14: // RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888
                    Ct = getIndexedTexture8888(); break;
            	case 0:  // TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
            	case 1:  // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
            	case 2:  // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
        	    case 3:  // TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
            	case 8:  // TPSM_PIXEL_STORAGE_MODE_DXT1
            	case 9:  // TPSM_PIXEL_STORAGE_MODE_DXT3
            	case 10: // TPSM_PIXEL_STORAGE_MODE_DXT5
            	default:
                    Ct = getNonIndexedTexture(); break;
            }
        #elif TEX_PIXEL_FORMAT >= 4 && TEX_PIXEL_FORMAT <= 7
            vec4 Ct = getIndexedTextureRED();
        #elif TEX_PIXEL_FORMAT == 11
            vec4 Ct = getIndexedTexture5650();
        #elif TEX_PIXEL_FORMAT == 12
            vec4 Ct = getIndexedTexture5551();
        #elif TEX_PIXEL_FORMAT == 13
            vec4 Ct = getIndexedTexture4444();
        #elif TEX_PIXEL_FORMAT == 14
            vec4 Ct = getIndexedTexture8888();
        #else
            vec4 Ct = getNonIndexedTexture();
        #endif
    #else
        vec4 Ct = getNonIndexedTexture();
    #endif

    #if !USE_DYNAMIC_DEFINES
        if (texEnvMode[1] == 0) // RGB
        {
            Ct.a = 1.0;
        }
    #elif TEX_ENV_MODE1 == 0
        // This case is implemented using "#if TEX_ENV_MODE1 != 0"
        //Ct.a = 1.0;
    #endif

    #if !USE_DYNAMIC_DEFINES
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
    #elif TEX_ENV_MODE0 == 0
        // MODULATE
        #if TEX_ENV_MODE1 != 0
            Cp = Cp * Ct;
        #else
            Cp.rgb = Cp.rgb * Ct.rgb;
        #endif
    #elif TEX_ENV_MODE0 == 1
        // DECAL
        #if TEX_ENV_MODE1 != 0
            Cp.rgb = mix(Cp.rgb, Ct.rgb, Ct.a);
        #else
            Cp.rgb = Ct.rgb;
        #endif
    #elif TEX_ENV_MODE0 == 2
        // BLEND
        Cp.rgb = mix(Cp.rgb, gl_TextureEnvColor[0].rgb, Ct.rgb);
        #if TEX_ENV_MODE1 != 0
            Cp.a = Cp.a * Ct.a;
        #endif
    #elif TEX_ENV_MODE0 == 3
        // REPLACE
        Cp.rgb = Ct.rgb;
        #if TEX_ENV_MODE1 != 0
            Cp.a = Ct.a;
        #endif
    #elif TEX_ENV_MODE0 == 4
        // ADD
        Cp.rgb = Cp.rgb + Ct.rgb;
        #if TEX_ENV_MODE1 != 0
            Cp.a = Cp.a * Ct.a;
        #endif
    #endif

    #if !USE_DYNAMIC_DEFINES
        vec4 Cd = vec4(vec3(colorDoubling), 1.0);
        return clamp(Cd * (Cp + Cs), 0.0, 1.0);
    #elif COLOR_DOUBLING == 1
        return clamp((Cp + Cs), 0.0, 1.0);
    #else
        vec4 Cd = vec4(vec3(COLOR_DOUBLING), 1.0);
        return clamp(Cd * (Cp + Cs), 0.0, 1.0);
    #endif
}


vec4 getFragColor()
{
    #if !USE_DYNAMIC_DEFINES
        if (texEnable)
        {
            return getTextureColor(gl_Color, gl_SecondaryColor);
        }
        else
        {
            return clamp(gl_Color + gl_SecondaryColor, 0.0, 1.0);
        }
    #elif TEX_ENABLE
        return getTextureColor(gl_Color, gl_SecondaryColor);
    #else
        return clamp(gl_Color + gl_SecondaryColor, 0.0, 1.0);
    #endif
}


///////////////////////////////////////////////////////////////
// Color Test
///////////////////////////////////////////////////////////////

#if !USE_DYNAMIC_DEFINES || CTEST_ENABLE
void ColorTest(in vec3 Cf)
{
    #if !USE_DYNAMIC_DEFINES
        ivec3 Cs = ivec3(Cf * 255.0);
        switch (ctestFunc)
        {
        case 0:
            discard;
        case 2:
            #if USE_BIT_OPERATORS
                if ((Cs & ctestMsk) == (ctestRef & ctestMsk)) break;
                discard;
            #else
                // Not available without bit operators :-(
                break;
            #endif
        case 3:
            #if USE_BIT_OPERATORS
                if ((Cs & ctestMsk) != (ctestRef & ctestMsk)) break;
                discard;
            #else
                // Not available without bit operators :-(
                break;
            #endif
        default:
            break;
        }
    #elif CTEST_FUNC == 0
        discard;
    #elif CTEST_FUNC == 2 && USE_BIT_OPERATORS
        ivec3 Cs = ivec3(Cf * 255.0);
        if ((Cs & ctestMsk) != (ctestRef & ctestMsk)) discard;
    #elif CTEST_FUNC == 3 && USE_BIT_OPERATORS
        ivec3 Cs = ivec3(Cf * 255.0);
        if ((Cs & ctestMsk) == (ctestRef & ctestMsk)) discard;
    #endif
}
#endif


///////////////////////////////////////////////////////////////
// Main program
///////////////////////////////////////////////////////////////

void main()
{
    vec4 Cf = getFragColor();

    #if !USE_DYNAMIC_DEFINES
        if (ctestEnable) ColorTest(Cf.rgb);
    #elif CTEST_ENABLE
        ColorTest(Cf.rgb);
    #endif

	// TODO: ColorMask implementation has to mix the color
	// from the already rendered buffer with the fragment color:
	//   ivec4 Cs = ivec4(Cf * 255.0)
	//   ivec4 Cd = color from rendered buffer (how to access it?)
	//   Cf = ((Cd & colorMask) | (Cs & ~colorMask)) / 255.0

    gl_FragColor = Cf;
}

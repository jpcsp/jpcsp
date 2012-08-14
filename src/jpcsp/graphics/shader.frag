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

// The built-in function round() is only available in v1.30 or later
#if __VERSION__ >= 130
    #define ROUND(n) round(n)
#else
    #define ROUND(n) floor((n) + 0.5)
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
    uniform bool  stencilTestEnable;
    uniform int   stencilFunc;
    uniform int   stencilRef;
    uniform int   stencilMask;
    uniform int   stencilOpFail;
    uniform int   stencilOpZFail;
    uniform int   stencilOpZPass;
    uniform bool  depthTestEnable;
    uniform int   depthFunc;
    uniform int   depthMask;
    uniform bool  colorMaskEnable;
    uniform ivec4 colorMask;
    uniform ivec4 notColorMask;
    uniform bool  alphaTestEnable;
    uniform int   alphaTestFunc;
    uniform int   alphaTestRef;
    uniform bool  blendTestEnable;
    uniform int   blendEquation;
    uniform int   blendSrc;
    uniform int   blendDst;
    uniform vec3  blendSFix;
    uniform vec3  blendDFix;
#endif
uniform sampler2D tex;   // The active texture
uniform sampler2D fbTex; // The texture containing the current screen (FrameBuffer)

#if USE_NATIVE_CLUT
    uniform usampler2D utex;
    uniform sampler2D clut;
#endif

#if __VERSION__ >= 140
    #define TEXTURE_2D_PROJ textureProj
#else
    #define TEXTURE_2D_PROJ texture2DProj
#endif


///////////////////////////////////////////////////////////////
// Common functions
///////////////////////////////////////////////////////////////

ivec2 getFragCoord()
{
	//
	// gl_FragCoord is holding the window relative coordinates for the fragment
	//     gl_FragCoord.x is in range [0..width-1]
	//     gl_FragCoord.y is in range [0..height-1]
	// Rem.: gl_FragCoord x and y are offset by a half-point (0.5, 0.5).
	//       This is truncated while converting to an integer.
	// i.e.:
	//     vec4 screenColor = texelFetch(fbTex, getFragCoord(), 0);
	//
	return ivec2(gl_FragCoord.xy);
}

///////////////////////////////////////////////////////////////
// Decode Indexed Texture & Compute Fragment Color
///////////////////////////////////////////////////////////////

#if USE_NATIVE_CLUT
#if !USE_DYNAMIC_DEFINES || (TEX_PIXEL_FORMAT >= 4 && TEX_PIXEL_FORMAT <= 7)
// Indexed texture (using a CLUT)
// The index is stored in the RED component of the texture
vec4 getIndexedTextureRED()
{
    uint Ci = TEXTURE_2D_PROJ(utex, gl_TexCoord[0].xyz).r;
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
    vec3 Ct = TEXTURE_2D_PROJ(tex, gl_TexCoord[0].xyz).rgb * vec3(31.0, 63.0, 31.0);
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
    vec4 Ct = TEXTURE_2D_PROJ(tex, gl_TexCoord[0].xyz) * vec4(31.0, 31.0, 31.0, 1.0);
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
    vec4 Ct = TEXTURE_2D_PROJ(tex, gl_TexCoord[0].xyz) * vec4(15.0);
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
    vec4 Ct = ROUND(TEXTURE_2D_PROJ(tex, gl_TexCoord[0].xyz) * 255.0);
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
    return TEXTURE_2D_PROJ(tex, gl_TexCoord[0].xyz);
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
void ApplyColorTest(in vec3 Cf)
{
    #if !USE_DYNAMIC_DEFINES
        ivec3 Cs = ivec3(ROUND(Cf * 255.0));
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
        ivec3 Cs = ivec3(ROUND(Cf * 255.0));
        if ((Cs & ctestMsk) != (ctestRef & ctestMsk)) discard;
    #elif CTEST_FUNC == 3 && USE_BIT_OPERATORS
        ivec3 Cs = ivec3((Cf * 255.0));
        if ((Cs & ctestMsk) == (ctestRef & ctestMsk)) discard;
    #endif
}
#endif


///////////////////////////////////////////////////////////////
// Depth test (disabled)
///////////////////////////////////////////////////////////////


// Convert the depth value from float to int and apply the depth mask
#if 0
int getDepthInt(float depth)
{
    #if USE_BIT_OPERATORS
        return int(ROUND(depth * 255.0)) & depthMask;
    #else
        return depthMask == 0 ? 0 : int(ROUND(depth * 255.0));
    #endif
}


bool passDepthTest(float depth)
{
    #if !USE_DYNAMIC_DEFINES
        switch (depthFunc)
        {
        case 0: // ZTST_FUNCTION_NEVER_PASS_PIXEL
            return false;
        case 1: // ZTST_FUNCTION_ALWAYS_PASS_PIXEL
            return true;
        case 2: // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_EQUAL
            return getDepthInt(depth) == gl_FragCoord.z;
        case 3: // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_ISNOT_EQUAL
            return getDepthInt(depth) != gl_FragCoord.z;
        case 4: // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS
            return getDepthInt(depth) < gl_FragCoord.z;
        case 5: // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS_OR_EQUAL
            return getDepthInt(depth) <= gl_FragCoord.z;
        case 6: // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER
            return getDepthInt(depth) > gl_FragCoord.z;
        case 7: // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER_OR_EQUAL
            return getDepthInt(depth) >= gl_FragCoord.z;
        }

        return true;
    #elif DEPTH_FUNC == 0
        // ZTST_FUNCTION_NEVER_PASS_PIXEL
        return false;
    #elif DEPTH_FUNC == 1
        // ZTST_FUNCTION_ALWAYS_PASS_PIXEL
        return true;
    #elif DEPTH_FUNC == 2
        // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_EQUAL
        return getDepthInt(depth) == gl_FragCoord.z;
    #elif DEPTH_FUNC == 3
        // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_ISNOT_EQUAL
        return getDepthInt(depth) != gl_FragCoord.z;
    #elif DEPTH_FUNC == 4
        // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS
        return getDepthInt(depth) < gl_FragCoord.z;
    #elif DEPTH_FUNC == 5
        // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS_OR_EQUAL
        return getDepthInt(depth) <= gl_FragCoord.z;
    #elif DEPTH_FUNC == 6
        // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER
        return getDepthInt(depth) > gl_FragCoord.z;
    #elif DEPTH_FUNC == 7
        // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER_OR_EQUAL
        return getDepthInt(depth) >= gl_FragCoord.z;
    #else
        return true;
    #endif
}
#endif

///////////////////////////////////////////////////////////////
// Stencil Test
///////////////////////////////////////////////////////////////


// Apply the selected stencil operation to the fragment color Cf
void applyStencilOp(inout vec4 Cf, float fbAlpha, int stencilOp)
{
	switch (stencilOp)
	{
	case 0: // SOP_KEEP_STENCIL_VALUE
		// Keep the previous Frame Buffer alpha value
		Cf.a = fbAlpha;
		break;
	case 1: // SOP_ZERO_STENCIL_VALUE
		// Set the alpha value to zero
		Cf.a = 0.0;
		break;
	case 2: // SOP_REPLACE_STENCIL_VALUE
		// Replace the alpha value by the reference value
		Cf.a = float(stencilRef) / 255.0;
		break;
	case 3: // SOP_INVERT_STENCIL_VALUE
		// This is equivalent to a bit-invert (~) operation.
		Cf.a = 1.0 - fbAlpha;
		break;
	case 4: // SOP_INCREMENT_STENCIL_VALUE
		// Increment the 8-bit int value by 1 up to 255
		Cf.a = min(fbAlpha + 1.0/256.0, 1.0);
		break;
	case 5: // SOP_DECREMENT_STENCIL_VALUE
		// Drecement the 8-bit int value by 1 down to 0
		Cf.a = max(fbAlpha - 1.0/256.0, 0.0);
		break;
	}
}


// Convert the alpha value from float to int and apply the stencil mask
int getStencilFbAlphaInt(float fbAlpha)
{
    #if USE_BIT_OPERATORS
        return int(ROUND(fbAlpha * 255.0)) & stencilMask;
    #else
        // Masking with stencilMask is not available when not using bit operators...
        return int(ROUND(fbAlpha * 255.0));
    #endif
}


bool passStencilTest(float fbAlpha)
{
    #if !USE_DYNAMIC_DEFINES
        switch (stencilFunc)
        {
        case 0: // STST_FUNCTION_NEVER_PASS_STENCIL_TEST
            return false;
        case 1: // STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST
            return true;
        case 2: // STST_FUNCTION_PASS_TEST_IF_MATCHES
            return stencilRef == getStencilFbAlphaInt(fbAlpha);
        case 3: // STST_FUNCTION_PASS_TEST_IF_DIFFERS
            return stencilRef != getStencilFbAlphaInt(fbAlpha);
        case 4: // STST_FUNCTION_PASS_TEST_IF_LESS
            return stencilRef < getStencilFbAlphaInt(fbAlpha);
        case 5: // STST_FUNCTION_PASS_TEST_IF_LESS_OR_EQUAL
            return stencilRef <= getStencilFbAlphaInt(fbAlpha);
        case 6: // STST_FUNCTION_PASS_TEST_IF_GREATER
            return stencilRef > getStencilFbAlphaInt(fbAlpha);
        case 7: // STST_FUNCTION_PASS_TEST_IF_GREATER_OR_EQUAL
            return stencilRef >= getStencilFbAlphaInt(fbAlpha);
        }

        return true;
    #elif STENCIL_FUNC == 0
        // STST_FUNCTION_NEVER_PASS_STENCIL_TEST
        return false;
    #elif STENCIL_FUNC == 1
        // STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST
        return true;
    #elif STENCIL_FUNC == 2
        // STST_FUNCTION_PASS_TEST_IF_MATCHES
        return stencilRef == getStencilFbAlphaInt(fbAlpha);
    #elif STENCIL_FUNC == 3
        // STST_FUNCTION_PASS_TEST_IF_DIFFERS
        return stencilRef != getStencilFbAlphaInt(fbAlpha);
    #elif STENCIL_FUNC == 4
        // STST_FUNCTION_PASS_TEST_IF_LESS
        return stencilRef < getStencilFbAlphaInt(fbAlpha);
    #elif STENCIL_FUNC == 5
        // STST_FUNCTION_PASS_TEST_IF_LESS_OR_EQUAL
        return stencilRef <= getStencilFbAlphaInt(fbAlpha);
    #elif STENCIL_FUNC == 6
        // STST_FUNCTION_PASS_TEST_IF_GREATER
        return stencilRef > getStencilFbAlphaInt(fbAlpha);
    #elif STENCIL_FUNC == 7
        // STST_FUNCTION_PASS_TEST_IF_GREATER_OR_EQUAL
        return stencilRef >= getStencilFbAlphaInt(fbAlpha);
    #else
        return true;
    #endif
}


// Apply the stencil test function to the fragment color Cf
bool ApplyStencilTest(inout vec4 Cf, in vec4 Cdst)
{
    int stencilOp;
    bool stencilTestPassed;

    if (passStencilTest(Cdst.a))
    {
        // The stencil did pass, the RGB will be updated
        // and update the Alpha according to the stencil operation.
        // FAKING: Always pass the Z test, for now.
        stencilOp = stencilOpZPass;
        stencilTestPassed = true;
    }
    else
    {
        // The stencil test did not pass, keep the RGB unchanged
        // and update the Alpha according to the stencil operation.
        Cf.rgb = Cdst.rgb;
        stencilOp = stencilOpFail;
        stencilTestPassed = false;
    }

    // Update the Alpha according to the stencil operation
    applyStencilOp(Cf, Cdst.a, stencilOp);

    return stencilTestPassed;
}


///////////////////////////////////////////////////////////////
// Color masking
///////////////////////////////////////////////////////////////

// The Color masking function is mixing the color
// from the already rendered buffer with the current fragment color.
void ApplyColorMask(inout vec4 Cf, in vec4 Cdst)
{
    #if USE_BIT_OPERATORS
        // The current fragment color in integer format
        ivec4 Cs = ivec4(ROUND(Cf * 255.0));
        // The current FrameBuffer pixel color
        ivec4 Cd = ivec4(ROUND(Cdst * 255.0));

        // ATI driver has problems to compute "~colorMask", so use a pre-computed
        // uniform "notColorMask"
        Cf = ((Cd & colorMask) | (Cs & notColorMask)) / 255.0;
    #else
        // Color mask not available when not using bit operators...
    #endif
}


///////////////////////////////////////////////////////////////
// Alpha Test
///////////////////////////////////////////////////////////////

void ApplyAlphaTest(inout vec4 Cf)
{
    #if !USE_DYNAMIC_DEFINES
        // Convert Cf.a to an integer value before testing
        // in order to avoid rouding errors when testing for equality.
        int alphaTest;

        switch (alphaTestFunc)
        {
        case 0: // ATST_NEVER_PASS_PIXEL
            discard;
        case 1: // ATST_ALWAYS_PASS_PIXEL
            // Nothing to do
            break;
        case 2: // ATST_PASS_PIXEL_IF_MATCHES
            alphaTest = int(ROUND(Cf.a * 255.0));
            if (alphaTest != alphaTestRef) discard;
            break;
        case 3: // ATST_PASS_PIXEL_IF_DIFFERS
            alphaTest = int(ROUND(Cf.a * 255.0));
            if (alphaTest == alphaTestRef) discard;
            break;
        case 4: // ATST_PASS_PIXEL_IF_LESS
            alphaTest = int(ROUND(Cf.a * 255.0));
            if (alphaTest >= alphaTestRef) discard;
            break;
        case 5: // ATST_PASS_PIXEL_IF_LESS_OR_EQUAL
            alphaTest = int(ROUND(Cf.a * 255.0));
            if (alphaTest > alphaTestRef) discard;
            break;
        case 6: // ATST_PASS_PIXEL_IF_GREATER
            alphaTest = int(ROUND(Cf.a * 255.0));
            if (alphaTest <= alphaTestRef) discard;
            break;
        case 7: // ATST_PASS_PIXEL_IF_GREATER_OR_EQUAL
            alphaTest = int(ROUND(Cf.a * 255.0));
            if (alphaTest < alphaTestRef) discard;
            break;
        }
    #elif ALPHA_TEST_FUNC == 0
        // ATST_NEVER_PASS_PIXEL
        discard;
    #elif ALPHA_TEST_FUNC == 1
        // ATST_ALWAYS_PASS_PIXEL
        // Nothing to do
    #elif ALPHA_TEST_FUNC == 2
        // ATST_PASS_PIXEL_IF_MATCHES
        int alphaTest = int(ROUND(Cf.a * 255.0));
        if (alphaTest != alphaTestRef) discard;
    #elif ALPHA_TEST_FUNC == 3
        // ATST_PASS_PIXEL_IF_DIFFERS
        int alphaTest = int(ROUND(Cf.a * 255.0));
        if (alphaTest == alphaTestRef) discard;
    #elif ALPHA_TEST_FUNC == 4
        // ATST_PASS_PIXEL_IF_LESS
        int alphaTest = int(ROUND(Cf.a * 255.0));
        if (alphaTest >= alphaTestRef) discard;
    #elif ALPHA_TEST_FUNC == 5
        // ATST_PASS_PIXEL_IF_LESS_OR_EQUAL
        int alphaTest = int(ROUND(Cf.a * 255.0));
        if (alphaTest > alphaTestRef) discard;
    #elif ALPHA_TEST_FUNC == 6
        // ATST_PASS_PIXEL_IF_GREATER
        int alphaTest = int(ROUND(Cf.a * 255.0));
        if (alphaTest <= alphaTestRef) discard;
    #elif ALPHA_TEST_FUNC == 7
        // ATST_PASS_PIXEL_IF_GREATER_OR_EQUAL
        int alphaTest = int(ROUND(Cf.a * 255.0));
        if (alphaTest < alphaTestRef) discard;
    #endif
}


///////////////////////////////////////////////////////////////
// Blend Test
///////////////////////////////////////////////////////////////

vec3 getBlendParameter(int parameter, in vec3 color, float srcAlpha, float dstAlpha, in vec3 fix)
{
    switch (parameter)
    {
    case 0: // ALPHA_SOURCE_COLOR / ALPHA_DESTINATION_COLOR
        return color;
    case 1: // ALPHA_ONE_MINUS_SOURCE_COLOR / ALPHA_ONE_MINUS_DESTINATION_COLOR
        return vec3(1.0 - color);
    case 2: // ALPHA_SOURCE_ALPHA
        return vec3(srcAlpha);
    case 3: // ALPHA_ONE_MINUS_SOURCE_ALPHA
        return vec3(1.0 - srcAlpha);
    case 4: // ALPHA_DESTINATION_ALPHA
        return vec3(dstAlpha);
    case 5: // ALPHA_ONE_MINUS_DESTINATION_ALPHA
        return vec3(1.0 - dstAlpha);
    case 6: // ALPHA_DOUBLE_SOURCE_ALPHA
        return vec3(2.0 * srcAlpha);
    case 7: // ALPHA_ONE_MINUS_DOUBLE_SOURCE_ALPHA
        return vec3(1.0 - 2.0 * srcAlpha);
    case 8: // ALPHA_DOUBLE_DESTINATION_ALPHA
        return vec3(2.0 * dstAlpha);
    case 9: // ALPHA_ONE_MINUS_DOUBLE_DESTINATION_ALPHA
        return vec3(1.0 - 2.0 * dstAlpha);
    case 10: // ALPHA_FIX
        return fix;
    }

    return color;
}

void ApplyBlendTest(inout vec4 Cf, in vec4 Csrc, in vec4 Cdst)
{
    #if !USE_DYNAMIC_DEFINES || BLEND_EQUATION <= 2
        vec3 CPsrc = clamp(Csrc.rgb * getBlendParameter(blendSrc, Cdst.rgb, Csrc.a, Cdst.a, blendSFix), 0.0, 1.0);
        vec3 CPdst = clamp(Cdst.rgb * getBlendParameter(blendDst, Csrc.rgb, Csrc.a, Cdst.a, blendDFix), 0.0, 1.0);
    #endif

    #if !USE_DYNAMIC_DEFINES
        switch (blendEquation)
        {
        case 0: // ALPHA_SOURCE_BLEND_OPERATION_ADD
            Cf.rgb = CPsrc + CPdst;
            break;
        case 1: // ALPHA_SOURCE_BLEND_OPERATION_SUBTRACT
            Cf.rgb = CPsrc - CPdst;
            break;
        case 2: // ALPHA_SOURCE_BLEND_OPERATION_REVERSE_SUBTRACT
            Cf.rgb = CPdst - CPsrc;
            break;
        case 3: // ALPHA_SOURCE_BLEND_OPERATION_MINIMUM_VALUE
            Cf.rgb = min(Csrc.rgb, Cdst.rgb);
            break;
        case 4: // ALPHA_SOURCE_BLEND_OPERATION_MAXIMUM_VALUE
            Cf.rgb = max(Csrc.rgb, Cdst.rgb);
            break;
        case 5: // ALPHA_SOURCE_BLEND_OPERATION_ABSOLUTE_VALUE
            Cf.rgb = abs(Csrc.rgb - Cdst.rgb);
            break;
        }
    #elif BLEND_EQUATION == 0
        // ALPHA_SOURCE_BLEND_OPERATION_ADD
        Cf.rgb = CPsrc + CPdst;
    #elif BLEND_EQUATION == 1
        // ALPHA_SOURCE_BLEND_OPERATION_SUBTRACT
        Cf.rgb = CPsrc - CPdst;
    #elif BLEND_EQUATION == 2
        // ALPHA_SOURCE_BLEND_OPERATION_REVERSE_SUBTRACT
        Cf.rgb = CPdst - CPsrc;
    #elif BLEND_EQUATION == 3
        // ALPHA_SOURCE_BLEND_OPERATION_MINIMUM_VALUE
        Cf.rgb = min(Csrc.rgb, Cdst.rgb);
    #elif BLEND_EQUATION == 4
        // ALPHA_SOURCE_BLEND_OPERATION_MAXIMUM_VALUE
        Cf.rgb = max(Csrc.rgb, Cdst.rgb);
    #elif BLEND_EQUATION == 5
        // ALPHA_SOURCE_BLEND_OPERATION_ABSOLUTE_VALUE
        Cf.rgb = abs(Csrc.rgb - Cdst.rgb);
    #endif
}


///////////////////////////////////////////////////////////////
// Main program
///////////////////////////////////////////////////////////////

void main()
{
    vec4 Cf = getFragColor();
    #if !USE_DYNAMIC_DEFINES || BLEND_TEST_ENABLE
        vec4 Csrc = Cf;
    #endif

    #if !USE_DYNAMIC_DEFINES
        if (ctestEnable) ApplyColorTest(Cf.rgb);
    #elif CTEST_ENABLE
        ApplyColorTest(Cf.rgb);
    #endif

    #if !USE_DYNAMIC_DEFINES
        vec4 Cdst = vec4(0.0);
        // texelFetch is only available in v1.30 or later
        #if __VERSION__ >= 130
            if (stencilTestEnable || blendTestEnable || colorMaskEnable)
            {
                // Retrieve the current FrameBuffer pixel color.
                // As a performance improvement, this is only done when this color
                // is used in later tests.
                // i.e. when stencil test, blend test or color mask are enabled
                Cdst = texelFetch(fbTex, getFragCoord(), 0);
            }
        #endif
    #elif STENCIL_TEST_ENABLE || BLEND_TEST_ENABLE || COLOR_MASK_ENABLE
        vec4 Cdst = texelFetch(fbTex, getFragCoord(), 0);
    #endif

    #if !USE_DYNAMIC_DEFINES
        if (alphaTestEnable) ApplyAlphaTest(Cf);
    #elif ALPHA_TEST_ENABLE
        ApplyAlphaTest(Cf);
    #endif

    bool stencilTestPassed = true;
    #if !USE_DYNAMIC_DEFINES
        if (stencilTestEnable) stencilTestPassed = ApplyStencilTest(Cf, Cdst);
    #elif STENCIL_TEST_ENABLE
        stencilTestPassed = ApplyStencilTest(Cf, Cdst);
    #endif

    if (stencilTestPassed)
    {
        #if !USE_DYNAMIC_DEFINES
            if (blendTestEnable) ApplyBlendTest(Cf, Csrc, Cdst);
        #elif BLEND_TEST_ENABLE
            ApplyBlendTest(Cf, Csrc, Cdst);
        #endif

        #if !USE_DYNAMIC_DEFINES
            if (colorMaskEnable) ApplyColorMask(Cf, Cdst);
        #elif COLOR_MASK_ENABLE
            ApplyColorMask(Cf, Cdst);
        #endif
    }

    gl_FragColor = Cf;
}
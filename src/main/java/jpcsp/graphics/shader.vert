// INSERT VERSION

// INSERT DEFINES

#define AVOID_NVIDIA_BUG 1

#if USE_BIT_OPERATORS
    #extension GL_EXT_gpu_shader4 : enable
#endif
#if USE_UBO
    #extension GL_ARB_uniform_buffer_object : enable
#endif
#if __VERSION__ >= 130 && __VERSION__ < 150 && defined(GL_ARB_compatibility)
    #extension GL_ARB_compatibility : enable
#endif
#if __VERSION__ < 410 && defined(GL_ARB_separate_shader_objects)
    #extension GL_ARB_separate_shader_objects : enable
#endif
#if __VERSION__ >= 410 || defined(GL_ARB_separate_shader_objects)
	#define LOCATION(N) layout(location=N)
#else
	#define LOCATION(N)
#endif

// Use attributes instead of gl_Vertex, gl_Normal...: attributes support all the
// data types used by the PSP (signed/unsigned, bytes/shorts/floats).
LOCATION(0) in vec4 pspPosition;
LOCATION(1) in vec4 pspTexture;
LOCATION(2) in vec4 pspColor;
LOCATION(3) in vec3 pspNormal;
LOCATION(4) in vec4 pspWeights1;
LOCATION(5) in vec4 pspWeights2;

// The output locations must match those defined as input in the geometry and fragment shaders
LOCATION(0) out vec3 texCoord;
LOCATION(1) noperspective out float discarded;
LOCATION(2) out float fogDepth;
#if !USE_DYNAMIC_DEFINES
	// When not using dynamic defines, we need to generate the primary and secondary
	// colors in both the smooth and flat variants as the use of one or the other
	// will be decided dynamically in the fragment shader based on the value of the shadeModel.
	LOCATION(3) smooth out vec4 pspPrimaryColorSmooth;
	LOCATION(4) smooth out vec4 pspSecondaryColorSmooth;
	LOCATION(5) flat out vec4 pspPrimaryColorFlat;
	LOCATION(6) flat out vec4 pspSecondaryColorFlat;
#elif SHADE_MODEL == 0
	LOCATION(3) flat out vec4 pspPrimaryColor;
	LOCATION(4) flat out vec4 pspSecondaryColor;
#else
	LOCATION(3) smooth out vec4 pspPrimaryColor;
	LOCATION(4) smooth out vec4 pspSecondaryColor;
#endif


#if USE_UBO
	UBO_STRUCTURE
#else
    uniform mat4  modelMatrix;
    uniform mat4  modelViewProjectionMatrix;
    uniform mat4  pspBoneMatrix[8];
    uniform int   pspNumberBones;
    uniform float weightScale;
    uniform int   vinfoColor;
    uniform int   vinfoPosition;
    uniform int   vinfoTexture;
    uniform int   vinfoNormal;
    uniform float positionScale;
    uniform float textureScale;
    uniform float normalScale;
    uniform vec4  vertexColor;
    uniform bool  vinfoTransform2D;
#endif

///////////////////////////////////////////////////////////////
// Common functions
// (shared with the Tessellation Evaluation shader)
///////////////////////////////////////////////////////////////
#if !HAS_TESSELLATION_SHADER
	#include "/jpcsp/graphics/shader-common.vert"
#endif


///////////////////////////////////////////////////////////////
// Skinning
///////////////////////////////////////////////////////////////

#if !USE_DYNAMIC_DEFINES || NUMBER_BONES > 0
void ApplySkinning(inout vec3 Vv, inout vec3 Nv)
{
    vec3  V = vec3(0.0, 0.0, 0.0);
    vec3  N = V;
    float W = 0.0;
    mat3  M;
    vec4  W1 = pspWeights1 / weightScale;
    #if !USE_DYNAMIC_DEFINES || NUMBER_BONES > 4
        vec4  W2 = pspWeights2 / weightScale;
    #endif

    #if USE_DYNAMIC_DEFINES
        #if NUMBER_BONES >= 8
            W = W2[3]; M = mat3(pspBoneMatrix[7]); V += (M * Vv + pspBoneMatrix[7][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 7
            W = W2[2]; M = mat3(pspBoneMatrix[6]); V += (M * Vv + pspBoneMatrix[6][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 6
            W = W2[1]; M = mat3(pspBoneMatrix[5]); V += (M * Vv + pspBoneMatrix[5][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 5
            W = W2[0]; M = mat3(pspBoneMatrix[4]); V += (M * Vv + pspBoneMatrix[4][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 4
            W = W1[3]; M = mat3(pspBoneMatrix[3]); V += (M * Vv + pspBoneMatrix[3][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 3
            W = W1[2]; M = mat3(pspBoneMatrix[2]); V += (M * Vv + pspBoneMatrix[2][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 2
            W = W1[1]; M = mat3(pspBoneMatrix[1]); V += (M * Vv + pspBoneMatrix[1][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 1
            W = W1[0]; M = mat3(pspBoneMatrix[0]); V += (M * Vv + pspBoneMatrix[0][3].xyz) * W; N += M * Nv * W;
        #endif
    #else
        switch (pspNumberBones)
        {
        case 8: W = W2[3]; M = mat3(pspBoneMatrix[7]); V += (M * Vv + pspBoneMatrix[7][3].xyz) * W; N += M * Nv * W; // fallthrough
        case 7: W = W2[2]; M = mat3(pspBoneMatrix[6]); V += (M * Vv + pspBoneMatrix[6][3].xyz) * W; N += M * Nv * W; // fallthrough
        case 6: W = W2[1]; M = mat3(pspBoneMatrix[5]); V += (M * Vv + pspBoneMatrix[5][3].xyz) * W; N += M * Nv * W; // fallthrough
        case 5: W = W2[0]; M = mat3(pspBoneMatrix[4]); V += (M * Vv + pspBoneMatrix[4][3].xyz) * W; N += M * Nv * W; // fallthrough
        case 4: W = W1[3]; M = mat3(pspBoneMatrix[3]); V += (M * Vv + pspBoneMatrix[3][3].xyz) * W; N += M * Nv * W; // fallthrough
        case 3: W = W1[2]; M = mat3(pspBoneMatrix[2]); V += (M * Vv + pspBoneMatrix[2][3].xyz) * W; N += M * Nv * W; // fallthrough
        case 2: W = W1[1]; M = mat3(pspBoneMatrix[1]); V += (M * Vv + pspBoneMatrix[1][3].xyz) * W; N += M * Nv * W; // fallthrough
        case 1: W = W1[0]; M = mat3(pspBoneMatrix[0]); V += (M * Vv + pspBoneMatrix[0][3].xyz) * W; N += M * Nv * W;
        }
    #endif
    Vv = V;
    Nv = N;
}
#endif


///////////////////////////////////////////////////////////////
// Position decoding
///////////////////////////////////////////////////////////////

#if !USE_DYNAMIC_DEFINES || (VINFO_TRANSFORM_2D && VINFO_POSITION == 1)
// GU_VERTEX_8BIT in 2D
void DecodePosition2D_1(inout vec3 V)
{
    // V.z is unsigned 8 bit in 2D
    if (V.z < 0.0) V.z += 256.0;
}
#endif


#if !USE_DYNAMIC_DEFINES || (VINFO_TRANSFORM_2D && VINFO_POSITION == 2)
// GU_VERTEX_16BIT in 2D
void DecodePosition2D_2(inout vec3 V)
{
    // V.z is unsigned 16 bit in 2D
    if (V.z < 0.0) V.z += 65536.0;
}
#endif


#if !USE_DYNAMIC_DEFINES || (VINFO_TRANSFORM_2D && VINFO_POSITION == 3)
// GU_VERTEX_32BITF in 2D
void DecodePosition2D_3(inout vec3 V)
{
	// Z is an integer value clamped between 0 and 65535
	V.z = float(int(clamp(V.z, 0.0, 65535.0)));
}
#endif


#if !USE_DYNAMIC_DEFINES || !VINFO_TRANSFORM_2D
void DecodePosition3D(inout vec3 V)
{
    V /= positionScale;
}
#endif


#if !USE_DYNAMIC_DEFINES
void DecodePosition(inout vec3 V)
{
    // V.z is unsigned in 2D
    if (vinfoTransform2D)
    {
        switch (vinfoPosition)
        {
        case 1: DecodePosition2D_1(V); break;
        case 2: DecodePosition2D_2(V); break;
        case 3: DecodePosition2D_3(V); break;
        }
    }
    else
    {
        DecodePosition3D(V);
    }
}
#endif


///////////////////////////////////////////////////////////////
// Color decoding
///////////////////////////////////////////////////////////////

#if !USE_DYNAMIC_DEFINES || VINFO_COLOR == 4
// GU_COLOR_5650
void DecodeColor5650(inout vec4 C)
{
    int packedBits = int(C.x);
    #if USE_BIT_OPERATORS
        int rBits = (packedBits      ) & 0x1F;
        int gBits = (packedBits >>  5) & 0x3F;
        int bBits = (packedBits >> 11) & 0x1F;
        C.r = float((rBits << 3) | (rBits >> 2));
        C.g = float((gBits << 2) | (gBits >> 4));
        C.b = float((bBits << 3) | (bBits >> 2));
    #else
        int rBits = packedBits - (packedBits / 32 * 32);
        int gBits = (packedBits / 32) - (packedBits / 32 / 64 * 64);
        int bBits = packedBits / 2048;
        C.r = float((rBits * 8) + (rBits /  4));
        C.g = float((gBits * 4) + (gBits / 16));
        C.b = float((bBits * 8) + (bBits /  4));
    #endif
    C.a = 1.0;
    C.rgb /= 255.0;
}
#endif


#if !USE_DYNAMIC_DEFINES || VINFO_COLOR == 5
// GU_COLOR_5551
void DecodeColor5551(inout vec4 C)
{
    int packedBits = int(C.x);
    #if USE_BIT_OPERATORS
        int rBits = (packedBits      ) & 0x1F;
        int gBits = (packedBits >>  5) & 0x1F;
        int bBits = (packedBits >> 10) & 0x1F;
        C.r = float((rBits << 3) | (rBits >> 2));
        C.g = float((gBits << 3) | (gBits >> 2));
        C.b = float((bBits << 3) | (bBits >> 2));
        C.a = float((packedBits >> 15) & 0x01);
    #else
        int rBits = packedBits - (packedBits / 32 * 32);
        int gBits = (packedBits / 32) - (packedBits / 32 / 32 * 32);
        int bBits = (packedBits / 1024) - (packedBits / 1024 / 32 * 32);
        C.r = float((rBits * 8) + (rBits / 4));
        C.g = float((gBits * 8) + (gBits / 4));
        C.b = float((bBits * 8) + (bBits / 4));
        C.a = float(packedBits / 32768);
    #endif
    C.rgb /= 255.0;
}
#endif


#if !USE_DYNAMIC_DEFINES || VINFO_COLOR == 6
// GU_COLOR_4444
void DecodeColor4444(inout vec4 C)
{
    int packedBits = int(C.x);
    #if USE_BIT_OPERATORS
        int rBits = (packedBits      ) & 0x0F;
        int gBits = (packedBits >>  4) & 0x0F;
        int bBits = (packedBits >>  8) & 0x0F;
        int aBits = (packedBits >> 12) & 0x0F;
        C.r = float((rBits << 4) | rBits);
        C.g = float((gBits << 4) | gBits);
        C.b = float((bBits << 4) | bBits);
        C.a = float((aBits << 4) | aBits);
    #else
        int rBits = packedBits - (packedBits / 16 * 16);
        int gBits = (packedBits / 16) - (packedBits / 16 / 16 * 16);
        int bBits = (packedBits / 256) - (packedBits / 256 / 16 * 16);
        int aBits = packedBits / 4096;
        C.r = float((rBits * 16) + rBits);
        C.g = float((gBits * 16) + gBits);
        C.b = float((bBits * 16) + bBits);
        C.a = float((aBits * 16) + aBits);
    #endif
    C /= 255.0;
}
#endif


#if !USE_DYNAMIC_DEFINES || VINFO_COLOR == 7
// GU_COLOR_8888
void DecodeColor8888(inout vec4 C)
{
    C /= 255.0;
}
#endif

#if !USE_DYNAMIC_DEFINES || VINFO_COLOR == 8
// !useVertexColor
void DecodeNoVertexColor(out vec4 C)
{
    C = vertexColor;
}
#endif


#if !USE_DYNAMIC_DEFINES
void DecodeColor(inout vec4 C)
{
    switch (vinfoColor)
    {
        case 4: DecodeColor5650(C); break;
        case 5: DecodeColor5551(C); break;
        case 6: DecodeColor4444(C); break;
        case 7: DecodeColor8888(C); break;
        case 8: DecodeNoVertexColor(C); break;
    }
}
#endif


///////////////////////////////////////////////////////////////
// Main program
///////////////////////////////////////////////////////////////

void main()
{
    #if !USE_DYNAMIC_DEFINES
        vec3 N  = (vinfoNormal != 0 ? pspNormal : vec3(1.0, 0.0, 0.0));
    #elif VINFO_NORMAL != 0
        vec3 N = pspNormal;
    #else
        vec3 N = vec3(1.0, 0.0, 0.0);
    #endif
    #if !USE_DYNAMIC_DEFINES
        vec4 V  = (vinfoPosition != 0 ? pspPosition : vec4(0.0));
    #elif VINFO_POSITION != 0
        vec4 V = pspPosition;
    #else
        vec4 V = vec4(0.0);
    #endif
    #if !USE_DYNAMIC_DEFINES
        vec4 T  = (vinfoTexture != 0 ? pspTexture : vec4(0.0));
    #elif VINFO_TEXTURE != 0
        vec4 T = pspTexture;
    #else
        vec4 T = vec4(0.0);
    #endif
    #if !USE_DYNAMIC_DEFINES
        vec4 Cp = (vinfoColor != 8 ? pspColor : vec4(0.0));
    #elif VINFO_COLOR != 8
        vec4 Cp = pspColor;
    #else
        vec4 Cp = vec4(0.0);
    #endif
    vec4 Cs = vec4(0.0);

    #if !USE_DYNAMIC_DEFINES
        DecodeColor(Cp);
    #elif VINFO_COLOR == 4
        DecodeColor5650(Cp);
    #elif VINFO_COLOR == 5
        DecodeColor5551(Cp);
    #elif VINFO_COLOR == 6
        DecodeColor4444(Cp);
    #elif VINFO_COLOR == 7
        DecodeColor8888(Cp);
    #elif VINFO_COLOR == 8
        DecodeNoVertexColor(Cp);
    #endif

    #if !USE_DYNAMIC_DEFINES
        DecodePosition(V.xyz);
    #elif VINFO_TRANSFORM_2D && VINFO_POSITION == 1
        DecodePosition2D_1(V.xyz);
    #elif VINFO_TRANSFORM_2D && VINFO_POSITION == 2
        DecodePosition2D_2(V.xyz);
    #elif VINFO_TRANSFORM_2D && VINFO_POSITION == 3
        DecodePosition2D_3(V.xyz);
    #elif !VINFO_TRANSFORM_2D
        DecodePosition3D(V.xyz);
    #endif

    #if !USE_DYNAMIC_DEFINES || VINFO_NORMAL != 0
        N /= normalScale;
    #endif

    #if !USE_DYNAMIC_DEFINES
        if (pspNumberBones > 0) ApplySkinning(V.xyz, N);
    #elif NUMBER_BONES > 0
        ApplySkinning(V.xyz, N);
    #endif

	// Lighting and texture mapping are done in the
	// Tessellation Evaluation Shader, if available
	#if !HAS_TESSELLATION_SHADER
		vec3 Ve = vec3(modelViewMatrix * V);

		#if !USE_DYNAMIC_DEFINES || VINFO_NORMAL != 0
			vec3 Ne = vec3(modelMatrix * vec4(N, 0.0));
		#else
			vec3 Ne = vec3(1.0, 0.0, 0.0);
		#endif

		#if !USE_DYNAMIC_DEFINES
			if (lightingEnable) ApplyLighting(Cp, Cs, V.xyz, normalize(Ne));
		#elif LIGHTING_ENABLE
			ApplyLighting(Cp, Cs, V.xyz, normalize(Ne));
		#endif

		#if !USE_DYNAMIC_DEFINES
			if (texEnable) ApplyTexture(T, V, N, Ne);
		#elif TEX_ENABLE
			ApplyTexture(T, V, N, Ne);
		#endif

		gl_Position             = modelViewProjectionMatrix * V;
		fogDepth                = (fogEnd + Ve.z) * fogScale;
	    discarded               = getDiscarded(gl_Position);
	#else
		// The tessellation has to happen in the model coordinates,
		// specially to compute the normal
		gl_Position             = V;
    #endif

    texCoord                    = T.xyz;
    #if !USE_DYNAMIC_DEFINES
		pspPrimaryColorFlat     = Cp;
		pspSecondaryColorFlat   = Cs;
		pspPrimaryColorSmooth   = Cp;
		pspSecondaryColorSmooth = Cs;
	#else
		pspPrimaryColor         = Cp;
		pspSecondaryColor       = Cs;
	#endif
}

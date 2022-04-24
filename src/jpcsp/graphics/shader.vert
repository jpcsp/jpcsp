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
#if __VERSION__ >= 410
	#define LOCATION(N) layout(location=N)
#else
	#define LOCATION(N)
#endif

// Use attributes instead of gl_Vertex, gl_Normal...: attributes support all the
// data types used by the PSP (signed/unsigned, bytes/shorts/floats).
in vec4 pspTexture;
in vec4 pspColor;
in vec3 pspNormal;
in vec4 pspPosition;
in vec4 pspWeights1;
in vec4 pspWeights2;

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
    uniform ivec3 pspMatFlags; // Ambient, Diffuse, Specular
    uniform ivec4 pspLightType;
    uniform ivec4 pspLightKind;
    uniform ivec4 pspLightEnabled;
    uniform vec3  pspLightPosition[NUM_LIGHTS];
    uniform vec3  pspLightDirection[NUM_LIGHTS];
    uniform vec3  pspLightAmbientColor[NUM_LIGHTS];
    uniform vec3  pspLightDiffuseColor[NUM_LIGHTS];
    uniform vec3  pspLightSpecularColor[NUM_LIGHTS];
    uniform float pspLightSpotLightExponent[NUM_LIGHTS];
    uniform float pspLightSpotLightCutoff[NUM_LIGHTS];
    uniform vec3  pspLightAttenuation[NUM_LIGHTS];
    uniform mat4  pspBoneMatrix[8];
    uniform int   pspNumberBones;
    uniform bool  texEnable;
    uniform int   texMapMode;
    uniform int   texMapProj;
    uniform ivec2 texShade;
    uniform bool  lightingEnable;
    uniform bool  colorAddition;
    uniform int   vinfoColor;
    uniform int   vinfoPosition;
    uniform int   vinfoTexture;
    uniform int   vinfoNormal;
    uniform bool  vinfoTransform2D;
    uniform float positionScale;
    uniform float normalScale;
    uniform float textureScale;
    uniform float weightScale;
    uniform vec4  vertexColor;
    uniform bool  clipPlaneEnable;
    uniform vec3  viewportPos;
    uniform vec3  viewportScale;
    uniform vec4  ambientLightColor;
    uniform float materialShininess;
    uniform vec4  materialAmbientColor;
    uniform vec3  materialDiffuseColor;
    uniform vec3  materialSpecularColor;
    uniform vec3  materialEmissionColor;
    uniform mat4  pspTextureMatrix;
    uniform mat4  modelViewMatrix;
    uniform mat4  modelViewProjectionMatrix;
    uniform mat3  normalMatrix;
    uniform float fogEnd;
    uniform float fogScale;
#endif


///////////////////////////////////////////////////////////////
// Lighting
///////////////////////////////////////////////////////////////

#if !USE_DYNAMIC_DEFINES || LIGHTING_ENABLE || TEX_MAP_MODE == 2
vec3 getLightVector(in int i, in vec3 V)
{
	return (pspLightType[i] != 0) ? pspLightPosition[i] - V : pspLightPosition[i];
}
#endif

#if !USE_DYNAMIC_DEFINES || LIGHTING_ENABLE
void ComputeLight(in int i, in vec3 N, in vec3 V, inout vec3 A, inout vec3 D, inout vec3 S)
{
	vec3  L     = getLightVector(i, V);
    vec3  H     = L + vec3(0.0, 0.0, 1.0);
    float att   = 1.0;
    float NdotL = max(dot(normalize(L), N), 0.0);
    float NdotH = max(dot(normalize(H), N), 0.0);
    float k     = materialShininess;
    float Dk    = (pspLightKind[i] == 2) ? max(pow(NdotL, k), 0.0) : NdotL;
    float Sk    = (pspLightKind[i] != 0) ? max(pow(NdotH, k), 0.0) : 0.0;

    if (pspLightType[i] != 0) // Not directional type
    {
        float d = length(L);
        att = clamp(1.0 / dot(pspLightAttenuation[i], vec3(1.0, d, d * d)), 0.0, 1.0);
        if (pspLightSpotLightCutoff[i] > -1.0f)
        {
            float spot = dot(normalize(pspLightDirection[i]), -L);
            att *= (spot < pspLightSpotLightCutoff[i]) ? 0.0 : pow(spot, pspLightSpotLightExponent[i]);
        }
    }
    A += pspLightAmbientColor[i]  * att;
    D += pspLightDiffuseColor[i]  * att * Dk;
    S += pspLightSpecularColor[i] * att * Sk;
}

void ApplyLighting(inout vec4 Cp, inout vec4 Cs, in vec3 V, in vec3 N)
{
    vec3 Em = materialEmissionColor;
    #if !USE_DYNAMIC_DEFINES
        vec4 Am = pspMatFlags[0] != 0 ? Cp.rgba : materialAmbientColor;
        vec3 Dm = pspMatFlags[1] != 0 ? Cp.rgb  : materialDiffuseColor;
        vec3 Sm = pspMatFlags[2] != 0 ? Cp.rgb  : materialSpecularColor;
    #else
        #if MAT_FLAGS0
            vec4 Am = Cp.rgba;
        #else
            vec4 Am = materialAmbientColor;
        #endif
        #if MAT_FLAGS1
            vec3 Dm = Cp.rgb;
        #else
            vec3 Dm = materialDiffuseColor;
        #endif
        #if MAT_FLAGS2
            vec3 Sm = Cp.rgb;
        #else
            vec3 Sm = materialSpecularColor;
        #endif
    #endif

    vec4 Al = ambientLightColor;
    vec3 Dl = vec3(0.0);
    vec3 Sl = vec3(0.0);

    #if !USE_DYNAMIC_DEFINES
        if (pspLightEnabled[0] != 0) ComputeLight(0, N, V, Al.rgb, Dl, Sl);
        if (pspLightEnabled[1] != 0) ComputeLight(1, N, V, Al.rgb, Dl, Sl);
        if (pspLightEnabled[2] != 0) ComputeLight(2, N, V, Al.rgb, Dl, Sl);
        if (pspLightEnabled[3] != 0) ComputeLight(3, N, V, Al.rgb, Dl, Sl);
    #else
        #if LIGHT_ENABLED0 != 0
            ComputeLight(0, N, V, Al.rgb, Dl, Sl);
        #endif
        #if LIGHT_ENABLED1 != 0
            ComputeLight(1, N, V, Al.rgb, Dl, Sl);
        #endif
        #if LIGHT_ENABLED2 != 0
            ComputeLight(2, N, V, Al.rgb, Dl, Sl);
        #endif
        #if LIGHT_ENABLED3 != 0
            ComputeLight(3, N, V, Al.rgb, Dl, Sl);
        #endif

        #if AVOID_NVIDIA_BUG
            // When this code is not present, the NVIDIA compiler
            // is producing incorrect code (light incorrectly computed).
            // This code is actually doing nothing...
            vec3 dummy = vec3(0.0);
            if (pspLightEnabled[0] == 123456) {
                ComputeLight(0, N, dummy, dummy, dummy, dummy);
            }
            if (dummy.r == 123456.0) {
                Dl = Sl;
            }
        #endif
    #endif

    #if !USE_DYNAMIC_DEFINES
        if (colorAddition)
        {
            Cp.rgb = clamp(Em + Al.rgb * Am.rgb + Dl * Dm, 0.0, 1.0);
            Cs.rgb = clamp(Sl * Sm, 0.0, 1.0);
        }
        else
        {
            Cp.rgb = clamp(Em + Al.rgb * Am.rgb + Dl * Dm + Sl * Sm, 0.0, 1.0);
        }
    #elif LIGHT_MODE
        Cp.rgb = clamp(Em + Al.rgb * Am.rgb + Dl * Dm, 0.0, 1.0);
        Cs.rgb = clamp(Sl * Sm, 0.0, 1.0);
    #else
        Cp.rgb = clamp(Em + Al.rgb * Am.rgb + Dl * Dm + Sl * Sm, 0.0, 1.0);
    #endif
    Cp.a = Al.a * Am.a;
}
#endif


///////////////////////////////////////////////////////////////
// Texture decoding & mapping
///////////////////////////////////////////////////////////////

#if !USE_DYNAMIC_DEFINES || TEX_ENABLE
void ApplyTexture(inout vec4 T, in vec4 V, in vec3 N, in vec3 Ne)
{
    T.xy /= textureScale;

    #if !USE_DYNAMIC_DEFINES
        switch (texMapMode)
        {
        case 0: // UV mapping
            T.xyz = vec3(vec2(pspTextureMatrix * T), 1.0);
            break;

        case 1: // Projection mapping
            switch (texMapProj)
            {
            case 0: // Model Coordinate Projection (XYZ)
                T.xyz = vec3(pspTextureMatrix * vec4(V.xyz, 1.0));
                break;
            case 1: // Texture Coordinate Projection (UV0)
                T.xyz = vec3(pspTextureMatrix * vec4(T.st, 0.0, 1.0));
                break;
            case 2: // Normalized Normal Coordinate projection (N/|N|), using the Normal from the vertex data
                T.xyz = vec3(pspTextureMatrix * vec4(normalize(N.xyz), 1.0));
                break;
            case 3: // Non-normalized Normal Coordinate projection (N), using the Normal from the vertex data
                T.xyz = vec3(pspTextureMatrix * vec4(N.xyz, 1.0));
                break;
            }
            break;

        case 2: // Shade mapping, using the Normal in eye coordinates
            vec3  Nn = normalize(Ne);
            vec3  Ve = vec3(modelViewMatrix * V);
            float k  = materialShininess;
            vec3  Lu = getLightVector(texShade.x, Ve);
            vec3  Lv = getLightVector(texShade.y, Ve);
            float Pu = pspLightKind[texShade.x] == 0 ? dot(Nn, normalize(Lu)) : pow(dot(Nn, normalize(Lu + vec3(0.0, 0.0, 1.0))), k);
            float Pv = pspLightKind[texShade.y] == 0 ? dot(Nn, normalize(Lv)) : pow(dot(Nn, normalize(Lv + vec3(0.0, 0.0, 1.0))), k);
            T.xyz = vec3(0.5*vec2(1.0 + Pu, 1.0 + Pv), 1.0);
            break;
        }
    #elif TEX_MAP_MODE == 0
        // UV mapping
        T.xyz = vec3(vec2(pspTextureMatrix * T), 1.0);
    #elif TEX_MAP_MODE == 1 && TEX_MAP_PROJ == 0
        // Model Coordinate Projection (XYZ)
        T.xyz = vec3(pspTextureMatrix * vec4(V.xyz, 1.0));
    #elif TEX_MAP_MODE == 1 && TEX_MAP_PROJ == 1
        // Texture Coordinate Projection (UV0)
        T.xyz = vec3(pspTextureMatrix * vec4(T.st, 0.0, 1.0));
    #elif TEX_MAP_MODE == 1 && TEX_MAP_PROJ == 2
        // Normalized Normal Coordinate projection (N/|N|)
        T.xyz = vec3(pspTextureMatrix * vec4(normalize(N.xyz), 1.0));
    #elif TEX_MAP_MODE == 1 && TEX_MAP_PROJ == 3
        // Non-normalized Normal Coordinate projection (N)
        T.xyz = vec3(pspTextureMatrix * vec4(N.xyz, 1.0));
    #elif TEX_MAP_MODE == 2
        // Shade mapping
        vec3  Nn = normalize(Ne);
        vec3  Ve = vec3(modelViewMatrix * V);
        float k  = materialShininess;
        vec3  Lu = getLightVector(TEX_SHADE0, Ve);
        vec3  Lv = getLightVector(TEX_SHADE1, Ve);
        float Pu = pspLightKind[TEX_SHADE0] == 0 ? dot(Nn, normalize(Lu)) : pow(dot(Nn, normalize(Lu + vec3(0.0, 0.0, 1.0))), k);
        float Pv = pspLightKind[TEX_SHADE1] == 0 ? dot(Nn, normalize(Lv)) : pow(dot(Nn, normalize(Lv + vec3(0.0, 0.0, 1.0))), k);
        T.xyz = vec3(0.5*vec2(1.0 + Pu, 1.0 + Pv), 1.0);
    #endif
}
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

float getDiscarded(inout vec4 pos)
{
	float discarded = 0.0;

	if (!vinfoTransform2D)
	{
		vec3 screenPos = pos.xyz / pos.w * viewportScale + viewportPos;

		// If x,y are out of [0..4096], discard the triangle(s) using this vertex.
		if (screenPos.x < 0.0 || screenPos.x >= 4096.0 || screenPos.y < 0.0 || screenPos.y >= 4096.0)
		{
			// As a vertex shader cannot discard a vertex, the discarding is done
			// by the fragment shader for each pixel of the rendered triangle.
			// This is achieved by setting the vertex shader output variable "discarded"
			// to a high value. The value stays at 0.0 for non-discarded vertices.
			// This value is then interpolated by OpenGL between the 3 vertices of a triangle
			// when passed to the fragment shader. The fragment shader just needs to check
			// if the value is > 0.0, meaning that at least one vertex is discarded.
			discarded = 100000.0;
		}
		else if (!clipPlaneEnable)
		{
			if (screenPos.z < 0.0 || screenPos.z >= 65536.0)
			{
				discarded = 100000.0;
			}
		}
	}

	return discarded;
}

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

    vec3 Ve = vec3(modelViewMatrix * V);

    #if !USE_DYNAMIC_DEFINES || VINFO_NORMAL != 0
        vec3 Ne = normalMatrix * N;
    #else
		vec3 Ne = vec3(1.0, 0.0, 0.0);
    #endif

    #if !USE_DYNAMIC_DEFINES
        if (lightingEnable) ApplyLighting(Cp, Cs, Ve, normalize(Ne));
    #elif LIGHTING_ENABLE
        ApplyLighting(Cp, Cs, Ve, normalize(Ne));
    #endif

    #if !USE_DYNAMIC_DEFINES
        if (texEnable) ApplyTexture(T, V, N, Ne);
    #elif TEX_ENABLE
        ApplyTexture(T, V, N, Ne);
    #endif

    gl_Position                 = modelViewProjectionMatrix * V;
    fogDepth                    = (fogEnd + Ve.z) * fogScale;
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
    discarded                   = getDiscarded(gl_Position);
}

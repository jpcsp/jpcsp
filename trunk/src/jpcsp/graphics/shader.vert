// INSERT VERSION

// INSERT DEFINES

#define AVOID_NVIDIA_BUG 1

#if USE_BIT_OPERATORS
    #extension GL_EXT_gpu_shader4 : enable
#endif
#if USE_UBO
    #extension GL_ARB_uniform_buffer_object : enable
#endif
#if __VERSION__ >= 130 && defined(GL_ARB_compatibility)
    #extension GL_ARB_compatibility : enable
#endif

// Use attributes instead of gl_Vertex, gl_Normal...: attributes support all the
// data types used by the PSP (signed/unsigned, bytes/shorts/floats).
#if __VERSION__ >= 130
    #define ATTRIBUTE in
#else
    #define ATTRIBUTE attribute
#endif
ATTRIBUTE vec4 pspTexture;
ATTRIBUTE vec4 pspColor;
ATTRIBUTE vec3 pspNormal;
ATTRIBUTE vec4 pspPosition;
ATTRIBUTE vec4 pspWeights1;
ATTRIBUTE vec4 pspWeights2;

#if USE_UBO
	UBO_STRUCTURE
#else
    uniform ivec3 psp_matFlags; // Ambient, Diffuse, Specular
    uniform ivec4 psp_lightType;
    uniform ivec4 psp_lightKind;
    uniform ivec4 psp_lightEnabled;
    uniform mat4  psp_boneMatrix[8];
    uniform int   psp_numberBones;
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
#endif


///////////////////////////////////////////////////////////////
// Lighting
///////////////////////////////////////////////////////////////

#if !USE_DYNAMIC_DEFINES || LIGHTING_ENABLE
void ComputeLight(in int i, in vec3 N, in vec3 V, inout vec3 A, inout vec3 D, inout vec3 S)
{
    float w     = gl_LightSource[i].position.w;
    vec3  L     = gl_LightSource[i].position.xyz - V * w;
    vec3  H     = L + vec3(0.0, 0.0, 1.0);
    float att   = 1.0;
    float NdotL = max(dot(normalize(L), N), 0.0);
    float NdotH = max(dot(normalize(H), N), 0.0);
    float k     = gl_FrontMaterial.shininess;
    float Dk    = (psp_lightKind[i] == 2) ? max(pow(NdotL, k), 0.0) : NdotL;
    float Sk    = (psp_lightKind[i] != 0) ? max(pow(NdotH, k), 0.0) : 0.0;

    if (w != 0.0)
    {
        float d = length(L);
        att = clamp(1.0 / (gl_LightSource[i].constantAttenuation + (gl_LightSource[i].linearAttenuation + gl_LightSource[i].quadraticAttenuation * d) * d), 0.0, 1.0);
        if (gl_LightSource[i].spotCutoff < 180.0)
        {
            float spot = dot(normalize(gl_LightSource[i].spotDirection.xyz), -L);
            att *= (spot < gl_LightSource[i].spotCosCutoff) ? 0.0 : pow(spot, gl_LightSource[i].spotExponent);
        }
    }
    A += gl_LightSource[i].ambient.rgb  * att;
    D += gl_LightSource[i].diffuse.rgb  * att * Dk;
    S += gl_LightSource[i].specular.rgb * att * Sk;
}

void ApplyLighting(inout vec4 Cp, inout vec4 Cs, in vec3 V, in vec3 N)
{
    vec3 Em = gl_FrontMaterial.emission.rgb;
    #if !USE_DYNAMIC_DEFINES
        vec4 Am = psp_matFlags[0] != 0 ? Cp.rgba : gl_FrontMaterial.ambient.rgba;
        vec3 Dm = psp_matFlags[1] != 0 ? Cp.rgb  : gl_FrontMaterial.diffuse.rgb;
        vec3 Sm = psp_matFlags[2] != 0 ? Cp.rgb  : gl_FrontMaterial.specular.rgb;
    #else
        #if MAT_FLAGS0
            vec4 Am = Cp.rgba;
        #else
            vec4 Am = gl_FrontMaterial.ambient.rgba;
        #endif
        #if MAT_FLAGS1
            vec3 Dm = Cp.rgb;
        #else
            vec3 Dm = gl_FrontMaterial.diffuse.rgb;
        #endif
        #if MAT_FLAGS2
            vec3 Sm = Cp.rgb;
        #else
            vec3 Sm = gl_FrontMaterial.specular.rgb;
        #endif
    #endif

    vec4 Al = gl_LightModel.ambient;
    vec3 Dl = vec3(0.0);
    vec3 Sl = vec3(0.0);

    #if !USE_DYNAMIC_DEFINES
        if (psp_lightEnabled[0] != 0) ComputeLight(0, N, V, Al.rgb, Dl, Sl);
        if (psp_lightEnabled[1] != 0) ComputeLight(1, N, V, Al.rgb, Dl, Sl);
        if (psp_lightEnabled[2] != 0) ComputeLight(2, N, V, Al.rgb, Dl, Sl);
        if (psp_lightEnabled[3] != 0) ComputeLight(3, N, V, Al.rgb, Dl, Sl);
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
            if (psp_lightEnabled[0] == 123456) {
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
void ApplyTexture(inout vec4 T, in vec4 V, in vec3 N)
{
    T.xy /= textureScale;

    #if !USE_DYNAMIC_DEFINES
        switch (texMapMode)
        {
        case 0: // UV mapping
            T.xyz = vec3(vec2(gl_TextureMatrix[0] * T), 1.0);
            break;

        case 1: // Projection mapping
            switch (texMapProj)
            {
            case 0: // Model Coordinate Projection (XYZ)
                T.xyz = vec3(gl_TextureMatrix[0] * vec4(V.xyz, 1.0));
                break;
            case 1: // Texture Coordinate Projection (UV0)
                T.xyz = vec3(gl_TextureMatrix[0] * vec4(T.st, 0.0, 1.0));
                break;
            case 2: // Normalized Normal Coordinate projection (N/|N|)
                T.xyz = vec3(gl_TextureMatrix[0] * vec4(normalize(N.xyz), 1.0));
                break;
            case 3: // Non-normalized Normal Coordinate projection (N)
                T.xyz = vec3(gl_TextureMatrix[0] * vec4(N.xyz, 1.0));
                break;
            }
            break;

        case 2: // Shade mapping
            vec3  Nn = normalize(N);
            vec3  Ve = vec3(gl_ModelViewMatrix * V);
            float k  = gl_FrontMaterial.shininess;
            vec3  Lu = gl_LightSource[texShade.x].position.xyz - Ve.xyz * gl_LightSource[texShade.x].position.w;
            vec3  Lv = gl_LightSource[texShade.y].position.xyz - Ve.xyz * gl_LightSource[texShade.y].position.w;
            float Pu = psp_lightKind[texShade.x] == 0 ? dot(Nn, normalize(Lu)) : pow(dot(Nn, normalize(Lu + vec3(0.0, 0.0, 1.0))), k);
            float Pv = psp_lightKind[texShade.y] == 0 ? dot(Nn, normalize(Lv)) : pow(dot(Nn, normalize(Lv + vec3(0.0, 0.0, 1.0))), k);
            T.xyz = vec3(0.5*vec2(1.0 + Pu, 1.0 + Pv), 1.0);
            break;
        }
    #elif TEX_MAP_MODE == 0
        // UV mapping
        T.xyz = vec3(vec2(gl_TextureMatrix[0] * T), 1.0);
    #elif TEX_MAP_MODE == 1 && TEX_MAP_PROJ == 0
        // Model Coordinate Projection (XYZ)
        T.xyz = vec3(gl_TextureMatrix[0] * vec4(V.xyz, 1.0));
    #elif TEX_MAP_MODE == 1 && TEX_MAP_PROJ == 1
        // Texture Coordinate Projection (UV0)
        T.xyz = vec3(gl_TextureMatrix[0] * vec4(T.st, 0.0, 1.0));
    #elif TEX_MAP_MODE == 1 && TEX_MAP_PROJ == 2
        // Normalized Normal Coordinate projection (N/|N|)
        T.xyz = vec3(gl_TextureMatrix[0] * vec4(normalize(N.xyz), 1.0));
    #elif TEX_MAP_MODE == 1 && TEX_MAP_PROJ == 3
        // Non-normalized Normal Coordinate projection (N)
        T.xyz = vec3(gl_TextureMatrix[0] * vec4(N.xyz, 1.0));
    #elif TEX_MAP_MODE == 2
        // Shade mapping
        vec3  Nn = normalize(N);
        vec3  Ve = vec3(gl_ModelViewMatrix * V);
        float k  = gl_FrontMaterial.shininess;
        vec3  Lu = gl_LightSource[TEX_SHADE0].position.xyz - Ve.xyz * gl_LightSource[TEX_SHADE0].position.w;
        vec3  Lv = gl_LightSource[TEX_SHADE1].position.xyz - Ve.xyz * gl_LightSource[TEX_SHADE1].position.w;
        float Pu = psp_lightKind[TEX_SHADE0] == 0 ? dot(Nn, normalize(Lu)) : pow(dot(Nn, normalize(Lu + vec3(0.0, 0.0, 1.0))), k);
        float Pv = psp_lightKind[TEX_SHADE1] == 0 ? dot(Nn, normalize(Lv)) : pow(dot(Nn, normalize(Lv + vec3(0.0, 0.0, 1.0))), k);
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
            W = W2[3]; M = mat3(psp_boneMatrix[7]); V += (M * Vv + psp_boneMatrix[7][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 7
            W = W2[2]; M = mat3(psp_boneMatrix[6]); V += (M * Vv + psp_boneMatrix[6][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 6
            W = W2[1]; M = mat3(psp_boneMatrix[5]); V += (M * Vv + psp_boneMatrix[5][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 5
            W = W2[0]; M = mat3(psp_boneMatrix[4]); V += (M * Vv + psp_boneMatrix[4][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 4
            W = W1[3]; M = mat3(psp_boneMatrix[3]); V += (M * Vv + psp_boneMatrix[3][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 3
            W = W1[2]; M = mat3(psp_boneMatrix[2]); V += (M * Vv + psp_boneMatrix[2][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 2
            W = W1[1]; M = mat3(psp_boneMatrix[1]); V += (M * Vv + psp_boneMatrix[1][3].xyz) * W; N += M * Nv * W;
        #endif
        #if NUMBER_BONES >= 1
            W = W1[0]; M = mat3(psp_boneMatrix[0]); V += (M * Vv + psp_boneMatrix[0][3].xyz) * W; N += M * Nv * W;
        #endif
    #else
        switch (psp_numberBones - 1)
        {
        case 7: W = W2[3]; M = mat3(psp_boneMatrix[7]); V += (M * Vv + psp_boneMatrix[7][3].xyz) * W; N += M * Nv * W;
        case 6: W = W2[2]; M = mat3(psp_boneMatrix[6]); V += (M * Vv + psp_boneMatrix[6][3].xyz) * W; N += M * Nv * W;
        case 5: W = W2[1]; M = mat3(psp_boneMatrix[5]); V += (M * Vv + psp_boneMatrix[5][3].xyz) * W; N += M * Nv * W;
        case 4: W = W2[0]; M = mat3(psp_boneMatrix[4]); V += (M * Vv + psp_boneMatrix[4][3].xyz) * W; N += M * Nv * W;
        case 3: W = W1[3]; M = mat3(psp_boneMatrix[3]); V += (M * Vv + psp_boneMatrix[3][3].xyz) * W; N += M * Nv * W;
        case 2: W = W1[2]; M = mat3(psp_boneMatrix[2]); V += (M * Vv + psp_boneMatrix[2][3].xyz) * W; N += M * Nv * W;
        case 1: W = W1[1]; M = mat3(psp_boneMatrix[1]); V += (M * Vv + psp_boneMatrix[1][3].xyz) * W; N += M * Nv * W;
        case 0: W = W1[0]; M = mat3(psp_boneMatrix[0]); V += (M * Vv + psp_boneMatrix[0][3].xyz) * W; N += M * Nv * W;
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
        if (psp_numberBones > 0) ApplySkinning(V.xyz, N);
    #elif NUMBER_BONES > 0
        ApplySkinning(V.xyz, N);
    #endif

    vec3 Ve = vec3(gl_ModelViewMatrix * V);

    #if !USE_DYNAMIC_DEFINES || VINFO_NORMAL != 0
        N  = gl_NormalMatrix * N;
    #endif

    #if !USE_DYNAMIC_DEFINES
        if (lightingEnable) ApplyLighting(Cp, Cs, Ve, normalize(N));
    #elif LIGHTING_ENABLE
        ApplyLighting(Cp, Cs, Ve, normalize(N));
    #endif

    #if !USE_DYNAMIC_DEFINES
        if (texEnable) ApplyTexture(T, V, N);
    #elif TEX_ENABLE
        ApplyTexture(T, V, N);
    #endif

    gl_Position            = gl_ModelViewProjectionMatrix * V;
    gl_FogFragCoord        = abs(Ve.z);
    gl_TexCoord[0]         = T;
    gl_FrontColor          = Cp;
    gl_FrontSecondaryColor = Cs;
}

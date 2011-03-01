// INSERT VERSION

// INSERT DEFINES

#extension GL_EXT_gpu_shader4 : enable
#if USE_UBO
#   extension GL_ARB_uniform_buffer_object : enable
#endif
#if __VERSION__ >= 130
#   extension GL_ARB_compatibility : enable
#endif

// Use attributes instead of gl_Vertex, gl_Normal...: attributes support all the
// data types used by the PSP (signed/unsigned bytes/shorts/floats).
#if __VERSION__ >= 130
#   define ATTRIBUTE in
#else
#   define ATTRIBUTE attribute
#endif
ATTRIBUTE vec4 texture;
ATTRIBUTE vec4 color;
ATTRIBUTE vec3 normal;
ATTRIBUTE vec4 position;
ATTRIBUTE vec4 weights1;
ATTRIBUTE vec4 weights2;

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
    uniform bool  vinfoTransform2D;
    uniform float positionScale;
    uniform float normalScale;
    uniform float textureScale;
    uniform float weightScale;
#endif

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
            att *= (spot < gl_LightSource[i].spotCosCutoff) ? 0.0 : pow(att, gl_LightSource[i].spotExponent);
        }
    }
    A += gl_LightSource[i].ambient.rgb  * att;
    D += gl_LightSource[i].diffuse.rgb  * att * Dk;
    S += gl_LightSource[i].specular.rgb * att * Sk;
}

void ApplyLighting(inout vec4 Cp, inout vec4 Cs, in vec3 V, in vec3 N)
{
    vec3 Em = gl_FrontMaterial.emission.rgb;
    vec4 Am = psp_matFlags[0] != 0 ? Cp.rgba : gl_FrontMaterial.ambient.rgba;
    vec3 Dm = psp_matFlags[1] != 0 ? Cp.rgb  : gl_FrontMaterial.diffuse.rgb;
    vec3 Sm = psp_matFlags[2] != 0 ? Cp.rgb  : gl_FrontMaterial.specular.rgb;

    vec4 Al = gl_LightModel.ambient;
    vec3 Dl = vec3(0.0);
    vec3 Sl = vec3(0.0);

    if (psp_lightEnabled[0] != 0) ComputeLight(0, N, V, Al.rgb, Dl.rgb, Sl.rgb);
    if (psp_lightEnabled[1] != 0) ComputeLight(1, N, V, Al.rgb, Dl.rgb, Sl.rgb);
    if (psp_lightEnabled[2] != 0) ComputeLight(2, N, V, Al.rgb, Dl.rgb, Sl.rgb);
    if (psp_lightEnabled[3] != 0) ComputeLight(3, N, V, Al.rgb, Dl.rgb, Sl.rgb);

    if (colorAddition)
    {
        Cp.rgb = clamp(Em.rgb + Al.rgb * Am.rgb + Dl.rgb * Dm.rgb, 0.0, 1.0);
        Cs.rgb = clamp(Sl.rgb * Sm.rgb, 0.0, 1.0);
    }
    else
    {
        Cp.rgb = clamp(Em.rgb + Al.rgb * Am.rgb + Dl.rgb * Dm.rgb + Sl.rgb * Sm.rgb, 0.0, 1.0);
    }
    Cp.a = Al.a * Am.a;
}

void ApplyTexture(inout vec4 T, in vec4 V, in vec3 N)
{
    T.xy /= textureScale;

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
}

void ApplySkinning(inout vec3 Vv, inout vec3 Nv)
{
    vec3  V = vec3(0.0, 0.0, 0.0);
    vec3  N = V;
    float W;
    mat3  M;
    vec4  W1 = weights1 / weightScale;
    vec4  W2 = weights2 / weightScale;

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
    Vv = V;
    Nv = N;
}

void DecodePosition(inout vec3 V)
{
    // V.z is unsigned in 2D
    if (vinfoTransform2D)
    {
        switch (vinfoPosition)
        {
        case 1: // GU_VERTEX_8BIT
            // V.z is unsigned 8 bit in 2D
            if (V.z < 0.0) V.z += 256.0;
            break;
        case 2: // GU_VERTEX_16BIT
            // V.z is unsigned 16 bit in 2D
            if (V.z < 0.0) V.z += 65536.0;
            break;
        case 3: // GU_VERTEX_32BITF
            if (V.z < 0.0)
            {
                // Negative V.z are interpreted as 0 in 2D
                V.z = 0.0;
            }
            else
            {
                // 2D positions are always integer values
                V.z = float(int(V.z));
            }
        }
    }
    else
    {
        V /= positionScale;
    }
}

void DecodeColor(inout vec4 C)
{
    switch (vinfoColor)
    {
    case -1: // !useVertexColor
        C = gl_Color;
        break;
    case 4: // GU_COLOR_5650
    {
        int packedBits = int(C.x);
        int rBits = (packedBits      ) & 0x1F;
        int gBits = (packedBits >>  5) & 0x3F;
        int bBits = (packedBits >> 11) & 0x1F;
        C.r = float((rBits << 3) | (rBits >> 2));
        C.g = float((gBits << 2) | (gBits >> 4));
        C.b = float((bBits << 3) | (bBits >> 2));
        C.a = 1.0;
        C.rgb /= 255.0;
        break;
    }
    case 5: // GU_COLOR_5551
    {
        int packedBits = int(C.x);
        int rBits = (packedBits      ) & 0x1F;
        int gBits = (packedBits >>  5) & 0x1F;
        int bBits = (packedBits >> 10) & 0x1F;
        C.r = float((rBits << 3) | (rBits >> 2));
        C.g = float((gBits << 3) | (gBits >> 2));
        C.b = float((bBits << 3) | (bBits >> 2));
        C.a = float((packedBits >> 15) & 0x01);
        C.rgb /= 255.0;
        break;
    }
    case 6: // GU_COLOR_4444
    {
        int packedBits = int(C.x);
        int rBits = (packedBits      ) & 0x0F;
        int gBits = (packedBits >>  4) & 0x0F;
        int bBits = (packedBits >>  8) & 0x0F;
        int aBits = (packedBits >> 12) & 0x0F;
        C.r = float((rBits << 4) | rBits);
        C.g = float((gBits << 4) | gBits);
        C.b = float((bBits << 4) | bBits);
        C.a = float((aBits << 4) | aBits);
        C /= 255.0;
        break;
    }
    case 7: // GU_COLOR_8888
        C /= 255.0;
        break;
    }

}

void main()
{
    vec3 N  = normal;
    vec4 V  = position;
    vec3 Ve = vec3(gl_ModelViewMatrix * V);
    vec4 Cp = color;
    vec4 Cs = vec4(0.0);
    vec4 T  = texture;

    DecodeColor(Cp);
    DecodePosition(V.xyz);
    N /= normalScale;

    if (psp_numberBones > 0) ApplySkinning(V.xyz, N);

    N  = gl_NormalMatrix * N;

    if (lightingEnable) ApplyLighting(Cp, Cs, Ve, normalize(N));

    if (texEnable) ApplyTexture(T, V, N);

    gl_Position            = gl_ModelViewProjectionMatrix * V;
    gl_FogFragCoord        = abs(Ve.z);
    gl_TexCoord[0]         = T;
    gl_FrontColor          = Cp;
    gl_FrontSecondaryColor = Cs;
}

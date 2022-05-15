#if !USE_UBO
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
    uniform vec3  viewportPos;
    uniform vec3  viewportScale;
    uniform bool  clipPlaneEnable;
    uniform bool  texEnable;
    uniform int   texMapMode;
    uniform int   texMapProj;
    uniform ivec2 texShade;
    uniform bool  lightingEnable;
    uniform bool  colorAddition;
    uniform vec4  ambientLightColor;
    uniform float materialShininess;
    uniform vec4  materialAmbientColor;
    uniform vec3  materialDiffuseColor;
    uniform vec3  materialSpecularColor;
    uniform vec3  materialEmissionColor;
    uniform mat4  pspTextureMatrix;
    uniform mat4  modelViewMatrix;
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
    float att   = 1.0;
    float NdotL = max(dot(normalize(L), N), 0.0);
    float k     = materialShininess;
    float Dk    = (pspLightKind[i] == 2) ? max(pow(NdotL, k), 0.0) : NdotL;
    float Sk    = 0.0;
    if (pspLightKind[i] != 0) // LIGHT_DIFFUSE_SPECULAR or LIGHT_POWER_DIFFUSE_SPECULAR
    {
		if (dot(L, N) >= 0)
		{
		    vec3 H = normalize(L) + vec3(0.0, 0.0, 1.0);
		    float NdotH = max(dot(normalize(H), N), 0.0);
			Sk = max(pow(NdotH, k), 0.0);
		}
	}

    if (pspLightType[i] != 0) // Not directional type
    {
        float d = length(L);
        att = clamp(1.0 / dot(pspLightAttenuation[i], vec3(1.0, d, d * d)), 0.0, 1.0);
        if (pspLightType[i] == 2) // Spot light
        {
            float spot = dot(normalize(pspLightDirection[i]), normalize(-L));
            att *= (spot <= pspLightSpotLightCutoff[i]) ? 0.0 : pow(spot, pspLightSpotLightExponent[i]);
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
            vec3  Lu = normalize(getLightVector(texShade.x, Ve));
            vec3  Lv = normalize(getLightVector(texShade.y, Ve));
            float Pu = pspLightKind[texShade.x] == 0 ? dot(Nn, Lu) : pow(dot(Nn, normalize(Lu + vec3(0.0, 0.0, 1.0))), k);
            float Pv = pspLightKind[texShade.y] == 0 ? dot(Nn, Lv) : pow(dot(Nn, normalize(Lv + vec3(0.0, 0.0, 1.0))), k);
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
// Viewport
///////////////////////////////////////////////////////////////

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

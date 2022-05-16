// INSERT VERSION

// INSERT DEFINES

#if USE_UBO
    #extension GL_ARB_uniform_buffer_object : enable
#endif
#if __VERSION__ < 410
    #extension GL_ARB_tessellation_shader : enable
    #extension GL_ARB_separate_shader_objects : enable
#endif

#define LOCATION(N) layout(location=N)

layout(quads) in;

// The input locations must match those defined as output in the tessellation control shader
LOCATION(0) in vec3 in_texCoord[gl_MaxPatchVertices];
#if !USE_DYNAMIC_DEFINES
	LOCATION(3) smooth in vec4 in_pspPrimaryColorSmooth[gl_MaxPatchVertices];
	LOCATION(4) smooth in vec4 in_pspSecondaryColorSmooth[gl_MaxPatchVertices];
	LOCATION(5) flat in vec4 in_pspPrimaryColorFlat[gl_MaxPatchVertices];
	LOCATION(6) flat in vec4 in_pspSecondaryColorFlat[gl_MaxPatchVertices];
#elif SHADE_MODEL == 0
	LOCATION(3) flat in vec4 in_pspPrimaryColor[gl_MaxPatchVertices];
	LOCATION(4) flat in vec4 in_pspSecondaryColor[gl_MaxPatchVertices];
#else
	LOCATION(3) smooth in vec4 in_pspPrimaryColor[gl_MaxPatchVertices];
	LOCATION(4) smooth in vec4 in_pspSecondaryColor[gl_MaxPatchVertices];
#endif

// The output locations must match those defined as input in the fragment shader
LOCATION(0) out vec3 texCoord;
LOCATION(1) noperspective out float discarded;
LOCATION(2) out float fogDepth;
#if !USE_DYNAMIC_DEFINES
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
    uniform bool  vinfoTransform2D;
    uniform mat4  modelMatrix;
    uniform mat4  modelViewProjectionMatrix;
    uniform int   vinfoTexture;
    uniform float textureScale;
    uniform int   curvedSurfaceType;
    uniform ivec4 splineInfo;
    uniform int   patchFace;
#endif

///////////////////////////////////////////////////////////////
// Common functions
// (shared with the Vertex shader)
///////////////////////////////////////////////////////////////
#include "/jpcsp/graphics/shader-common.vert"


///////////////////////////////////////////////////////////////
// Common Bezier and Spline functions
///////////////////////////////////////////////////////////////

float interpolate1(in vec4 bu, in vec4 bv, in float values[gl_MaxPatchVertices])
{
	return bu[0] * (bv[0] * values[ 0] + bv[1] * values[ 4] + bv[2] * values[ 8] + bv[3] * values[12])
	     + bu[1] * (bv[0] * values[ 1] + bv[1] * values[ 5] + bv[2] * values[ 9] + bv[3] * values[13])
	     + bu[2] * (bv[0] * values[ 2] + bv[1] * values[ 6] + bv[2] * values[10] + bv[3] * values[14])
	     + bu[3] * (bv[0] * values[ 3] + bv[1] * values[ 7] + bv[2] * values[11] + bv[3] * values[15]);
}

vec3 interpolate3(in vec4 bu, in vec4 bv, in vec3 values[gl_MaxPatchVertices])
{
	return bu[0] * (bv[0] * values[ 0] + bv[1] * values[ 4] + bv[2] * values[ 8] + bv[3] * values[12])
	     + bu[1] * (bv[0] * values[ 1] + bv[1] * values[ 5] + bv[2] * values[ 9] + bv[3] * values[13])
	     + bu[2] * (bv[0] * values[ 2] + bv[1] * values[ 6] + bv[2] * values[10] + bv[3] * values[14])
	     + bu[3] * (bv[0] * values[ 3] + bv[1] * values[ 7] + bv[2] * values[11] + bv[3] * values[15]);
}

vec4 interpolate4(in vec4 bu, in vec4 bv, in vec4 values[gl_MaxPatchVertices])
{
	return bu[0] * (bv[0] * values[ 0] + bv[1] * values[ 4] + bv[2] * values[ 8] + bv[3] * values[12])
	     + bu[1] * (bv[0] * values[ 1] + bv[1] * values[ 5] + bv[2] * values[ 9] + bv[3] * values[13])
	     + bu[2] * (bv[0] * values[ 2] + bv[1] * values[ 6] + bv[2] * values[10] + bv[3] * values[14])
	     + bu[3] * (bv[0] * values[ 3] + bv[1] * values[ 7] + bv[2] * values[11] + bv[3] * values[15]);
}

vec4 interpolatePosition(in vec4 bu, in vec4 bv)
{
	return bu[0] * (bv[0] * gl_in[0].gl_Position + bv[1] * gl_in[4].gl_Position + bv[2] * gl_in[ 8].gl_Position + bv[3] * gl_in[12].gl_Position)
	     + bu[1] * (bv[0] * gl_in[1].gl_Position + bv[1] * gl_in[5].gl_Position + bv[2] * gl_in[ 9].gl_Position + bv[3] * gl_in[13].gl_Position)
	     + bu[2] * (bv[0] * gl_in[2].gl_Position + bv[1] * gl_in[6].gl_Position + bv[2] * gl_in[10].gl_Position + bv[3] * gl_in[14].gl_Position)
	     + bu[3] * (bv[0] * gl_in[3].gl_Position + bv[1] * gl_in[7].gl_Position + bv[2] * gl_in[11].gl_Position + bv[3] * gl_in[15].gl_Position);
}

void renderCommon(in vec4 bu, in vec4 bv, in vec4 dpdu, in vec4 dpdv)
{
	vec4 V = interpolatePosition(bu, bv);

	#if !USE_DYNAMIC_DEFINES
		vec3 N = patchFace == 0 ? cross(dpdu.xyz, dpdv.xyz) : cross(dpdv.xyz, dpdu.xyz);
	#elif PATCH_FACE == 0
		vec3 N = cross(dpdu.xyz, dpdv.xyz);
	#else
		vec3 N = cross(dpdv.xyz, dpdu.xyz);
	#endif

	#if !USE_DYNAMIC_DEFINES
		// Interpolate the curve only on the flat colors
		vec4 Cp = interpolate4(bu, bv, in_pspPrimaryColorFlat);
		vec4 Cs = interpolate4(bu, bv, in_pspSecondaryColorFlat);
	#else
		vec4 Cp = interpolate4(bu, bv, in_pspPrimaryColor);
		vec4 Cs = interpolate4(bu, bv, in_pspSecondaryColor);
	#endif

    vec3 Ve = vec3(modelViewMatrix * V);
	vec3 Ne = vec3(modelMatrix * vec4(N, 0.0));

    #if !USE_DYNAMIC_DEFINES
        if (lightingEnable) ApplyLighting(Cp, Cs, V.xyz, normalize(Ne));
    #elif LIGHTING_ENABLE
        ApplyLighting(Cp, Cs, V.xyz, normalize(Ne));
    #endif

	vec4 T;
	T.xyz = vinfoTexture != 0 ? interpolate3(bu, bv, in_texCoord) : gl_TessCoord.yxz;
    #if !USE_DYNAMIC_DEFINES
        if (texEnable) ApplyTexture(T, V, N, Ne);
    #elif TEX_ENABLE
        ApplyTexture(T, V, N, Ne);
    #endif

    gl_Position = modelViewProjectionMatrix * V;
    fogDepth = (fogEnd + Ve.z) * fogScale;
    discarded = getDiscarded(gl_Position);
	texCoord = T.xyz;
	#if !USE_DYNAMIC_DEFINES
		pspPrimaryColorSmooth = Cp;
		pspSecondaryColorSmooth = Cs;
		pspPrimaryColorFlat = Cp;
		pspSecondaryColorFlat = Cs;
	#else
		pspPrimaryColor = Cp;
		pspSecondaryColor = Cs;
	#endif
}

///////////////////////////////////////////////////////////////
// Bezier
///////////////////////////////////////////////////////////////

#if !USE_DYNAMIC_DEFINES || CURVED_SURFACE_TYPE == 1
vec4 getBezierFactors(in float u)
{
	float uu = 1.0 - u;
	return vec4(uu * uu * uu, 3.0 * u * uu * uu, 3.0 * u * u * uu, u * u * u);
}

vec4 getBezierDerivativeFactors(in float u)
{
	float uu = 1.0 - u;
	return vec4(-3.0 * uu * uu, 3.0 * uu * (1.0 - 3.0 * u), 3.0 * u * (2.0 - 3.0 * u), 3.0 * u * u);
}

void renderBezier()
{
	float u = gl_TessCoord.y;
	float v = gl_TessCoord.x;

	vec4 bu = getBezierFactors(u);
	vec4 bv = getBezierFactors(v);

	vec4 dbu = getBezierDerivativeFactors(u);
	vec4 dbv = getBezierDerivativeFactors(v);

	// Tangent Vectors
	vec4 dpdu = interpolatePosition(dbu, bv);
	vec4 dpdv = interpolatePosition(bu, dbv);

	renderCommon(bu, bv, dpdu, dpdv);
}
#endif


///////////////////////////////////////////////////////////////
// Spline
///////////////////////////////////////////////////////////////

#if !USE_DYNAMIC_DEFINES || CURVED_SURFACE_TYPE == 2

#define MAX_KNOTS (256 + 4)

// Called with j = 0
float spline0(in int i, in int j, in float u, in int knots[MAX_KNOTS])
{
	return knots[i] <= u && u < knots[i + 1] ? 1.0 : 0.0;
}

// Called with j = 1
float spline1(in int i, in int j, in float u, in int knots[MAX_KNOTS])
{
	float res = 0.0;
	if (knots[i + j] != knots[i])
	{
		res += (u - knots[i]) / (knots[i + j] - knots[i]) * spline0(i, j - 1, u, knots);
	}
	if (knots[i + j + 1] != knots[i + 1]) {
		res += (knots[i + j + 1] - u) / (knots[i + j + 1] - knots[i + 1]) * spline0(i + 1, j - 1, u, knots);
	}
	return res;
}

// Called with j = 2
float spline2(in int i, in int j, in float u, in int knots[MAX_KNOTS])
{
	float res = 0.0;
	if (knots[i + j] != knots[i])
	{
		res += (u - knots[i]) / (knots[i + j] - knots[i]) * spline1(i, j - 1, u, knots);
	}
	if (knots[i + j + 1] != knots[i + 1]) {
		res += (knots[i + j + 1] - u) / (knots[i + j + 1] - knots[i + 1]) * spline1(i + 1, j - 1, u, knots);
	}
	return res;
}

// Called with j = 3
float spline3(in int i, in int j, in float u, in int knots[MAX_KNOTS])
{
	float res = 0.0;
	if (knots[i + j] != knots[i])
	{
		res += (u - knots[i]) / (knots[i + j] - knots[i]) * spline2(i, j - 1, u, knots);
	}
	if (knots[i + j + 1] != knots[i + 1]) {
		res += (knots[i + j + 1] - u) / (knots[i + j + 1] - knots[i + 1]) * spline2(i + 1, j - 1, u, knots);
	}
	return res;
}

vec4 getSplineOrder3(in int end, in float u, in int knots[MAX_KNOTS])
{
	vec4 bu;
	bu[0] = spline3(end - 3, 3, u, knots);
	bu[1] = spline3(end - 2, 3, u, knots);
	bu[2] = spline3(end - 1, 3, u, knots);
	bu[3] = spline3(end    , 3, u, knots);

	return bu;
}

void computeKnots(in int count, in int type, inout int knots[MAX_KNOTS])
{
	int start = -3;
	int end = count;
	if (type == 1 || type == 3)
	{
		start += 3;
        knots[0] = 0;
        knots[1] = 0;
        knots[2] = 0;
	}
	if (type == 2 || type == 3)
	{
		end -= 3;
		knots[count + 1] = end;
		knots[count + 2] = end;
		knots[count + 3] = end;
	}

    for (int i = start; i <= end; i++) {
        knots[i + 3] = i;
    }
}

void renderSpline()
{
	int ucount = splineInfo[0];
	int vcount = splineInfo[1];
	int utype = splineInfo[2];
	int vtype = splineInfo[3];

	int patchU = gl_PrimitiveID % (ucount - 3);
	int patchV = gl_PrimitiveID / (ucount - 3);

	int knotsU[MAX_KNOTS];
	int knotsV[MAX_KNOTS];
	computeKnots(ucount, utype, knotsU);
	computeKnots(vcount, vtype, knotsV);

	float u = patchU + gl_TessCoord.y * 0.999999;
	float v = patchV + gl_TessCoord.x * 0.999999;
	int endU = patchU + 3;
	int endV = patchV + 3;

	vec4 bu = getSplineOrder3(endU, u, knotsU);
	vec4 bv = getSplineOrder3(endV, v, knotsV);

	float step = 0.0001;
	float nu = u + step;
	float nv = v + step;

	vec4 nbu = getSplineOrder3(endU, nu, knotsU);
	vec4 nbv = getSplineOrder3(endV, nv, knotsV);

	vec4 deltaPositionU = interpolatePosition(nbu,  bv);
	vec4 deltaPositionV = interpolatePosition(bu, nbv);

	vec4 V = interpolatePosition(bu, bv);

	// Tangent Vectors
	vec4 dpdu = deltaPositionU - V;
	vec4 dpdv = deltaPositionV - V;

	renderCommon(bu, bv, dpdu, dpdv);
}
#endif


///////////////////////////////////////////////////////////////
// Main program
///////////////////////////////////////////////////////////////

void main()
{
	#if !USE_DYNAMIC_DEFINES
		if (curvedSurfaceType == 1) // RE_BEZIER
		{
			renderBezier();
		}
		else if (curvedSurfaceType == 2) // RE_SPLINE
		{
			renderSpline();
		}
	#elif CURVED_SURFACE_TYPE == 1
		renderBezier();
	#elif CURVED_SURFACE_TYPE == 2
		renderSpline();
	#endif
}

// INSERT VERSION

// INSERT DEFINES

#if __VERSION__ < 410
    #extension GL_ARB_separate_shader_objects : enable
#endif

#define LOCATION(N) layout(location=N)

layout(lines) in;
layout(triangle_strip, max_vertices = 4) out;

// The input locations must match those defined as output in the vertex shader
LOCATION(0) in vec3 in_texCoord[2];
LOCATION(1) noperspective in float in_discarded[2];
LOCATION(2) in float in_fogDepth[2];
#if !USE_DYNAMIC_DEFINES
	LOCATION(3) smooth in vec4 in_pspPrimaryColorSmooth[2];
	LOCATION(4) smooth in vec4 in_pspSecondaryColorSmooth[2];
	LOCATION(5) flat in vec4 in_pspPrimaryColorFlat[2];
	LOCATION(6) flat in vec4 in_pspSecondaryColorFlat[2];
#elif SHADE_MODEL == 0
	LOCATION(3) flat in vec4 in_pspPrimaryColor[2];
	LOCATION(4) flat in vec4 in_pspSecondaryColor[2];
#else
	LOCATION(3) smooth in vec4 in_pspPrimaryColor[2];
	LOCATION(4) smooth in vec4 in_pspSecondaryColor[2];
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

void main()
{
	// Output attributes which are common for all vertices
	discarded = in_discarded[0] + in_discarded[1];
    fogDepth = in_fogDepth[1];
	#if !USE_DYNAMIC_DEFINES
    	pspPrimaryColorSmooth = in_pspPrimaryColorSmooth[1];
    	pspSecondaryColorSmooth = in_pspSecondaryColorSmooth[1];
    	pspPrimaryColorFlat = in_pspPrimaryColorFlat[1];
    	pspSecondaryColorFlat = in_pspSecondaryColorFlat[1];
	#else
    	pspPrimaryColor = in_pspPrimaryColor[1];
    	pspSecondaryColor = in_pspSecondaryColor[1];
	#endif

    vec4 v1 = gl_in[0].gl_Position;
    vec4 v2 = gl_in[1].gl_Position;
    vec3 t1 = in_texCoord[0];
    vec3 t2 = in_texCoord[1];

    // Remark: gl_Position has already been transformed by MVP,
    // and the Y-axis is already inverted in 2D
    bool flippedTexture = (v1.y < v2.y && v1.x < v2.x) ||
                          (v1.y > v2.y && v1.x > v2.x);

    gl_Position = v1;
    texCoord = t1;
    EmitVertex();

    gl_Position = vec4(v1.x, v2.yzw);
    texCoord = flippedTexture ? vec3(t2.x, t1.y, t2.z) : vec3(t1.x, t2.yz);
    EmitVertex();

    gl_Position = vec4(v2.x, v1.y, v2.zw);
    texCoord = flippedTexture ? vec3(t1.x, t2.yz) : vec3(t2.x, t1.y, t2.z);
    EmitVertex();

    gl_Position = v2;
    texCoord = t2;
    EmitVertex();

    EndPrimitive();
}

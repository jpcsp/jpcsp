// INSERT VERSION

// INSERT DEFINES

#extension GL_EXT_gpu_shader4 : enable
#if USE_UBO
#   extension GL_ARB_uniform_buffer_object : enable
#endif
#if __VERSION__ >= 140
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
#endif
uniform sampler2D tex;

vec4 getFragColor()
{
    vec4 Cp = gl_Color;
    vec4 Cs = gl_SecondaryColor;
    vec4 Cd = vec4(vec3(colorDoubling), 1.0);

    if (texEnable)
    {
        vec4 Ct = texture2DProj(tex, gl_TexCoord[0].xyz);

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

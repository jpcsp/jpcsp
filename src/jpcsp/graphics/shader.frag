#version 120
#extension GL_EXT_gpu_shader4 : enable

uniform sampler2D tex;
uniform bool      texEnable;
uniform ivec2     texEnvMode;
uniform int       texMapMode;
uniform float     colorDoubling;

uniform bool      ctestEnable;
uniform int       ctestFunc;
uniform ivec3     ctestRef;
uniform ivec3     ctestMsk;

vec4 getFragColor()
{
    vec4 Cp = gl_Color;
    vec4 Cs = gl_SecondaryColor;
    vec4 Cd = vec4(1.0); // vec4(vec3(colorDoubling), 1.0);

    if (!texEnable)
    {
        return Cp + Cs;
    }
    else
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

        Cp = clamp(Cp, 0.0, 1.0);

        return Cd * (Cp + Cs);
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

    gl_FragColor = Cf;
}

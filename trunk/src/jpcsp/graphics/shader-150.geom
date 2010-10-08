#version 150 compatibility

layout(lines) in;
layout(triangle_strip, max_vertices = 4) out;

void main()
{
    vec4 v1 = gl_in[0].gl_Position;
    vec4 v2 = gl_in[1].gl_Position;
    vec4 t1 = gl_in[0].gl_TexCoord[0];
    vec4 t2 = gl_in[1].gl_TexCoord[0];

    // Flipped:
    //  sprite (16,0)-(0,56) at (0,16,65535)-(56,0,65535)
    // Not flipped:
    //	sprite (0,0)-(0,0) at (279,440,0)-(272,433,0)
    // Not flipped:
    //  sprite (24,0)-(0,48) at (226,120,0)-(254,178,0)
    //
    // Remark: gl_Position has already been transformed by MVP,
    // i.e. the Y-axis is already flipped as compared to the PSP
    bool flippedTexture = (t1.x > t2.x && (v1.x < v2.x && v1.y > v2.y));

    gl_Position = v1;
    gl_FogFragCoord = gl_in[1].gl_FogFragCoord;
    gl_TexCoord[0] = t1;
    gl_FrontColor = gl_in[1].gl_FrontColor;
    gl_FrontSecondaryColor = gl_in[1].gl_FrontSecondaryColor;
    EmitVertex();

    gl_Position = vec4(v1.x, v2.yzw);
    gl_FogFragCoord = gl_in[1].gl_FogFragCoord;
    gl_TexCoord[0] = flippedTexture ? vec4(t2.x, t1.y, t2.zw) : vec4(t1.x, t2.yzw);
    gl_FrontColor = gl_in[1].gl_FrontColor;
    gl_FrontSecondaryColor = gl_in[1].gl_FrontSecondaryColor;
    EmitVertex();

    gl_Position = vec4(v2.x, v1.y, v2.zw);
    gl_FogFragCoord = gl_in[1].gl_FogFragCoord;
    gl_TexCoord[0] = flippedTexture ? vec4(t1.x, t2.yzw) : vec4(t2.x, t1.y, t2.zw);
    gl_FrontColor = gl_in[1].gl_FrontColor;
    gl_FrontSecondaryColor = gl_in[1].gl_FrontSecondaryColor;
    EmitVertex();

    gl_Position = v2;
    gl_FogFragCoord = gl_in[1].gl_FogFragCoord;
    gl_TexCoord[0] = t2;
    gl_FrontColor = gl_in[1].gl_FrontColor;
    gl_FrontSecondaryColor = gl_in[1].gl_FrontSecondaryColor;
    EmitVertex();

    EndPrimitive();
}
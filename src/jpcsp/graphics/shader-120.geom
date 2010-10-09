#version 120
#extension GL_EXT_geometry_shader4 : enable

//
// This shader fails to run on ATI graphic cards:
// an error (dump in atioglxx.dll) occurs when accessing the variable
// "gl_FrontColorIn[0]".
// A shader version 150 is provided for these cards.
//
void main()
{
    vec4 v1 = gl_PositionIn[0];
    vec4 v2 = gl_PositionIn[1];
    vec4 t1 = gl_TexCoordIn[0][0];
    vec4 t2 = gl_TexCoordIn[1][0];

    // Remark: gl_Position has already been transformed by MVP,
    // i.e. the Y-axis is already flipped as compared to the PSP
    bool flippedTexture = (v1.y < v2.y && v1.x < v2.x) || (v1.y > v2.y && v1.x > v2.x);

    gl_Position = v1;
    gl_FogFragCoord = gl_FogFragCoordIn[1];
    gl_TexCoord[0] = t1;
    gl_FrontColor = gl_FrontColorIn[1];
    gl_FrontSecondaryColor = gl_FrontSecondaryColorIn[1];
    EmitVertex();

    gl_Position = vec4(v1.x, v2.yzw);
    gl_FogFragCoord = gl_FogFragCoordIn[1];
    gl_TexCoord[0] = flippedTexture ? vec4(t2.x, t1.y, t2.zw) : vec4(t1.x, t2.yzw);
    gl_FrontColor = gl_FrontColorIn[1];
    gl_FrontSecondaryColor = gl_FrontSecondaryColorIn[1];
    EmitVertex();

    gl_Position = vec4(v2.x, v1.y, v2.zw);
    gl_FogFragCoord = gl_FogFragCoordIn[1];
    gl_TexCoord[0] = flippedTexture ? vec4(t1.x, t2.yzw) : vec4(t2.x, t1.y, t2.zw);
    gl_FrontColor = gl_FrontColorIn[1];
    gl_FrontSecondaryColor = gl_FrontSecondaryColorIn[1];
    EmitVertex();

    gl_Position = v2;
    gl_FogFragCoord = gl_FogFragCoordIn[1];
    gl_TexCoord[0] = t2;
    gl_FrontColor = gl_FrontColorIn[1];
    gl_FrontSecondaryColor = gl_FrontSecondaryColorIn[1];
    EmitVertex();

    EndPrimitive();

    // Fragment shader doing nothing...
//    for (int i = 0; i < gl_VerticesIn; i++)
//    {
//        gl_Position            = gl_PositionIn[i];
//        gl_FogFragCoord        = gl_FogFragCoordIn[i];
//        gl_TexCoord[0]         = gl_TexCoordIn[i][0];
//        gl_FrontColor          = gl_FrontColorIn[i];
//        gl_FrontSecondaryColor = gl_FrontSecondaryColorIn[i];
//        EmitVertex();
//    }
//    EndPrimitive();
}
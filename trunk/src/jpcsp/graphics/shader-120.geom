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
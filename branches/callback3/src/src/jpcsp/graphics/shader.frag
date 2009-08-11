uniform sampler2D tex;
uniform bool texEnable;
uniform int texEnvMode;
uniform vec3 texEnvColor;

void main()
{
	vec3 ct,cf;
	vec4 texel;
	float intensity,at,af;

	if(texEnable) {
		texel = texture2D(tex,gl_TexCoord[0].st);
		ct = texel.rgb;
		at = texel.a;
		
		if(texEnvMode == 0) { // MODULATE
			gl_FragColor = texel * gl_Color; 
		} else if(texEnvMode == 1) { // DECAL
			gl_FragColor = vec4(gl_Color.rgb * (1.0 - texel.a) + texel.rgb * texel.a, gl_Color.a);
		} else if(texEnvMode == 2) { // BLEND
			gl_FragColor = vec4(gl_Color.rgb * (1.0 - gl_Color.a) + texEnvColor.rgb * gl_Color.a, gl_Color.a * texel.a);
		} else if(texEnvMode == 3) { // REPLACE
			gl_FragColor = texel;
		} else if(texEnvMode == 4) { // ADD
			gl_FragColor = vec4(texel.rgb + gl_Color.rgb, texel.a * gl_Color.a);
		}
	} else {
		gl_FragColor = gl_Color;
	}
	gl_FragColor = gl_Color;
}

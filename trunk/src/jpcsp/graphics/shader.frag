uniform sampler2D tex;
uniform bool texEnable;
uniform int texEnvMode;

vec4 getFragColor() {
	if (!texEnable) {
		return gl_Color;
	}

	vec4 texel = texture2D(tex, gl_TexCoord[0].st);
	vec4 color;

	if (texEnvMode == 0) { // MODULATE
		color = gl_Color * texel; 
	} else if(texEnvMode == 1) { // DECAL
		color.rgb = mix(gl_Color.rgb, texel.rgb, texel.a);
		color.a = gl_Color.a;
	} else if(texEnvMode == 2) { // BLEND
		color.rgb = mix(gl_Color.rgb, gl_TextureEnvColor[0].rgb, texel.rgb);
		color.a = gl_Color.a * texel.a;
	} else if(texEnvMode == 3) { // REPLACE
		color = texel;
	} else if(texEnvMode == 4) { // ADD
		color.rgb = gl_Color.rgb + texel.rgb;
		color.a = gl_Color.a * texel.a;
	} else {
		color = gl_Color;
	}

	return color;
}

void main() {
	gl_FragColor = getFragColor();
}

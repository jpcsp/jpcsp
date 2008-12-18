uniform float psp_zPos;
uniform float psp_zScale;
uniform ivec4 psp_matFlags; // Ambient, Diffuse, Specular, Use
uniform ivec4 psp_lightType;
uniform ivec4 psp_lightKind;
uniform ivec4 psp_lightEnabled;
uniform bool texEnable;
uniform bool lightingEnable; 

float calculateAttenuation(in int i, in float dist)
{
    return clamp(1.0 / (gl_LightSource[i].constantAttenuation +
                  gl_LightSource[i].linearAttenuation * dist +
                  gl_LightSource[i].quadraticAttenuation * dist * dist), 0.0, 1.0);
}

void directionalLight(in int i, in vec3 N, in float shininess,
                      inout vec4 ambient, inout vec4 diffuse, inout vec4 specular)
{
    vec3 L = normalize(gl_LightSource[i].position.xyz);
   
    float nDotL = dot(N, L);
   
    if (nDotL > 0.0)
    {   
        vec3 H = gl_LightSource[i].halfVector.xyz;
       
        float pf = pow(max(dot(N,H), 0.0), shininess);

        diffuse  += gl_LightSource[i].diffuse  * nDotL;
        specular += gl_LightSource[i].specular * pf;
    }
   
    ambient  += gl_LightSource[i].ambient;
}

void pointLight(in int i, in vec3 N, in vec3 V, in float shininess,
                inout vec4 ambient, inout vec4 diffuse, inout vec4 specular)
{
    vec3 D = gl_LightSource[i].position.xyz - V;
    vec3 L = normalize(D);

    float dist = length(D);
    float attenuation = calculateAttenuation(i, dist);

    float nDotL = dot(N,L);

    if (nDotL > 0.0)
    {   
        vec3 E = normalize(-V);
        vec3 R = reflect(-L, N);
       
        float pf = pow(max(dot(R,E), 0.0), shininess);

        diffuse  += gl_LightSource[i].diffuse  * attenuation * nDotL;
        specular += gl_LightSource[i].specular * attenuation * pf;
    }
   
    ambient  += gl_LightSource[i].ambient * attenuation;
}

void spotLight(in int i, in vec3 N, in vec3 V, in float shininess,
               inout vec4 ambient, inout vec4 diffuse, inout vec4 specular)
{
    vec3 D = gl_LightSource[i].position.xyz - V;
    vec3 L = normalize(D);

    float dist = length(D);
    float attenuation = calculateAttenuation(i, dist);

    float nDotL = dot(N,L);

    if (nDotL > 0.0)
    {   
        float spotEffect = dot(normalize(gl_LightSource[i].spotDirection), -L);
       
        if (spotEffect > gl_LightSource[i].spotCosCutoff)
        {
            attenuation *=  pow(spotEffect, gl_LightSource[i].spotExponent);

            vec3 E = normalize(-V);
            vec3 R = reflect(-L, N);
       
            float pf = pow(max(dot(R,E), 0.0), shininess);

            diffuse  += gl_LightSource[i].diffuse  * attenuation * nDotL;
            specular += gl_LightSource[i].specular * attenuation * pf;
        }
    }
   
    ambient  += gl_LightSource[i].ambient * attenuation;
}

void calculateLighting(in vec3 N, in vec3 V, in float shininess,
                       inout vec4 ambient, inout vec4 diffuse, inout vec4 specular)
{
    for (int i = 0; i < 4; i++)
    {
    	if(psp_lightEnabled[i] != 0) {
	    	if(psp_lightType[i] == 0)
	    		directionalLight(i, N, shininess, ambient, diffuse, specular);
	    	else if(psp_lightType[i] == 1)
	    		pointLight(i, N, V, shininess, ambient, diffuse, specular);
	    	else if(psp_lightType[i] == 2)
	    		spotLight(i, N, V, shininess, ambient, diffuse, specular);
	    }
    }
}

vec4 doLight(in vec4 matAmbient, in vec4 matDiffuse, in vec4 matSpecular) {
	vec4 ambient  = vec4(0.0);
    vec4 diffuse  = vec4(0.0);
    vec4 specular = vec4(0.0);
	vec3 n = normalize(gl_NormalMatrix * gl_Normal);
	    
    calculateLighting(n, vec3(gl_ModelViewMatrix * gl_Vertex), gl_FrontMaterial.shininess,
                      ambient, diffuse, specular);
   
    vec4 color = gl_FrontLightModelProduct.sceneColor  +
                 (ambient  * matAmbient) +
                 (diffuse  * matDiffuse) +
                 (specular * matSpecular);
               
    return clamp(color, 0.0, 1.0);
}

void main()
{
	if(lightingEnable) {
		vec4 matAmbient, matDiffuse, matSpecular;
		
		if(psp_matFlags[0] != 0) {
			matAmbient = gl_Color * float(psp_matFlags[1]);
			matDiffuse = gl_Color * float(psp_matFlags[2]);
			matSpecular = gl_Color * float(psp_matFlags[3]);
		} else {
			matAmbient = gl_FrontMaterial.ambient;
			matDiffuse = gl_FrontMaterial.diffuse;
			matSpecular = gl_FrontMaterial.specular;
		}		
		gl_FrontColor = doLight(matAmbient, matDiffuse, matSpecular);
	} else {
		gl_FrontColor = gl_Color;
	}
	gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
	gl_Position.z = gl_Position.z * psp_zScale + psp_zPos * gl_Position.w;
	
}

attribute vec4 psp_weights1;
attribute vec4 psp_weights2;
uniform float psp_zPos;
uniform float psp_zScale;
uniform ivec3 psp_matFlags; // Ambient, Diffuse, Specular
uniform ivec4 psp_lightType;
uniform ivec4 psp_lightKind;
uniform ivec4 psp_lightEnabled;
uniform mat4 psp_boneMatrix[8];
uniform int psp_numberBones;
uniform bool texEnable;
uniform bool lightingEnable; 

float calculateAttenuation(in int i, in float dist) {
    return clamp(1.0 / (gl_LightSource[i].constantAttenuation +
                 gl_LightSource[i].linearAttenuation * dist +
                 gl_LightSource[i].quadraticAttenuation * dist * dist), 0.0, 1.0);
}

void directionalLight(in int i, in vec3 N, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular) {
    vec3 L = normalize(gl_LightSource[i].position.xyz);
    float nDotL = dot(N, L);
    if (nDotL > 0.0) {
        vec3 H = gl_LightSource[i].halfVector.xyz;
        float nDotH = dot(N,H);
        if (nDotH > 0.0) {
			float pf = pow(nDotH, gl_FrontMaterial.shininess);
			specular += gl_LightSource[i].specular * pf;
		}
		diffuse += gl_LightSource[i].diffuse * nDotL;
    }

    ambient += gl_LightSource[i].ambient;
}

void pointLight(in int i, in vec3 N, in vec3 V, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular) {
    vec3 D = gl_LightSource[i].position.xyz - V;
    vec3 L = normalize(D);

    float dist = length(D);
    float attenuation = calculateAttenuation(i, dist);

    float nDotL = dot(N,L);

    if (nDotL > 0.0) {
    	// TODO Which model is correct?
    	if (true) {
	        vec3 E = normalize(-V);
	        vec3 R = reflect(-L, N);

			float rDotE = dot(R,E);
			if (rDotE > 0.0) {
				float pf = pow(rDotE, gl_FrontMaterial.shininess);
		        specular += gl_LightSource[i].specular * attenuation * pf;
		    }
		} else {
			vec3 H = normalize(L + vec3(0.0, 0.0, 1.0));
			float nDotH = dot(N,H);
			if (nDotH > 0.0) {
				float pf = pow(nDotH, gl_FrontMaterial.shininess);
				specular += gl_LightSource[i].specular * attenuation * pf;
			}
		}
        diffuse += gl_LightSource[i].diffuse * attenuation * nDotL;
    }

    ambient  += gl_LightSource[i].ambient * attenuation;
}

void spotLight(in int i, in vec3 N, in vec3 V, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular) {
    vec3 D = gl_LightSource[i].position.xyz - V;
    vec3 L = normalize(D);

	// Check if point on surface is inside cone of illumination
    float spotEffect = dot(normalize(gl_LightSource[i].spotDirection), -L);

    if (spotEffect >= gl_LightSource[i].spotCosCutoff) {
	    float dist = length(D);
    	float attenuation = calculateAttenuation(i, dist);

        attenuation *=  pow(spotEffect, gl_LightSource[i].spotExponent);

	    float nDotL = dot(N,L);
	    if (nDotL > 0.0) {
			// TODO Which model is correct?
			if (true) {
	            vec3 E = normalize(-V);
	            vec3 R = reflect(-L, N);

				float rDotE = dot(R,E);
				if (rDotE > 0.0) {
					float pf = pow(rDotE, gl_FrontMaterial.shininess);
					specular += gl_LightSource[i].specular * attenuation * pf;
				}
			} else {
				vec3 H = normalize(L + vec3(0.0, 0.0, 1.0));
				float nDotH = dot(N,H);
				if (nDotH > 0.0) {
					float pf = pow(nDotH, gl_FrontMaterial.shininess);
					specular += gl_LightSource[i].specular * attenuation * pf;
				}
			}
			diffuse += gl_LightSource[i].diffuse * attenuation * nDotL;
	    }

	    ambient += gl_LightSource[i].ambient * attenuation;
	}
}

void calculateLighting(in vec3 N, in vec3 V, inout vec4 ambient, inout vec4 diffuse, inout vec4 specular) {
    for (int i = 0; i < 4; i++) {
    	if (psp_lightEnabled[i] != 0) {
	    	if(psp_lightType[i] == 0)
	    		directionalLight(i, N, ambient, diffuse, specular);
	    	else if(psp_lightType[i] == 1)
	    		pointLight(i, N, V, ambient, diffuse, specular);
	    	else if(psp_lightType[i] == 2)
	    		spotLight(i, N, V, ambient, diffuse, specular);
	    }
    }
}

vec4 getEyeCoordinatePosition() {
	return gl_ModelViewMatrix * gl_Vertex;
}

vec3 getEyeCoordinatePosition3(in vec4 eyeCoordinatePosition) {
	return vec3(eyeCoordinatePosition) / eyeCoordinatePosition.w;
}

vec4 doLight(in vec4 eyeCoordinatePosition, in vec4 matAmbient, in vec4 matDiffuse, in vec4 matSpecular, in vec3 normal) {
	vec4 ambient  = vec4(0.0);
    vec4 diffuse  = vec4(0.0);
    vec4 specular = vec4(0.0);
	vec3 n = normalize(gl_NormalMatrix * normal);

    calculateLighting(n, getEyeCoordinatePosition3(eyeCoordinatePosition), ambient, diffuse, specular);

//	ambient += gl_FrontLightModelProduct.sceneColor;
	ambient += gl_LightModel.ambient;
    vec4 color = (ambient  * matAmbient) +
                 (diffuse  * matDiffuse) +
                 (specular * matSpecular) +
                 gl_FrontMaterial.emission;

    return clamp(color, 0.0, 1.0);
}

vec4 getFrontColor(in vec4 eyeCoordinatePosition, in vec3 normal) {
	if (!lightingEnable) {
		return gl_Color;
	}

	vec4 matAmbient  = psp_matFlags[0] != 0 ? gl_Color : gl_FrontMaterial.ambient;
	vec4 matDiffuse  = psp_matFlags[1] != 0 ? gl_Color : gl_FrontMaterial.diffuse;
	vec4 matSpecular = psp_matFlags[2] != 0 ? gl_Color : gl_FrontMaterial.specular;

	return doLight(eyeCoordinatePosition, matAmbient, matDiffuse, matSpecular, normal);
}

vec4 getPosition(inout vec3 normal) {
	if (psp_numberBones == 0) {
		return gl_Vertex;
	}

	float weights[8];
	weights[0] = psp_weights1.x;
	weights[1] = psp_weights1.y;
	weights[2] = psp_weights1.z;
	weights[3] = psp_weights1.w;
	weights[4] = psp_weights2.x;
	weights[5] = psp_weights2.y;
	weights[6] = psp_weights2.z;
	weights[7] = psp_weights2.w;

	vec4 position = vec4(0.0, 0.0, 0.0, gl_Vertex.w);
	normal = vec3(0.0, 0.0, 0.0);
	for (int i = 0; i < psp_numberBones; i++) {
		float weight = weights[i];
		if (weight != 0.0) {
			position.x += ( gl_Vertex.x * psp_boneMatrix[i][0].x
		    	          + gl_Vertex.y * psp_boneMatrix[i][1].x
		        	      + gl_Vertex.z * psp_boneMatrix[i][2].x
		            	  +               psp_boneMatrix[i][3].x) * weight;
			position.y += ( gl_Vertex.x * psp_boneMatrix[i][0].y
			              + gl_Vertex.y * psp_boneMatrix[i][1].y
			              + gl_Vertex.z * psp_boneMatrix[i][2].y
			              +               psp_boneMatrix[i][3].y) * weight;
			position.z += ( gl_Vertex.x * psp_boneMatrix[i][0].z
			              + gl_Vertex.y * psp_boneMatrix[i][1].z
			              + gl_Vertex.z * psp_boneMatrix[i][2].z
			              +               psp_boneMatrix[i][3].z) * weight;

			// Normals shouldn't be translated :)
			normal.x += ( gl_Normal.x * psp_boneMatrix[i][0].x
		    	        + gl_Normal.y * psp_boneMatrix[i][1].x
		        	    + gl_Normal.z * psp_boneMatrix[i][2].x) * weight;
			normal.y += ( gl_Normal.x * psp_boneMatrix[i][0].y
			            + gl_Normal.y * psp_boneMatrix[i][1].y
			            + gl_Normal.z * psp_boneMatrix[i][2].y) * weight;
			normal.z += ( gl_Normal.x * psp_boneMatrix[i][0].z
			            + gl_Normal.y * psp_boneMatrix[i][1].z
			            + gl_Normal.z * psp_boneMatrix[i][2].z) * weight;
		}
	}

	return position;
}

float getFogFragCoord(vec4 eyeCoordinatePosition) {
	return abs(eyeCoordinatePosition.z);
}

void main() {
	vec4 eyeCoordinatePosition = getEyeCoordinatePosition();
	vec3 normal = gl_Normal;

	gl_Position = gl_ModelViewProjectionMatrix * getPosition(normal);
	gl_FrontColor = getFrontColor(eyeCoordinatePosition, normal);
	gl_FogFragCoord = getFogFragCoord(eyeCoordinatePosition); 
	gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;

//	gl_Position = ftransform();
//	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
//	gl_Position.z = gl_Position.z * psp_zScale + psp_zPos * gl_Position.w;
}

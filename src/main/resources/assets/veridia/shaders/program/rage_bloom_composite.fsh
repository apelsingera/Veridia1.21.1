#version 150

uniform sampler2D NearSampler;
uniform sampler2D FarSampler;
uniform float NearStrength;
uniform float FarStrength;

in vec2 texCoord;

out vec4 fragColor;

// Sum of both glow layers; it is added to the frame by blending (ONE, ONE)
void main() {
    vec3 glow = texture(NearSampler, texCoord).rgb * NearStrength + texture(FarSampler, texCoord).rgb * FarStrength;
    fragColor = vec4(glow, 1.0);
}

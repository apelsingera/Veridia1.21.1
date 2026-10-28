#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Intensity;

in vec2 texCoord;

out vec4 fragColor;

// How much of a pixel goes into the glow: only bright magenta (red and blue high, green clearly lower).
// The dark purple rage sky and fog, white, grey and lava do not glow.
float glowWeight(vec3 color) {
    float magenta = min(color.r, color.b);
    float brightness = smoothstep(0.43, 0.7, magenta);
    float purity = smoothstep(0.12, 0.35, magenta - color.g);
    return brightness * purity;
}

vec3 glowAt(vec2 pixelOffset) {
    vec3 color = texture(DiffuseSampler, texCoord + pixelOffset / InSize).rgb;
    return color * glowWeight(color);
}

// The output is half the frame size: average the four frame pixels under each output pixel
void main() {
    vec3 sum = glowAt(vec2(-0.5, -0.5)) + glowAt(vec2(0.5, -0.5))
             + glowAt(vec2(-0.5, 0.5)) + glowAt(vec2(0.5, 0.5));
    fragColor = vec4(sum * 0.25 * Intensity, 1.0);
}

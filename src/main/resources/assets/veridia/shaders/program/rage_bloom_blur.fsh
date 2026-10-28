#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 BlurDir;
uniform float Radius;

in vec2 texCoord;

out vec4 fragColor;

// Gaussian blur along BlurDir: TAPS samples each way, the farthest one Radius texels away
const int TAPS = 8;
// In sample steps: the farthest sample weighs about 4% of the centre one
const float SIGMA = 3.2;

void main() {
    vec2 tapStep = BlurDir / InSize * (Radius / float(TAPS));
    vec3 sum = texture(DiffuseSampler, texCoord).rgb;
    float total = 1.0;
    for (int i = 1; i <= TAPS; i++) {
        float weight = exp(-0.5 * float(i * i) / (SIGMA * SIGMA));
        vec2 offset = tapStep * float(i);
        sum += (texture(DiffuseSampler, texCoord + offset).rgb + texture(DiffuseSampler, texCoord - offset).rgb) * weight;
        total += 2.0 * weight;
    }
    fragColor = vec4(sum / total, 1.0);
}

#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform vec2 OutSize;
uniform float WarpTime;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 t = InSize / OutSize;
    vec2 uv = texCoord / t;

    // Screen center
    vec2 center = vec2(0.5);

    // Vector from center to current pixel
    vec2 delta = uv - center;
    float dist = length(delta);

    // Wave front radius expands with WarpTime
    float waveRadius = WarpTime * 0.8;

    // Wave band width
    float bandWidth = 0.15;

    // How close this pixel is to the wave front
    float waveDist = dist - waveRadius;

    // Only distort pixels near the wave front (within the band)
    if (abs(waveDist) < bandWidth && waveRadius > 0.0) {
        // Normalized position within the band [-1, 1]
        float bandPos = waveDist / bandWidth;

        // Smooth falloff at band edges
        float falloff = 1.0 - bandPos * bandPos;

        // Warp strength — stronger near the front, fades at edges
        float strength = 0.04 * falloff * min(WarpTime * 2.0, 1.0);

        // Sinusoidal distortion along the radial direction
        float warpAmount = sin(dist * 30.0 - WarpTime * 10.0) * strength;

        // Offset UV along radial direction
        vec2 dir = normalize(delta);
        uv += dir * warpAmount;
    }

    fragColor = texture(DiffuseSampler, uv * t);
}

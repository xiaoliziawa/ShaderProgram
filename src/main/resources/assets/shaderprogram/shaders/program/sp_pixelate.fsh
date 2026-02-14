#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform vec2 OutSize;
uniform float PixelSize;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 t = InSize / OutSize;
    vec2 textureUv = texCoord / t;

    // Compute pixel block size in UV space
    vec2 blockSize = vec2(PixelSize) / InSize;

    // Snap UV to grid center
    vec2 snappedUv = blockSize * floor(textureUv / blockSize) + blockSize * 0.5;

    fragColor = texture(DiffuseSampler, snappedUv * t);
}

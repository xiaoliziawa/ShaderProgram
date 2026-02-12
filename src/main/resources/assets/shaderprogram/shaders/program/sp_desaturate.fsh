#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    // Perceptual luminance weights
    float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 gray = vec3(luma);

    // Keep 20% original color, 80% desaturated
    vec3 desaturated = mix(gray, color.rgb, 0.2);

    // Darken to 85%
    desaturated *= 0.85;

    fragColor = vec4(desaturated, color.a);
}

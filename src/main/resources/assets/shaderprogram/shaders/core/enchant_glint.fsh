#version 150

uniform sampler2D Sampler0;
uniform float GameTime;

in vec2 vUV;

out vec4 fragColor;

void main() {
    float time = GameTime * 1500.0;

    // Scale UV — texture tiles 4x across each block face
    vec2 baseUV = vUV * 4.0;

    // Layer 1: rotated -50 deg, scrolling right
    float a1 = radians(-50.0);
    float c1 = cos(a1), s1 = sin(a1);
    vec2 uv1 = mat2(c1, -s1, s1, c1) * baseUV;
    uv1.x += time * 0.04;

    // Layer 2: rotated +10 deg, scrolling left-down
    float a2 = radians(10.0);
    float c2 = cos(a2), s2 = sin(a2);
    vec2 uv2 = mat2(c2, -s2, s2, c2) * baseUV;
    uv2.x -= time * 0.03;
    uv2.y += time * 0.02;

    // Sample vanilla glint texture twice
    vec3 g1 = texture(Sampler0, uv1).rgb;
    vec3 g2 = texture(Sampler0, uv2).rgb;

    // Combine layers, apply enchantment purple tint
    vec3 glint = min(g1 + g2, vec3(1.0));
    vec3 tint  = vec3(0.6, 0.25, 0.95);
    vec3 color = glint * tint;

    float intensity = max(color.r, max(color.g, color.b));

    fragColor = vec4(color, intensity * 0.65);
}

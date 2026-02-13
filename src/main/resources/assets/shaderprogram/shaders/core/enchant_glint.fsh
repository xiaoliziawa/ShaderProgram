#version 150

uniform sampler2D Sampler0;
uniform float GameTime;

in vec3 vPos;

out vec4 fragColor;

void main() {
    float time = GameTime * 1200.0;

    // Determine face normal from screen-space derivatives
    vec3 dpdx = dFdx(vPos);
    vec3 dpdy = dFdy(vPos);
    vec3 faceNormal = normalize(cross(dpdx, dpdy));
    vec3 absN = abs(faceNormal);

    // Project position onto the face plane for UV
    vec2 uv;
    if (absN.x > absN.y && absN.x > absN.z) {
        uv = vPos.yz; // X-facing
    } else if (absN.y > absN.z) {
        uv = vPos.xz; // Y-facing
    } else {
        uv = vPos.xy; // Z-facing
    }

    // Layer 1: 30-degree diagonal scroll
    float a1 = radians(30.0);
    mat2 rot1 = mat2(cos(a1), -sin(a1), sin(a1), cos(a1));
    vec2 uv1 = rot1 * (uv * 0.5) + vec2(time * 0.05, time * 0.02);
    vec3 g1 = texture(Sampler0, uv1).rgb;

    // Layer 2: -45-degree diagonal scroll, different speed
    float a2 = radians(-45.0);
    mat2 rot2 = mat2(cos(a2), -sin(a2), sin(a2), cos(a2));
    vec2 uv2 = rot2 * (uv * 0.5) + vec2(-time * 0.04, time * 0.06);
    vec3 g2 = texture(Sampler0, uv2).rgb;

    // Combine two scrolling layers
    vec3 glintColor = max(g1, g2);
    float intensity = max(glintColor.r, max(glintColor.g, glintColor.b));

    fragColor = vec4(glintColor, intensity * 0.6);
}

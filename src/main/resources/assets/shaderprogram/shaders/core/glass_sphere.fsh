#version 150

uniform sampler2D Sampler0;
uniform float GameTime;
uniform vec2 ScreenSize;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // Remap billboard UV from [0,1] to [-1,1]
    vec2 uv = texCoord0 * 2.0 - 1.0;

    float dist2 = dot(uv, uv);
    float sphereRadius = 0.85;
    float r2 = sphereRadius * sphereRadius;

    // Anti-aliasing at sphere border
    float edge = length(uv) - sphereRadius;
    float aa = 1.0 - smoothstep(-fwidth(edge) * 1.5, fwidth(edge) * 1.5, edge);

    if (aa < 0.001) {
        discard;
    }

    // Clamp distance inside sphere for safe sqrt
    float safeDist2 = min(dist2, r2 - 0.001);

    // Reconstruct sphere normal from billboard position
    float z = sqrt(r2 - safeDist2);
    vec3 normal = normalize(vec3(uv.x, -uv.y, z));

    // View direction (billboard faces camera, view is along -Z)
    vec3 viewDir = vec3(0.0, 0.0, -1.0);

    float cosTheta = max(0.0, dot(-viewDir, normal));

    // ---- Refraction ----
    float eta = 1.0 / 1.501; // air -> glass IOR
    vec3 refrDir = refract(viewDir, normal, eta);

    // Sample captured scene texture with refraction offset
    vec2 screenUV = gl_FragCoord.xy / ScreenSize;
    vec2 refrUV = screenUV + refrDir.xy * 0.15;
    refrUV = clamp(refrUV, 0.001, 0.999);
    vec3 refrColor = texture(Sampler0, refrUV).rgb;

    // Slight glass color tint
    refrColor *= mix(vec3(1.0), vec3(0.95, 0.98, 1.02), 0.4);

    // ---- Reflection ----
    vec3 reflDir = reflect(viewDir, normal);

    // Slowly rotate reflection over time
    float angle = GameTime * 4000.0;
    float cosA = cos(angle);
    float sinA = sin(angle);
    vec3 rotRefl = vec3(
        reflDir.x * cosA + reflDir.z * sinA,
        reflDir.y,
       -reflDir.x * sinA + reflDir.z * cosA
    );

    // Procedural sky gradient
    float skyT = clamp(rotRefl.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 skyColor = mix(vec3(0.6, 0.7, 0.85), vec3(0.15, 0.3, 0.7), skyT);

    // Ground reflection
    if (rotRefl.y < 0.0) {
        float groundT = clamp(-rotRefl.y, 0.0, 1.0);
        skyColor = mix(vec3(0.4, 0.42, 0.38), vec3(0.2, 0.22, 0.18), groundT);
    }

    // Fake sun highlight
    vec3 sunDir = normalize(vec3(0.4, 0.6, 0.3));
    float sunDot = max(0.0, dot(rotRefl, sunDir));
    skyColor += vec3(1.0, 0.95, 0.85) * pow(sunDot, 64.0) * 0.7;
    skyColor += vec3(1.0, 0.9, 0.75) * pow(sunDot, 8.0) * 0.15;

    vec3 reflColor = skyColor;

    // ---- Fresnel (Schlick's approximation) ----
    float F0 = 0.04; // glass at normal incidence
    float fresnel = F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);

    // Blend refraction and reflection
    vec3 color = mix(refrColor, reflColor, fresnel);

    // ---- Specular highlight ----
    vec3 lightDir = normalize(vec3(0.5, 1.0, 0.3));
    vec3 halfVec = normalize(lightDir - viewDir);
    float spec = pow(max(0.0, dot(normal, halfVec)), 128.0);
    color += vec3(1.0, 1.0, 0.98) * spec * 0.6;

    // Secondary softer specular
    float spec2 = pow(max(0.0, dot(normal, halfVec)), 16.0);
    color += vec3(0.8, 0.85, 0.9) * spec2 * 0.08;

    // Ambient fill
    color += vec3(0.02, 0.025, 0.04);

    // Edge darkening (light absorption at grazing angles)
    float edgeDarken = pow(cosTheta, 0.35);
    color *= mix(0.5, 1.0, edgeDarken);

    fragColor = vec4(color, aa * 0.92);
}

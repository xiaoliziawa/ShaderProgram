#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float GameTime;
uniform float SynthwaveRadius;
uniform float WaveOriginX;
uniform float WaveOriginZ;
uniform float TileCount;
uniform float CameraPosX;
uniform float CameraPosZ;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 chunkPos;

out vec4 fragColor;

// ============================================================
// Synthwave ground shader — faithful port of Shadertoy
// "another synthwave sunset thing" (ground surface only)
// ============================================================

float jTime;

float hash21(vec2 co) {
    return fract(sin(dot(co.xy, vec2(1.9898, 7.233))) * 45758.5433);
}

float pow512(float a) {
    a *= a; a *= a; a *= a; a *= a;
    a *= a; a *= a; a *= a; a *= a;
    return a * a;
}

float pow1d5(float a) {
    return a * sqrt(a);
}

float synthHash(vec2 uv) {
    float w = 1.0 - 0.4 * pow512(0.51 + 0.49 * sin((0.02 * (uv.y + 0.5 * uv.x) - jTime) * 2.0));
    return pow1d5(hash21(uv)) * w;
}

float edgeMin(float dx, vec2 da, vec2 db, vec2 uv) {
    uv.x += 5.0;
    vec3 c = fract(floor(vec3(uv, uv.x + uv.y) + 0.5) * (vec3(0.0, 1.0, 2.0) + 0.61803398875));
    return min(min((1.0 - dx) * db.y, da.x), da.y);
}

vec2 trinoise(vec2 uv) {
    float sq = 1.2247448714;
    uv.x *= sq;
    uv.y -= 0.5 * uv.x;
    vec2 d = fract(uv);
    uv -= d;

    bool c = dot(d, vec2(1.0)) > 1.0;

    vec2 dd = 1.0 - d;
    vec2 da = c ? dd : d;
    vec2 db = c ? d : dd;

    float nn = synthHash(uv + float(c));
    float n2 = synthHash(uv + vec2(1.0, 0.0));
    float n3 = synthHash(uv + vec2(0.0, 1.0));

    float nmid = mix(n2, n3, d.y);
    float ns = mix(nn, c ? n2 : n3, da.y);
    float dx = da.x / db.y;
    return vec2(mix(ns, nmid, dx), edgeMin(dx, da, db, uv + d));
}

// Compute gradient of trinoise height for pseudo-normal
vec3 trinoiseGrad(vec2 uv) {
    const float e = 0.05;
    float h  = trinoise(uv).x;
    float hx = trinoise(uv + vec2(e, 0.0)).x;
    float hz = trinoise(uv + vec2(0.0, e)).x;
    return normalize(vec3(h - hx, 0.5, h - hz));
}

// Simplified sky color for reflection (from Shadertoy gsky)
vec3 skyColor(vec3 rd, vec3 ld) {
    float haze = exp2(-5.0 * (abs(rd.y) - 0.2 * dot(rd, ld)));
    vec3 back = vec3(0.4, 0.1, 0.7);
    return clamp(mix(back, vec3(0.7, 0.1, 0.4), haze), 0.0, 1.0);
}

vec3 synthwaveGround(vec2 worldXZ, vec3 fragPos) {
    // Scale to match Shadertoy proportions
    vec2 uv = worldXZ * 0.15;
    uv.y += jTime * 0.8;

    vec2 n = trinoise(uv);

    // Compute pseudo-normal from height gradient
    vec3 norm = trinoiseGrad(uv);

    // Light direction (synthwave sun, low on horizon)
    vec3 ld = normalize(vec3(0.0, 0.125 + 0.05 * sin(jTime * 0.1), 1.0));

    // Diffuse lighting
    float diff = dot(norm, ld) + 0.1 * norm.y;
    vec3 col = vec3(0.1, 0.11, 0.18) * diff;

    // View direction (chunkPos is camera-relative, camera at origin)
    vec3 rd = normalize(fragPos);

    // Reflection
    vec3 rfd = reflect(rd, norm);
    vec3 rfcol = skyColor(rfd, ld);

    // Fresnel reflection (more at grazing angles)
    float fresnel = 0.05 + 0.95 * pow(max(1.0 + dot(rd, norm), 0.0), 5.0);
    col = mix(col, rfcol, fresnel);

    // Grid edge lines — the iconic synthwave look
    col = mix(col, vec3(0.8, 0.1, 0.92), smoothstep(0.05, 0.0, n.y));

    return col;
}

vec2 tileUV(vec2 uv, float count) {
    vec2 atlasSize = vec2(textureSize(Sampler0, 0));
    vec2 spriteUVSize = 16.0 / atlasSize;
    vec2 halfTexel = 0.5 / atlasSize;
    vec2 spriteOrigin = floor(uv / spriteUVSize + 0.001) * spriteUVSize;
    vec2 localUV = (uv - spriteOrigin) / spriteUVSize;
    vec2 tiledUV = fract(localUV * count);
    return spriteOrigin + halfTexel + tiledUV * (spriteUVSize - 2.0 * halfTexel);
}

void main() {
    vec2 uv = texCoord0;
    if (TileCount > 1.5) {
        uv = tileUV(texCoord0, TileCount);
    }
    vec4 color = texture(Sampler0, uv) * vertexColor * ColorModulator;

    if (SynthwaveRadius > 0.01) {
        float distFromOrigin = length(chunkPos.xz - vec2(WaveOriginX, WaveOriginZ));

        float transitionWidth = 5.0;
        float coverage = 1.0 - smoothstep(max(SynthwaveRadius - transitionWidth, 0.0), SynthwaveRadius, distFromOrigin);

        if (coverage > 0.001) {
            jTime = GameTime * 1200.0;

            // Restore world-absolute coords for noise sampling (chunkPos is camera-relative)
            vec2 worldXZ = chunkPos.xz + vec2(CameraPosX, CameraPosZ);
            vec2 synthUV = worldXZ + chunkPos.y * 0.37;
            vec3 synthCol = synthwaveGround(synthUV, chunkPos);

            // Bright border line at wave edge
            float borderLine = 1.0 - smoothstep(0.0, 2.5, abs(distFromOrigin - SynthwaveRadius + 1.5));
            synthCol += vec3(0.8, 0.1, 0.92) * borderLine * 0.6;

            color.rgb = mix(color.rgb, synthCol, coverage);
        }
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

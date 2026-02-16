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
uniform float SeascapeEnabled;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 chunkPos;
in vec3 worldPos;

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

vec3 trinoiseGrad(vec2 uv) {
    const float e = 0.05;
    float h  = trinoise(uv).x;
    float hx = trinoise(uv + vec2(e, 0.0)).x;
    float hz = trinoise(uv + vec2(0.0, e)).x;
    return normalize(vec3(h - hx, 0.5, h - hz));
}

vec3 skyColor(vec3 rd, vec3 ld) {
    float haze = exp2(-5.0 * (abs(rd.y) - 0.2 * dot(rd, ld)));
    vec3 back = vec3(0.4, 0.1, 0.7);
    return clamp(mix(back, vec3(0.7, 0.1, 0.4), haze), 0.0, 1.0);
}

vec3 synthwaveGround(vec2 worldXZ, vec3 fragPos) {
    vec2 uv = worldXZ * 0.15;
    uv.y += jTime * 0.8;

    vec2 n = trinoise(uv);
    vec3 norm = trinoiseGrad(uv);

    vec3 ld = normalize(vec3(0.0, 0.125 + 0.05 * sin(jTime * 0.1), 1.0));

    float diff = dot(norm, ld) + 0.1 * norm.y;
    vec3 col = vec3(0.1, 0.11, 0.18) * diff;

    vec3 rd = normalize(fragPos);
    vec3 rfd = reflect(rd, norm);
    vec3 rfcol = skyColor(rfd, ld);

    float fresnel = 0.05 + 0.95 * pow(max(1.0 + dot(rd, norm), 0.0), 5.0);
    col = mix(col, rfcol, fresnel);

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

// ============================================================
// Seascape water — BSL-style dual-scale noise normals
// with Fresnel reflection, specular, and parallax
// ============================================================

const float WATER_SPEED = 1.0;
const float WATER_BUMP = 1.0;
const float WATER_DETAIL = 0.25;
const float WATER_SHARPNESS = 0.2;

// Procedural noise (no texture needed)
float seaHash(vec2 p) {
    float h = dot(p, vec2(127.1, 311.7));
    return fract(sin(h) * 43758.5453123);
}

float seaNoiseSample(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(seaHash(i), seaHash(i + vec2(1.0, 0.0)), u.x),
        mix(seaHash(i + vec2(0.0, 1.0)), seaHash(i + vec2(1.0, 1.0)), u.x),
        u.y);
}

// BSL-style multi-octave noise for height map (like noisetex sampling)
float seaNoiseOctaves(vec2 p) {
    float n = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        n += seaNoiseSample(p) * amp;
        p = p * 2.03 + vec2(0.5);
        amp *= 0.5;
    }
    return n;
}

// BSL-style dual-scale height map
// Large waves (scale 256) + small detail (scale 48), animated in opposite directions
float getWaterHeightMap(vec3 waterPos, vec2 offset, float time) {
    vec2 wind = vec2(time) * 0.5 * WATER_SPEED;

    waterPos.xz += waterPos.y * 0.2;

    // Layer A: large waves
    float noiseA = seaNoiseOctaves((waterPos.xz - wind) / 16.0 + offset / 16.0);
    // Layer B: small detail
    float noiseB = seaNoiseOctaves((waterPos.xz + wind) / 3.0 + offset / 3.0);

    return mix(noiseA, noiseB, WATER_DETAIL) * WATER_BUMP;
}

// BSL-style finite-difference normal with Fresnel-aware strength
vec3 getWaterNormal(vec3 waterPos, vec3 viewPos, float time) {
    float normalOffset = WATER_SHARPNESS;

    // Fresnel: reduce normal strength at grazing angles (more reflective = flatter)
    vec3 viewDir = normalize(viewPos);
    float fresnel = pow(clamp(1.0 + viewDir.y, 0.0, 1.0), 8.0);
    float normalStrength = 0.35 * (1.0 - fresnel);

    // Sample height at 4 adjacent points
    float h1 = getWaterHeightMap(waterPos, vec2( normalOffset, 0.0), time);
    float h2 = getWaterHeightMap(waterPos, vec2(-normalOffset, 0.0), time);
    float h3 = getWaterHeightMap(waterPos, vec2(0.0,  normalOffset), time);
    float h4 = getWaterHeightMap(waterPos, vec2(0.0, -normalOffset), time);

    float xDelta = (h2 - h1) / normalOffset;
    float yDelta = (h4 - h3) / normalOffset;

    vec3 normalMap = vec3(xDelta, yDelta, 1.0 - (xDelta * xDelta + yDelta * yDelta));
    return normalize(normalMap * normalStrength + vec3(0.0, 0.0, 1.0 - normalStrength));
}

// BSL-style parallax wave refinement (4 iterations)
vec3 getParallaxWaves(vec3 waterPos, vec3 viewVector, float time) {
    vec3 pPos = waterPos;
    for (int i = 0; i < 4; i++) {
        float height = -1.25 * getWaterHeightMap(pPos, vec2(0.0), time) + 0.25;
        pPos.xz += height * viewVector.xz * 0.2;
    }
    return pPos;
}

// Sky color for Fresnel reflection
vec3 waterSkyColor(vec3 e) {
    e.y = (max(e.y, 0.0) * 0.8 + 0.2) * 0.8;
    return vec3(pow(1.0 - e.y, 2.0), 1.0 - e.y, 0.6 + (1.0 - e.y) * 0.4) * 1.1;
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

            float borderLine = 1.0 - smoothstep(0.0, 2.5, abs(distFromOrigin - SynthwaveRadius + 1.5));
            synthCol += vec3(0.8, 0.1, 0.92) * borderLine * 0.6;

            color.rgb = mix(color.rgb, synthCol, coverage);
        }
    }

    if (SeascapeEnabled > 0.5) {
        float time = GameTime * 1200.0;
        vec3 viewDir = normalize(chunkPos);
        vec3 lightDir = normalize(vec3(0.0, 1.0, 0.8));

        // BSL-style parallax wave refinement
        vec3 waterPos = getParallaxWaves(worldPos, viewDir, time);

        // BSL-style normal with Fresnel-aware strength
        vec3 waterNorm = getWaterNormal(waterPos, chunkPos, time);
        // Convert tangent-space normal (XY = tangent, Z = up) to world-space (XZY)
        vec3 n = normalize(vec3(waterNorm.x, waterNorm.z, waterNorm.y));

        // Fresnel reflection
        float fresnel = pow(clamp(1.0 - dot(n, -viewDir), 0.0, 1.0), 5.0);
        fresnel = fresnel * 0.65 + 0.02;

        vec3 reflected = waterSkyColor(reflect(viewDir, n));
        vec3 waterBase = vec3(0.0, 0.09, 0.18);
        vec3 waterTint = vec3(0.8, 0.9, 0.6) * 0.6;
        vec3 refracted = waterBase + waterTint * 0.12 * pow(dot(n, lightDir) * 0.4 + 0.6, 80.0);

        vec3 seaCol = mix(refracted, reflected, fresnel);

        // Specular highlight
        float spec = pow(max(dot(reflect(viewDir, n), lightDir), 0.0), 60.0);
        spec *= (60.0 + 8.0) / (3.1415926 * 8.0);
        seaCol += vec3(spec);

        color.rgb = seaCol;
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

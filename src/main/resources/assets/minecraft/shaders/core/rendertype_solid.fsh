#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float GameTime;
uniform float DissolveProgress;
uniform float TileCount;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 chunkPos;

out vec4 fragColor;

// Based on original code by inigo quilez - iq/2013
// https://www.shadertoy.com/view/ldl3W8
// Modified by hadyn lander, ported for Minecraft block dissolve

#define PHASE_POWER 2.0

vec2 hash2(vec2 p) {
    return fract(sin(vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)))) * 43758.5453);
}

vec4 voronoi(in vec2 x) {
    vec2 n = floor(x);
    vec2 f = fract(x);
    vec2 o;

    vec2 mg, mr;
    float oldDist;
    float md = 8.0;

    for (int j = -1; j <= 1; j++)
    for (int i = -1; i <= 1; i++) {
        vec2 g = vec2(float(i), float(j));
        o = hash2(n + g);
        vec2 r = g + o - f;
        float d = dot(r, r);
        if (d < md) {
            md = d;
            mr = r;
            mg = g;
        }
    }

    oldDist = md;

    md = 8.0;
    for (int j = -2; j <= 2; j++)
    for (int i = -2; i <= 2; i++) {
        vec2 g = mg + vec2(float(i), float(j));
        o = hash2(n + g);
        vec2 r = g + o - f;
        if (dot(mr - r, mr - r) > 0.00001)
            md = min(md, dot(0.5 * (mr + r), normalize(r - mr)));
    }

    return vec4(md, mr, oldDist);
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

    if (DissolveProgress > 0.001) {
        // Project 3D world position to 2D for Voronoi
        vec2 p = chunkPos.xz + chunkPos.y * 0.37;
        p *= 0.8;

        // DissolveProgress drives the wave sweep
        float timeStep = DissolveProgress * 2.0 - 0.5;

        // Subtle drift from GameTime
        float t = GameTime * 200.0;
        p += vec2(t * 0.02, t * 0.01);

        vec4 c = voronoi(p);
        c.x = 1.0 - pow(1.0 - c.x, 2.0);

        // Cell phase — wave pattern per cell
        float cellPhase = p.x + c.y + 2.0 * sin((p.y + c.z) * 0.8 + (p.x + c.y) * 0.4);
        cellPhase *= 0.025;
        cellPhase = clamp(abs(mod(cellPhase - timeStep, 1.0) - 0.5) * 2.0, 0.0, 1.0);
        cellPhase = pow(clamp(cellPhase * 2.0 - 0.5, 0.0, 1.0), PHASE_POWER);

        // Edge phase — position-based wave ignoring cell assignment
        float edgePhase = p.x + 2.0 * sin(p.y * 0.8 + p.x * 0.4);
        edgePhase *= 0.025;
        edgePhase = clamp(abs(mod(edgePhase - timeStep, 1.0) - 0.5) * 2.0, 0.0, 1.0);
        edgePhase = pow(clamp(edgePhase * 2.0 - 0.5, 0.0, 1.0), PHASE_POWER);

        float phase = mix(edgePhase, cellPhase, smoothstep(0.0, 0.2, edgePhase));

        // phase ≈ 1 → wave has passed → dissolve (discard)
        // phase ≈ 0 → wave hasn't reached → block still visible
        if (phase > 0.95) discard;

        // Voronoi coloring at the transition zone
        float shapedPhase = 1.0 - pow(1.0 - phase, 2.0);
        vec3 voronoiCol = mix(
            vec3(0.0, 0.6, 1.0),
            vec3(1.0, 1.0, 1.0),
            smoothstep(
                shapedPhase - mix(0.025, 0.001, shapedPhase),
                shapedPhase,
                mix(c.x, 0.999 - c.w, shapedPhase)
            )
        );

        // Blend: low phase = normal blocks, increasing phase = Voronoi pattern
        color.rgb = mix(color.rgb, voronoiCol, smoothstep(0.0, 0.3, phase));
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

#version 150

// Heartfelt - by Martijn Steinrucken aka BigWings - 2017
// Adapted for Minecraft post-processing

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform vec2 OutSize;
uniform float RainTime;
uniform float RainIntensity;
uniform float ThunderFlash;

in vec2 texCoord;
out vec4 fragColor;

vec3 N13(float p) {
    vec3 p3 = fract(vec3(p) * vec3(.1031, .11369, .13787));
    p3 += dot(p3, p3.yzx + 19.19);
    return fract(vec3((p3.x + p3.y) * p3.z, (p3.x + p3.z) * p3.y, (p3.y + p3.z) * p3.x));
}

float N(float t) {
    return fract(sin(t * 12345.564) * 7658.76);
}

float Saw(float b, float t) {
    return smoothstep(0., b, t) * smoothstep(1., b, t);
}

vec2 DropLayer2(vec2 uv, float t) {
    vec2 UV = uv;
    uv.y += t * 0.75;
    vec2 a = vec2(6., 1.);
    vec2 grid = a * 2.;
    vec2 id = floor(uv * grid);

    float colShift = N(id.x);
    uv.y += colShift;

    id = floor(uv * grid);
    vec3 n = N13(id.x * 35.2 + id.y * 2376.1);
    vec2 st = fract(uv * grid) - vec2(.5, 0);

    float x = n.x - .5;
    float y = UV.y * 20.;
    float wiggle = sin(y + sin(y));
    x += wiggle * (.5 - abs(x)) * (n.z - .5);
    x *= .7;
    float ti = fract(t + n.z);
    y = (Saw(.85, ti) - .5) * .9 + .5;
    vec2 p = vec2(x, y);

    float d = length((st - p) * a.yx);
    float mainDrop = smoothstep(.4, .0, d);

    float r = sqrt(smoothstep(1., y, st.y));
    float cd = abs(st.x - x);
    float trail = smoothstep(.23 * r, .15 * r * r, cd);
    float trailFront = smoothstep(-.02, .02, st.y - y);
    trail *= trailFront * r * r;

    y = UV.y;
    float trail2 = smoothstep(.2 * r, .0, cd);
    float droplets = max(0., (sin(y * (1. - y) * 120.) - st.y)) * trail2 * trailFront * n.z;
    y = fract(y * 10.) + (st.y - .5);
    float dd = length(st - vec2(x, y));
    droplets = smoothstep(.3, 0., dd);
    float m = mainDrop + droplets * r * trailFront;

    return vec2(m, trail);
}

float StaticDrops(vec2 uv, float t) {
    uv *= 40.;
    vec2 id = floor(uv);
    uv = fract(uv) - .5;
    vec3 n = N13(id.x * 107.45 + id.y * 3543.654);
    vec2 p = (n.xy - .5) * .7;
    float d = length(uv - p);
    float fade = Saw(.025, fract(t + n.z));
    float c = smoothstep(.3, 0., d) * fract(n.z * 10.) * fade;
    return c;
}

vec2 Drops(vec2 uv, float t, float l0, float l1, float l2) {
    float s = StaticDrops(uv, t) * l0;
    vec2 m1 = DropLayer2(uv, t) * l1;
    vec2 m2 = DropLayer2(uv * 1.85, t) * l2;

    float c = s + m1.x + m2.x;
    c = smoothstep(.3, 1., c);

    return vec2(c, max(m1.y * l0, m2.y * l1));
}

vec3 sampleBlurred(vec2 uv, float blur) {
    float r = blur * 2.0 / InSize.x;

    vec3 col  = texture(DiffuseSampler, uv).rgb * 4.0;
    col += texture(DiffuseSampler, uv + vec2( r,  0.0)).rgb * 2.0;
    col += texture(DiffuseSampler, uv + vec2(-r,  0.0)).rgb * 2.0;
    col += texture(DiffuseSampler, uv + vec2(0.0,  r )).rgb * 2.0;
    col += texture(DiffuseSampler, uv + vec2(0.0, -r )).rgb * 2.0;
    col += texture(DiffuseSampler, uv + vec2( r,  r) * 0.707).rgb;
    col += texture(DiffuseSampler, uv + vec2(-r, -r) * 0.707).rgb;
    col += texture(DiffuseSampler, uv + vec2( r, -r) * 0.707).rgb;
    col += texture(DiffuseSampler, uv + vec2(-r,  r) * 0.707).rgb;

    return col / 16.0;
}

void main() {
    vec2 tScale = InSize / OutSize;
    vec2 UV = texCoord / tScale;

    vec3 original = texture(DiffuseSampler, texCoord).rgb;

    float rainAmount = RainIntensity;
    if (rainAmount < 0.001) {
        fragColor = vec4(original, 1.0);
        return;
    }

    vec2 rainUV = (UV - 0.5) * vec2(InSize.x / InSize.y, 1.0);

    float T = RainTime;
    float t = T * 0.2;

    float maxBlur = mix(3.0, 6.0, rainAmount);
    float minBlur = 2.0;

    float staticDrops = smoothstep(-0.5, 1.0, rainAmount) * 2.0;
    float layer1 = smoothstep(0.25, 0.75, rainAmount);
    float layer2 = smoothstep(0.0, 0.5, rainAmount);

    vec2 c = Drops(rainUV, t, staticDrops, layer1, layer2);

    vec2 e = vec2(0.001, 0.0);
    float cx = Drops(rainUV + e, t, staticDrops, layer1, layer2).x;
    float cy = Drops(rainUV + e.yx, t, staticDrops, layer1, layer2).x;
    vec2 n = vec2(cx - c.x, cy - c.x);

    float focus = mix(maxBlur - c.y, minBlur, smoothstep(0.1, 0.2, c.x));

    float radial = length((UV - 0.5) * vec2(InSize.x / InSize.y, 1.0));
    float lensFactor = radial * radial;

    n *= rainAmount * lensFactor;
    focus *= rainAmount * lensFactor;

    vec3 col = sampleBlurred((UV + n) * tScale, focus);

    // Lightning flash — driven by actual in-game thunder strikes
    col *= 1.0 + ThunderFlash * 0.4;

    vec2 vUV = UV - 0.5;
    col *= 1.0 - dot(vUV, vUV) * 0.4;

    col = mix(original, col, rainAmount);

    fragColor = vec4(col, 1.0);
}

#version 150

uniform float GameTime;

in vec3 vDir;

out vec4 fragColor;

// ============================================================
// Synthwave sky — ported from Shadertoy "another synthwave sunset thing"
// gsky() + starnoise() + addsun()
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

// Star field noise — original Shadertoy starnoise()
float starnoise(vec3 rd) {
    float c = 0.0;
    vec3 p = normalize(rd) * 300.0;
    for (float i = 0.0; i < 4.0; i++) {
        vec3 q = fract(p) - 0.5;
        vec3 id = floor(p);
        float c2 = smoothstep(0.5, 0.0, length(q));
        c2 *= step(hash21(id.xz / id.y), 0.06 - i * i * 0.005);
        c += c2;
        p = p * 0.6 + 0.5 * p * mat3(
            3.0/5.0, 0.0, 4.0/5.0,
            0.0,     1.0, 0.0,
           -4.0/5.0, 0.0, 3.0/5.0
        );
    }
    c *= c;
    float g = dot(sin(rd * 10.512), cos(rd.yzx * 10.512));
    c *= smoothstep(-3.14, -0.9, g) * 0.5 + 0.5 * smoothstep(-0.3, 1.0, g);
    return c * c;
}

// Synthwave sun — original Shadertoy addsun()
// 条纹太阳，经典蒸汽波/合成波风格
void addsun(vec3 rd, vec3 ld, inout vec3 col) {
    float sun = smoothstep(0.21, 0.2, distance(rd, ld));

    if (sun > 0.0) {
        float yd = rd.y - ld.y;
        float a = sin(3.1 * exp(-yd * 14.0));
        sun *= smoothstep(-0.8, 0.0, a);
        col = mix(col, vec3(1.0, 0.8, 0.4) * 0.75, sun);
    }
}

// Sky color — original Shadertoy gsky() with sun
vec3 gsky(vec3 rd, vec3 ld) {
    float haze = exp2(-5.0 * (abs(rd.y) - 0.2 * dot(rd, ld)));

    float st = starnoise(rd) * (1.0 - min(haze, 1.0));

    vec3 back = vec3(0.4, 0.1, 0.7);

    vec3 col = clamp(mix(back, vec3(0.7, 0.1, 0.4), haze) + st, 0.0, 1.0);

    addsun(rd, ld, col);

    return col;
}

void main() {
    vec3 rd = normalize(vDir);

    jTime = GameTime * 1200.0;

    // 光源方向（与地面shader一致）
    vec3 ld = normalize(vec3(0.0, 0.125 + 0.05 * sin(jTime * 0.1), 1.0));

    vec3 col = gsky(rd, ld);

    fragColor = vec4(col, 1.0);
}

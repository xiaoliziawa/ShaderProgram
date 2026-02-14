#version 150

// 太阳着色器
// 改编自 Shadertoy - Panteleymonov A K 2015
// 4D animated noise → 恒星表面 + 日冕射线

uniform float GameTime;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 vObjPos;
in vec3 vViewPos;

out vec4 fragColor;

const float SPHERE_RADIUS = 3.0;

// ---- 4D animated noise (Panteleymonov) ----
vec4 hash4(vec4 n) { return fract(sin(n) * 1399763.5453123); }

float noise4q(vec4 x) {
    vec4 n3 = vec4(0.0, 0.25, 0.5, 0.75);
    vec4 p2 = floor(x.wwww + n3);
    vec4 b = floor(x.xxxx + n3) + floor(x.yyyy + n3) * 157.0 + floor(x.zzzz + n3) * 113.0;
    vec4 p1 = b + fract(p2 * 0.00390625) * vec4(164352.0, -164352.0, 163840.0, -163840.0);
    p2 = b + fract((p2 + 1.0) * 0.00390625) * vec4(164352.0, -164352.0, 163840.0, -163840.0);
    vec4 f1 = fract(x.xxxx + n3);
    vec4 f2 = fract(x.yyyy + n3);
    f1 = f1 * f1 * (3.0 - 2.0 * f1);
    f2 = f2 * f2 * (3.0 - 2.0 * f2);
    vec4 n1 = vec4(0.0, 1.0, 157.0, 158.0);
    vec4 n2 = vec4(113.0, 114.0, 270.0, 271.0);
    vec4 vs1 = mix(hash4(p1), hash4(n1.yyyy + p1), f1);
    vec4 vs2 = mix(hash4(n1.zzzz + p1), hash4(n1.wwww + p1), f1);
    vec4 vs3 = mix(hash4(p2), hash4(n1.yyyy + p2), f1);
    vec4 vs4 = mix(hash4(n1.zzzz + p2), hash4(n1.wwww + p2), f1);
    vs1 = mix(vs1, vs2, f2);
    vs3 = mix(vs3, vs4, f2);
    vs2 = mix(hash4(n2.xxxx + p1), hash4(n2.yyyy + p1), f1);
    vs4 = mix(hash4(n2.zzzz + p1), hash4(n2.wwww + p1), f1);
    vs2 = mix(vs2, vs4, f2);
    vs4 = mix(hash4(n2.xxxx + p2), hash4(n2.yyyy + p2), f1);
    vec4 vs5 = mix(hash4(n2.zzzz + p2), hash4(n2.wwww + p2), f1);
    vs4 = mix(vs4, vs5, f2);
    f1 = fract(x.zzzz + n3);
    f2 = fract(x.wwww + n3);
    f1 = f1 * f1 * (3.0 - 2.0 * f1);
    f2 = f2 * f2 * (3.0 - 2.0 * f2);
    vs1 = mix(vs1, vs2, f1);
    vs3 = mix(vs3, vs4, f1);
    vs1 = mix(vs1, vs3, f2);
    float r = dot(vs1, vec4(0.25));
    return r * r * (3.0 - 2.0 * r);
}

// ---- 恒星表面 noise ----
float noiseSpere(vec3 ray, vec3 pos, float r, mat3 mr, float zoom, vec3 subnoise, float anim) {
    float b = dot(ray, pos);
    float c = dot(pos, pos) - b * b;
    float s = 0.0;
    float d = 0.03125;
    float d2 = zoom / (d * d);
    float ar = 5.0;
    for (int i = 0; i < 3; i++) {
        float rq = r * r;
        if (c < rq) {
            float l1 = sqrt(rq - c);
            vec3 r1 = (ray * (b - l1) - pos) * mr;
            s += abs(noise4q(vec4(r1 * d2 + subnoise * ar, anim * ar)) * d);
        }
        ar -= 2.0;
        d *= 4.0;
        d2 *= 0.0625;
        r -= r * 0.02;
    }
    return s;
}

// ---- 边缘暗环 ----
float ring(vec3 ray, vec3 pos, float r, float size) {
    float b = dot(ray, pos);
    float c = dot(pos, pos) - b * b;
    return max(0.0, 1.0 - size * abs(r - sqrt(c)));
}

// ---- 日冕射线 ----
float ringRayNoise(vec3 ray, vec3 pos, float r, float size, mat3 mr, float anim) {
    float b = dot(ray, pos);
    vec3 pr = ray * b - pos;
    float c = length(pr);
    pr = normalize(pr) * mr;
    float s = max(0.0, 1.0 - size * abs(r - c));
    float nd = noise4q(vec4(pr, -anim + c)) * 2.0;
    nd = pow(nd, 2.0);
    float n = 0.4;
    float ns = 1.0;
    if (c > r) {
        n = noise4q(vec4(pr * 10.0, -anim + c));
        ns = noise4q(vec4(pr * 50.0, -anim * 2.5 + c * 2.0)) * 2.0;
    }
    n = n * n * nd * ns;
    return pow(s, 4.0) + s * s * n;
}

void main() {
    float normDist = length(vObjPos.xy) / SPHERE_RADIUS;
    if (normDist > 1.0) discard;

    float time = GameTime * 2400.0;

    // 旋转矩阵（表面 noise 动画）
    float mx = time * 0.025;
    float my = -0.6;
    vec2 sn = sin(vec2(mx, my));
    vec2 cs = cos(vec2(mx, my));
    mat3 mr = mat3(vec3(cs.x, 0.0, sn.x), vec3(0.0, 1.0, 0.0), vec3(-sn.x, 0.0, cs.x));
    mr = mat3(vec3(1.0, 0.0, 0.0), vec3(0.0, cs.y, sn.y), vec3(0.0, -sn.y, cs.y)) * mr;

    // Billboard 坐标 → Shadertoy 等效光线 + 恒星位置
    vec2 p = vObjPos.xy / SPHERE_RADIUS * 1.2;
    vec3 ray = normalize(vec3(p, 2.0));
    vec3 pos = vec3(0.0, 0.0, 3.0);

    // 恒星表面 — 两层 noise
    float s1 = noiseSpere(ray, pos, 1.0, mr, 0.5, vec3(0.0), time);
    s1 = pow(min(1.0, s1 * 2.4), 2.0);
    float s2 = noiseSpere(ray, pos, 1.0, mr, 4.0, vec3(83.23, 34.34, 67.453), time);
    s2 = min(1.0, s2 * 2.2);

    vec3 col = mix(vec3(1.0, 1.0, 0.0), vec3(1.0), pow(s1, 60.0)) * s1;
    col += mix(mix(vec3(1.0, 0.0, 0.0), vec3(1.0, 0.0, 1.0), pow(s2, 2.0)),
               vec3(1.0), pow(s2, 10.0)) * s2;

    // 边缘暗环（临边昏暗效果）
    col -= vec3(ring(ray, pos, 1.03, 11.0)) * 2.0;
    col = max(vec3(0.0), col);

    // 日冕射线
    float s3 = ringRayNoise(ray, pos, 0.96, 1.0, mr, time);
    col += mix(vec3(1.0, 0.6, 0.1), vec3(1.0, 0.95, 1.0), pow(s3, 3.0)) * s3;

    col = clamp(col, 0.0, 1.0);

    // Alpha: 恒星体不透明，光晕半透明，billboard 边缘淡出
    float alpha = clamp(max(col.r, max(col.g, col.b)) * 1.5, 0.0, 1.0);
    alpha *= smoothstep(1.0, 0.85, normDist);

    fragColor = vec4(col, alpha);
}

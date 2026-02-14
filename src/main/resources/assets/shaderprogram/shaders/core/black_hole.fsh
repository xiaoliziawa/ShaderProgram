#version 150

// 黑洞引力透镜着色器
// 光线步进 + 引力弯曲，边缘混合回未扭曲背景消除 billboard 边界

uniform sampler2D Sampler0;   // 场景捕获纹理
uniform float GameTime;
uniform vec2 ScreenSize;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 vObjPos;
in vec3 vViewPos;

out vec4 fragColor;

// ---- 可调参数 ----
const float SPHERE_RADIUS = 3.0;   // billboard 半径（blocks）
const float BH_SIZE  = 0.08;       // 归一化黑洞质量（影响视界大小和透镜强度）

// ---- 出射光线（视空间） → 屏幕 UV ----
vec2 rayToScreenUV(vec3 viewRay) {
    vec4 clip = ProjMat * vec4(viewRay * 1000.0, 1.0);
    if (clip.w <= 0.0) return vec2(0.5);
    return clamp((clip.xy / clip.w) * 0.5 + 0.5, 0.001, 0.999);
}

void main() {
    float d = length(vObjPos.xy);
    float normDist = d / SPHERE_RADIUS;
    if (normDist > 1.0) discard;

    float lensStrength = smoothstep(1.0, 0.7, normDist);

    vec2 straightUV = rayToScreenUV(normalize(vViewPos));
    vec3 straightBg = texture(Sampler0, straightUV).rgb;

    vec3 centerView = vViewPos - vObjPos;
    vec3 pos = -centerView / SPHERE_RADIUS;
    vec3 ray = normalize(vViewPos);

    // ---- 光线步进主循环 ----
    for (int i = 0; i < 90; i++) {
        float dotpos = max(dot(pos, pos), 1e-10);
        float invDist = inversesqrt(dotpos);
        float centDist = dotpos * invDist;
        float stepDist = max(centDist * 0.1, 0.0001);
        float invDistSqr = invDist * invDist;
        float bendForce = stepDist * invDistSqr * BH_SIZE * 0.625;
        ray = normalize(ray - (bendForce * invDist) * pos);
        pos += stepDist * ray;

        float dist2 = length(pos);

        if (dist2 < BH_SIZE * 0.1) {
            fragColor = vec4(mix(straightBg, vec3(0.0), lensStrength), 1.0);
            return;
        }
        if (dist2 > BH_SIZE * 1000.) {
            vec2 lensedUV = rayToScreenUV(ray);
            vec3 lensedBg = texture(Sampler0, lensedUV).rgb;
            fragColor = vec4(mix(straightBg, lensedBg, lensStrength), 1.0);
            return;
        }
    }

    fragColor = vec4(mix(straightBg, vec3(0.0), lensStrength), 1.0);
}

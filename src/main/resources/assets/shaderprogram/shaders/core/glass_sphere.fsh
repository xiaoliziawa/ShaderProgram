#version 150

uniform sampler2D Sampler0;
uniform float GameTime;
uniform vec2 ScreenSize;
uniform mat4 ModelViewMat;

in vec3 vObjPos;
in vec3 vViewPos;

out vec4 fragColor;

void main() {
    // 从物体空间位置计算球体法线（球心在原点，normalize(pos) = 法线）
    vec3 objNormal = normalize(vObjPos);

    // gl_FrontFacing == false 表示看到的是背面，即相机在球体内部
    if (!gl_FrontFacing) {
        objNormal = -objNormal;
    }

    // 将法线变换到视图空间
    // mat3(ModelViewMat) 提取旋转部分（平移不影响方向向量）
    vec3 normal = normalize(mat3(ModelViewMat) * objNormal);

    // 逐像素视线方向（视图空间中相机在原点，vViewPos 是片段位置）
    vec3 viewDir = normalize(vViewPos);

    float cosTheta = max(0.0, dot(-viewDir, normal));

    // ---- 折射 ----
    float eta = 1.0 / 1.501; // 空气 -> 玻璃
    vec3 refrDir = refract(viewDir, normal, eta);

    // 使用折射偏移采样捕获的场景纹理
    vec2 screenUV = gl_FragCoord.xy / ScreenSize;
    vec2 refrUV = screenUV + refrDir.xy * 0.15;
    refrUV = clamp(refrUV, 0.001, 0.999);
    vec3 refrColor = texture(Sampler0, refrUV).rgb;

    // 轻微的玻璃颜色色调
    refrColor *= mix(vec3(1.0), vec3(0.95, 0.98, 1.02), 0.4);

    // ---- 反射 ----
    vec3 reflDir = reflect(viewDir, normal);
    // 将反射方向变换回世界空间，让天空渐变不随相机旋转
    vec3 worldRefl = transpose(mat3(ModelViewMat)) * reflDir;

    // 反射随时间缓慢旋转
    float angle = GameTime * 4000.0;
    float cosA = cos(angle);
    float sinA = sin(angle);
    vec3 rotRefl = vec3(
        worldRefl.x * cosA + worldRefl.z * sinA,
        worldRefl.y,
       -worldRefl.x * sinA + worldRefl.z * cosA
    );

    // 程序化天空渐变
    float skyT = clamp(rotRefl.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 skyColor = mix(vec3(0.6, 0.7, 0.85), vec3(0.15, 0.3, 0.7), skyT);

    // 地面反射
    if (rotRefl.y < 0.0) {
        float groundT = clamp(-rotRefl.y, 0.0, 1.0);
        skyColor = mix(vec3(0.4, 0.42, 0.38), vec3(0.2, 0.22, 0.18), groundT);
    }

    // 模拟太阳高光
    vec3 sunDir = normalize(vec3(0.4, 0.6, 0.3));
    float sunDot = max(0.0, dot(rotRefl, sunDir));
    skyColor += vec3(1.0, 0.95, 0.85) * pow(sunDot, 64.0) * 0.7;
    skyColor += vec3(1.0, 0.9, 0.75) * pow(sunDot, 8.0) * 0.15;

    vec3 reflColor = skyColor;

    // ---- 菲涅尔效果（Schlick近似） ----
    float F0 = 0.04;
    float fresnel = F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);

    // 混合折射和反射
    vec3 color = mix(refrColor, reflColor, fresnel);

    // ---- 镜面高光 ----
    // 将世界空间光源方向变换到视图空间
    vec3 lightDir = normalize(mat3(ModelViewMat) * normalize(vec3(0.5, 1.0, 0.3)));
    vec3 halfVec = normalize(lightDir - viewDir);
    float spec = pow(max(0.0, dot(normal, halfVec)), 128.0);
    color += vec3(1.0, 1.0, 0.98) * spec * 0.6;

    // 次级柔和镜面高光
    float spec2 = pow(max(0.0, dot(normal, halfVec)), 16.0);
    color += vec3(0.8, 0.85, 0.9) * spec2 * 0.08;

    // 环境光填充
    color += vec3(0.02, 0.025, 0.04);

    // 边缘变暗（掠射角光线吸收）
    float edgeDarken = pow(cosTheta, 0.35);
    color *= mix(0.5, 1.0, edgeDarken);

    fragColor = vec4(color, 1.0);
}

#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

// 物体空间位置，用于片段着色器中计算法线和到黑洞中心的距离
out vec3 vObjPos;
// 视空间位置，用于计算逐像素视线方向
out vec3 vViewPos;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    vObjPos = Position;
    vViewPos = viewPos.xyz;
}

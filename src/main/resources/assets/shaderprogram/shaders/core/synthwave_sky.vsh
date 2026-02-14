#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 vDir;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // 顶点位置即为从球心出发的方向（球心在相机位置）
    vDir = Position;
}

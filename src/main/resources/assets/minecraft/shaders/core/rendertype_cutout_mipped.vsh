#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform int FogShape;
uniform float GameTime;
uniform float DissolveProgress;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 chunkPos;

void main() {
    vec3 pos = Position + ChunkOffset;
    chunkPos = Position + ChunkOffset;

    if (DissolveProgress > 0.001) {
        float h = fract(sin(dot(Position, vec3(12.9898, 78.233, 45.164))) * 43758.5453);
        float disp = DissolveProgress * DissolveProgress * 3.0;
        float t = GameTime * 2000.0;

        vec3 offset = vec3(0.0);
        offset.y += disp * (0.5 + h * 0.5);
        offset.x += sin(h * 6.283 + t) * disp * 0.3;
        offset.z += cos(h * 5.0 + t * 1.3) * disp * 0.3;

        pos += offset;
    }

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
}

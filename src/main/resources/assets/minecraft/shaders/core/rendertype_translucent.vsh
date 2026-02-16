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
uniform float SeascapeEnabled;
uniform float CameraPosX;
uniform float CameraPosZ;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 chunkPos;
out vec3 worldPos;

// BSL-style dual sine wave displacement
float wavingWater(vec3 wPos, float time) {
    float wave = sin(6.2831854 * (time * 0.7 + wPos.x * 0.14 + wPos.z * 0.07))
               + sin(6.2831854 * (time * 0.5 + wPos.x * 0.10 + wPos.z * 0.20));
    return wave * 0.0125;
}

void main() {
    vec3 pos = Position + ChunkOffset;

    if (SeascapeEnabled > 0.5 && Normal.y > 0.5) {
        vec3 wPos = pos + vec3(CameraPosX, 0.0, CameraPosZ);
        float fractY = fract(wPos.y + 0.005);

        // BSL edge detection: only displace top-surface vertices (fractY > 0.01)
        // Bottom vertices and side vertices stay fixed — no gaps at block edges
        if (fractY > 0.01) {
            float time = GameTime * 1200.0;
            pos.y += wavingWater(wPos, time);
        }
    }

    chunkPos = pos;
    worldPos = pos + vec3(CameraPosX, 0.0, CameraPosZ);

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
}

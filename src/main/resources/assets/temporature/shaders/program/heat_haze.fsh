#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform HeatHazeConfig {
    float intensity;
};

out vec4 fragColor;

void main() {
    if (intensity < 0.005) {
        fragColor = texture(InSampler, texCoord);
        return;
    }

    // Vignette mask
    vec2 uv = texCoord - 0.5;
    float dist = length(uv) / 0.7071;
    float vignette = smoothstep(0.2, 1.1, dist);
    float mask = vignette * intensity;

    if (mask < 0.005) {
        fragColor = texture(InSampler, texCoord);
        return;
    }

    // Chromatic aberration
    float aspect = OutSize.x / OutSize.y;
    vec2 dir = normalize(vec2(uv.x * aspect, uv.y) + 0.0001);
    float aberration = mask * 0.007;

    float r = texture(InSampler, clamp(texCoord + dir * aberration * 2.0, 0.0, 1.0)).r;
    float g = texture(InSampler, clamp(texCoord + dir * aberration * 0.8, 0.0, 1.0)).g;
    float b = texture(InSampler, clamp(texCoord - dir * aberration * 0.6, 0.0, 1.0)).b;

    vec3 color = vec3(r, g, b);

    float brightness = dot(color, vec3(0.299, 0.587, 0.114));
    vec3 heatTint = vec3(1.0, 0.38, 0.08);
    float tintStrength = mask * smoothstep(0.35, 0.95, brightness);
    color = mix(color, color * heatTint, tintStrength);

    fragColor = vec4(color, 1.0);
}
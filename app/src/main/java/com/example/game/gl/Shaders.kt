package com.example.game.gl

import android.opengl.GLES30

object Shaders {

    fun compileProgram(vsSrc: String, fsSrc: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vsSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fsSrc)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs)
        GLES30.glAttachShader(prog, fs)
        GLES30.glLinkProgram(prog)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        return shader
    }

    const val SHADOW_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
uniform mat4 uLightMvp;
void main() {
    gl_Position = uLightMvp * vec4(aPos, 1.0);
}
"""

    const val INSTANCED_SHADOW_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 5) in vec4 aInstPosScale;
layout(location = 6) in vec4 aInstYawTint;
uniform mat4 uLightVp;
void main() {
    float c = cos(aInstYawTint.x);
    float s = sin(aInstYawTint.x);
    vec3 p = aPos * aInstPosScale.w;
    vec3 rot = vec3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c);
    vec3 world = rot + aInstPosScale.xyz;
    gl_Position = uLightVp * vec4(world, 1.0);
}
"""

    const val SHADOW_FS = """#version 300 es
precision mediump float;
out vec4 fragColor;
void main() {
    fragColor = vec4(gl_FragCoord.z, 0.0, 0.0, 1.0);
}
"""

    const val SKY_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
uniform mat4 uViewProj;
out vec3 vDir;
void main() {
    vDir = aPos;
    vec4 pos = uViewProj * vec4(aPos, 1.0);
    gl_Position = pos.xyww;
}
"""

    const val SKY_FS = """#version 300 es
precision highp float;
in vec3 vDir;
uniform vec3 uSunDir;
out vec4 fragColor;

vec3 acesFilmic(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    vec3 dir = normalize(vDir);
    float h = clamp(dir.y, -0.2, 1.0);
    vec3 zenith = vec3(0.05, 0.09, 0.24);
    vec3 midSky = vec3(0.46, 0.23, 0.42);
    vec3 horizon = vec3(1.0, 0.54, 0.24);

    vec3 col = mix(horizon, midSky, smoothstep(0.0, 0.26, h));
    col = mix(col, zenith, smoothstep(0.22, 0.78, h));

    float sunDot = max(dot(dir, normalize(uSunDir)), 0.0);
    col += vec3(1.0, 0.62, 0.28) * pow(sunDot, 12.0) * 0.65;
    col += vec3(1.0, 0.92, 0.74) * smoothstep(0.9965, 0.9993, sunDot) * 2.6;

    col = pow(acesFilmic(col), vec3(1.0 / 1.1));
    fragColor = vec4(col, 1.0);
}
"""

    const val OCEAN_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
uniform mat4 uViewProj;
uniform float uTime;
out vec3 vWorldPos;
void main() {
    vec3 p = aPos;
    p.y += sin(p.x * 0.18 + uTime * 2.0) * 0.12 + cos(p.z * 0.22 + uTime * 1.6) * 0.10;
    vWorldPos = p;
    gl_Position = uViewProj * vec4(p, 1.0);
}
"""

    const val OCEAN_FS = """#version 300 es
precision highp float;
in vec3 vWorldPos;
uniform vec3 uCamPos;
uniform vec3 uSunDir;
uniform float uTime;
uniform sampler2D uWaterTex;
out vec4 fragColor;

vec3 acesFilmic(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    vec3 V = normalize(uCamPos - vWorldPos);
    vec2 uv1 = vWorldPos.xz * 0.045 + vec2(uTime * 0.035, uTime * 0.022);
    vec2 uv2 = vWorldPos.xz * 0.028 - vec2(uTime * 0.021, uTime * 0.031);
    vec3 tex1 = texture(uWaterTex, uv1).rgb;
    vec3 tex2 = texture(uWaterTex, uv2).rgb;
    vec3 causticMix = (tex1 + tex2) * 0.5;

    float w1 = sin(vWorldPos.x * 0.45 + uTime * 2.4) * cos(vWorldPos.z * 0.35 + uTime * 1.7);
    float w2 = cos(vWorldPos.x * 0.9 - uTime * 1.8) * sin(vWorldPos.z * 0.8 + uTime * 2.1);
    vec3 N = normalize(vec3((w1 + w2) * 0.08 + (tex1.r - 0.2) * 0.09, 1.0, (w1 - w2) * 0.08 + (tex2.g - 0.5) * 0.09));

    vec3 R = reflect(-V, N);
    float skyH = clamp(R.y, 0.0, 1.0);
    vec3 skyCol = mix(vec3(1.0, 0.56, 0.28), vec3(0.08, 0.15, 0.34), smoothstep(0.0, 0.55, skyH));

    float distToIsland = max(abs(vWorldPos.x), abs(vWorldPos.z)) - 145.0;
    float shallow = 1.0 - smoothstep(0.0, 38.0, max(distToIsland, 0.0));
    vec3 deepWater = vec3(0.03, 0.30, 0.42);
    vec3 turquoise = causticMix * vec3(0.28, 0.98, 0.94);
    vec3 waterBase = mix(deepWater, turquoise, 0.35 + shallow * 0.60);

    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.4);
    vec3 col = mix(waterBase, skyCol, clamp(fresnel * 0.75 + 0.10, 0.0, 0.92));

    vec3 H = normalize(normalize(uSunDir) + V);
    float spec = pow(max(dot(N, H), 0.0), 210.0);
    col += vec3(1.0, 0.84, 0.52) * spec * 1.85;

    // Exponential horizon fog (fogExp2)
    float dist = length(vWorldPos.xz - uCamPos.xz);
    float fogDensity = 0.0020;
    float fogFactor = 1.0 - exp(-fogDensity * fogDensity * dist * dist);
    col = mix(col, vec3(0.95, 0.56, 0.34), clamp(fogFactor, 0.0, 0.92));

    col = pow(acesFilmic(col), vec3(1.0 / 1.08));
    fragColor = vec4(col, 1.0);
}
"""

    const val WORLD_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;
layout(location = 3) in vec3 aColor;
layout(location = 4) in float aMatType;

uniform mat4 uModel;
uniform mat4 uViewProj;
uniform mat4 uLightMvp;

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vUv;
out vec3 vColor;
out float vMatType;
out vec4 vShadowCoord;

void main() {
    vec4 world = uModel * vec4(aPos, 1.0);
    vWorldPos = world.xyz;
    vNormal = normalize(mat3(uModel) * aNormal);
    vUv = aUv;
    vColor = aColor;
    vMatType = aMatType;
    vShadowCoord = uLightMvp * world;
    gl_Position = uViewProj * world;
}
"""

    const val INSTANCED_WORLD_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;
layout(location = 3) in vec3 aColor;
layout(location = 4) in float aMatType;
layout(location = 5) in vec4 aInstPosScale;
layout(location = 6) in vec4 aInstYawTint;

uniform mat4 uViewProj;
uniform mat4 uLightVp;

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vUv;
out vec3 vColor;
out float vMatType;
out vec4 vShadowCoord;

void main() {
    float c = cos(aInstYawTint.x);
    float s = sin(aInstYawTint.x);
    vec3 p = aPos * aInstPosScale.w;
    vec3 rotPos = vec3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c);
    vec3 rotNorm = vec3(aNormal.x * c + aNormal.z * s, aNormal.y, -aNormal.x * s + aNormal.z * c);
    vec4 world = vec4(rotPos + aInstPosScale.xyz, 1.0);

    vWorldPos = world.xyz;
    vNormal = normalize(rotNorm);
    vUv = aUv;
    vColor = aColor * aInstYawTint.yzw;
    vMatType = aMatType;
    vShadowCoord = uLightVp * world;
    gl_Position = uViewProj * world;
}
"""

    const val WORLD_FS = """#version 300 es
precision highp float;

in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vUv;
in vec3 vColor;
in float vMatType;
in vec4 vShadowCoord;

uniform vec3 uCamPos;
uniform vec3 uSunDir;
uniform sampler2D uShadowMap;
uniform sampler2D uAsphaltTex;
uniform sampler2D uSkyscraperTex;
uniform sampler2D uCurbTex;
uniform float uTime;

// Physically-based car material uniforms
uniform vec3 uPaintColor;
uniform float uPaintRoughness;
uniform float uPaintMetalness;
uniform float uPaintClearcoat;
uniform float uPaintClearcoatRoughness;
uniform float uBrakeGlow;
uniform float uReverseGlow;
uniform vec3 uAccentColor;
uniform vec3 uCaliperColor;

out vec4 fragColor;

vec3 acesFilmic(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

vec3 evalSunsetEnv(vec3 R) {
    float h = clamp(R.y, -0.35, 1.0);
    vec3 ground = vec3(0.08, 0.11, 0.17);
    vec3 horizon = vec3(1.0, 0.54, 0.25);
    vec3 zenith = vec3(0.09, 0.15, 0.35);
    vec3 sky = mix(horizon, zenith, smoothstep(0.0, 0.55, max(h, 0.0)));
    vec3 env = mix(ground, sky, smoothstep(-0.08, 0.08, h));
    float sunHighlight = pow(max(dot(R, normalize(uSunDir)), 0.0), 28.0);
    env += vec3(1.0, 0.86, 0.56) * sunHighlight * 1.25;
    return env;
}

float calcShadow(vec4 sc, vec3 N, vec3 L) {
    vec3 proj = sc.xyz / sc.w;
    proj = proj * 0.5 + 0.5;
    if (proj.z > 1.0 || proj.x < 0.0 || proj.x > 1.0 || proj.y < 0.0 || proj.y > 1.0) {
        return 1.0;
    }
    float bias = max(0.0022 * (1.0 - dot(N, L)), 0.0008);
    float texel = 1.0 / 1024.0;
    float shadow = 0.0;
    shadow += (proj.z - bias > texture(uShadowMap, proj.xy).r) ? 0.30 : 1.0;
    shadow += (proj.z - bias > texture(uShadowMap, proj.xy + vec2(texel, 0.0)).r) ? 0.30 : 1.0;
    shadow += (proj.z - bias > texture(uShadowMap, proj.xy + vec2(-texel, 0.0)).r) ? 0.30 : 1.0;
    shadow += (proj.z - bias > texture(uShadowMap, proj.xy + vec2(0.0, texel)).r) ? 0.30 : 1.0;
    shadow += (proj.z - bias > texture(uShadowMap, proj.xy + vec2(0.0, -texel)).r) ? 0.30 : 1.0;
    return shadow * 0.20;
}

void main() {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(uCamPos - vWorldPos);
    vec3 L = normalize(uSunDir);
    vec3 H = normalize(L + V);
    vec3 R = reflect(-V, N);

    int mat = int(vMatType + 0.5);
    vec3 baseCol = vColor;
    float roughness = 0.65;
    float metalness = 0.05;
    float clearcoat = 0.0;
    float clearcoatRoughness = 0.15;
    vec3 emissive = vec3(0.0);

    if (mat == 1) {
        // High-res procedural asphalt road texture
        vec3 roadSample = texture(uAsphaltTex, vUv).rgb;
        baseCol = roadSample * (0.9 + 0.15 * vColor);
        roughness = 0.72;
        metalness = 0.04;
    } else if (mat == 2) {
        // Architectural glass skyscraper curtain wall texture
        if (abs(N.y) < 0.5) {
            vec3 winSample = texture(uSkyscraperTex, vUv * vec2(0.08, 0.06)).rgb;
            baseCol = winSample;
            roughness = 0.10;
            metalness = 0.78;
            clearcoat = 0.85;
            clearcoatRoughness = 0.06;
            // Lit warm gold or cyan office panes emit soft interior glow
            float luma = dot(winSample, vec3(0.3, 0.6, 0.1));
            if (luma > 0.48) {
                emissive = winSample * 0.78;
            }
        } else {
            baseCol = vec3(0.14, 0.16, 0.22);
            roughness = 0.55;
        }
    } else if (mat == 3) {
        // Metallic gold / chrome trim & spoked alloy rims
        roughness = 0.14;
        metalness = 0.94;
        clearcoat = 0.65;
        clearcoatRoughness = 0.05;
    } else if (mat == 4) {
        // Neon emissive (tunnel rings, boost chevrons, headlights, streetlamp bulbs)
        float pulse = 0.86 + 0.14 * sin(uTime * 5.2 + vWorldPos.z * 0.28);
        emissive = vColor * 1.65 * pulse;
        roughness = 0.18;
    } else if (mat == 5) {
        // Tinted glass canopy
        baseCol = vec3(0.03, 0.05, 0.09);
        roughness = 0.04;
        metalness = 0.88;
        clearcoat = 1.0;
        clearcoatRoughness = 0.03;
    } else if (mat == 6) {
        // MeshPhysicalMaterial-grade Car Paint with Clearcoat
        baseCol = uPaintColor;
        roughness = uPaintRoughness;
        metalness = uPaintMetalness;
        clearcoat = uPaintClearcoat;
        clearcoatRoughness = uPaintClearcoatRoughness;
    } else if (mat == 7) {
        // Reactive rear brake light bars
        emissive = vec3(1.0, 0.07, 0.09) * (0.48 + uBrakeGlow * 2.4);
        baseCol = vec3(0.32, 0.02, 0.03);
    } else if (mat == 8) {
        // Car signature LED accent strips
        emissive = uAccentColor * 1.85;
        baseCol = uAccentColor;
    } else if (mat == 9) {
        // Reactive reverse lights
        emissive = vec3(0.95, 0.98, 1.0) * (0.12 + uReverseGlow * 2.2);
        baseCol = vec3(0.75, 0.78, 0.82);
    } else if (mat == 10) {
        // Red/White racing curbs & barriers
        baseCol = texture(uCurbTex, vUv).rgb;
        roughness = 0.52;
    } else if (mat == 11) {
        // Painted brake calipers
        baseCol = uCaliperColor;
        roughness = 0.25;
        metalness = 0.75;
        clearcoat = 0.6;
    }

    float NdotL = max(dot(N, L), 0.0);
    float shadow = calcShadow(vShadowCoord, N, L);

    vec3 sunCol = vec3(1.0, 0.78, 0.50) * 1.45;
    vec3 ambSky = mix(vec3(0.18, 0.20, 0.34), vec3(0.58, 0.39, 0.43), N.y * 0.5 + 0.5);

    vec3 diffuse = baseCol * (1.0 - metalness * 0.78) * (ambSky * 0.62 + sunCol * NdotL * shadow);

    // Base specular lobe
    float specPow = mix(160.0, 14.0, roughness);
    float spec = pow(max(dot(N, H), 0.0), specPow) * mix(0.28, 1.55, 1.0 - roughness);
    vec3 specCol = mix(vec3(1.0, 0.88, 0.72), baseCol, metalness) * spec * shadow;

    // Dedicated Clearcoat specular lobe
    float ccPow = mix(280.0, 24.0, clearcoatRoughness);
    float ccSpec = pow(max(dot(N, H), 0.0), ccPow) * clearcoat * 1.35 * shadow;
    specCol += vec3(1.0, 0.94, 0.86) * ccSpec;

    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 4.0);
    vec3 envRef = evalSunsetEnv(R);
    float refWeight = mix(0.04, 0.88, metalness) * (1.0 - roughness * 0.55) + clearcoat * (0.16 + fresnel * 0.70);
    vec3 reflection = envRef * clamp(refWeight, 0.0, 0.96);

    vec3 color = diffuse + specCol + reflection + emissive;

    // Exponential horizon fog (<fogExp2 />)
    float dist = length(vWorldPos.xz - uCamPos.xz);
    float fogDensity = 0.0021;
    float fogFactor = 1.0 - exp(-fogDensity * fogDensity * dist * dist);
    color = mix(color, vec3(0.95, 0.56, 0.34), clamp(fogFactor, 0.0, 0.88));

    color = pow(acesFilmic(color), vec3(1.0 / 1.08));
    fragColor = vec4(color, 1.0);
}
"""

    const val PARTICLE_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec4 aColor;
layout(location = 2) in vec2 aUv;
uniform mat4 uViewProj;
out vec4 vColor;
out vec2 vUv;
void main() {
    vColor = aColor;
    vUv = aUv;
    gl_Position = uViewProj * vec4(aPos, 1.0);
}
"""

    const val PARTICLE_FS = """#version 300 es
precision mediump float;
in vec4 vColor;
in vec2 vUv;
out vec4 fragColor;
void main() {
    // Soft radial falloff when UV is in smoke puff mode (vUv.x >= 0.0)
    float alpha = vColor.a;
    if (vUv.x >= 0.0) {
        vec2 d = vUv * 2.0 - 1.0;
        float r2 = dot(d, d);
        alpha *= smoothstep(1.0, 0.1, r2);
    }
    if (alpha < 0.01) discard;
    fragColor = vec4(vColor.rgb, alpha);
}
"""
}

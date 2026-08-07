package com.mineralord.tcg.core.designsystem.tilt

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Holo por acabado ([finish]) sobre la carta activa, dirigido por [tilt]. AGSL/RuntimeShader
 * (Android 13+/API 33) como [RenderEffect] que muestrea el arte (`uContent`).
 *
 * Si se pasa una **máscara de foil REAL** ([mask], la capa `foil` de TCGL/malie.io), el
 * shader la usa como cobertura exacta (fidelidad tipo pokebox). Si es `null`, cae a una
 * **máscara procedimental** guiada por la luminancia del arte. En <33, o [Finish.NONE],
 * es un no-op.
 */
fun Modifier.holoOverlay(
    tilt: Tilt,
    finish: Finish,
    mask: Bitmap? = null,
    etch: Bitmap? = null,
    foilType: Float = -1f,
    intensity: Float = 1f,
): Modifier =
    if (finish == Finish.NONE || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) this
    else holoOverlay33(tilt, finish.shaderProfile, mask, etch, foilType, intensity)

private const val HOLO_AGSL = """
    uniform shader uContent;   // arte de la carta
    uniform shader uMask;      // máscara de foil real (o dummy 1x1)
    uniform float uHasMask;    // 1 si uMask es real
    uniform float2 uMaskScale; // tamaño máscara / tamaño capa
    uniform shader uEtch;      // capa etch (grabado) real (o dummy 1x1)
    uniform float uHasEtch;    // 1 si uEtch es real
    uniform float2 uEtchScale;
    uniform float2 uSize;
    uniform float2 uTilt;
    uniform float uIntensity;
    uniform float uProfile;
    uniform float uFoilType;   // 0 flat_silver,1 sun_pillar,2 sv_holo,3 sv_ultra, -1 none
    uniform float uTime;

    float hash(float2 p) {
        p = fract(p * float2(123.34, 456.21));
        p += dot(p, p + 45.32);
        return fract(p.x * p.y);
    }
    half3 rainbow(float t) {
        return half3(0.5 + 0.5 * cos(6.2831853 * (t + float3(0.0, 0.33, 0.67))));
    }
    float glitter(float2 uv, float scale) {
        float2 g = uv * scale;
        float h = hash(floor(g));
        float tw = 0.5 + 0.5 * sin(6.2831853 * h + uTime * 3.0);
        float2 f = fract(g) - 0.5;
        return step(0.86, h) * tw * smoothstep(0.35, 0.0, length(f));
    }

    half4 main(float2 fragCoord) {
        half4 src = uContent.eval(fragCoord);
        float2 uv = fragCoord / uSize;

        float bt = (uv.x + uv.y) + uTilt.x * 0.7 - uTilt.y * 0.7 + uTime * 0.03;

        float freq = 6.0; float sat = 1.0; float sparkleAmt = 0.0;
        float goldMix = 0.0; float metal = 0.0; float baseCover = 0.3; float amp = 1.0;
        if (uProfile < 0.5)      { freq = 10.0; sat = 0.7; sparkleAmt = 0.10; baseCover = 0.10; amp = 0.45; }
        else if (uProfile < 1.5) { freq = 7.0;  sat = 1.0; sparkleAmt = 0.12; baseCover = 0.14; amp = 0.55; }
        else if (uProfile < 2.5) { freq = 5.0;  sat = 0.9; sparkleAmt = 0.16; baseCover = 0.28; amp = 0.55; metal = 1.0; }
        else if (uProfile < 3.5) { freq = 5.0;  sat = 1.0; sparkleAmt = 0.20; baseCover = 0.30; amp = 0.60; metal = 1.0; }
        else if (uProfile < 4.5) { freq = 9.0;  sat = 1.1; sparkleAmt = 0.26; baseCover = 0.34; amp = 0.55; }
        else if (uProfile < 5.5) { freq = 12.0; sat = 1.2; sparkleAmt = 0.34; baseCover = 0.40; amp = 0.65; }
        else                     { freq = 6.0;  sat = 1.0; sparkleAmt = 0.30; baseCover = 0.34; amp = 0.55; goldMix = 1.0; }

        half3 rb = rainbow(bt * freq * 0.15);
        // Sheen metálico SUAVE (no rejilla dura): modula el brillo del arcoíris.
        float m1 = 0.5 + 0.5 * sin((uv.x - uv.y) * 55.0 + uTilt.x * 8.0);
        float m2 = 0.5 + 0.5 * sin((uv.x + uv.y) * 55.0 - uTilt.y * 8.0);
        float metalMod = 0.6 + 0.4 * (m1 * m2);
        half3 base = mix(rb, rb * half3(metalMod), metal);
        half3 gold = half3(1.0, 0.82, 0.28);
        base = mix(base, gold * (0.5 + 0.5 * base.r), goldMix);
        float g = dot(base, half3(0.3333));
        base = mix(half3(g), base, sat);

        // Carácter de color por TIPO de foil real (verdad de origen).
        if (uFoilType > -0.5) {
            if (uFoilType < 0.5) {            // FLAT_SILVER: plateado, poco saturado
                base = mix(base, half3(dot(base, half3(0.3333))), 0.75);
            } else if (uFoilType < 1.5) {     // SUN_PILLAR: pilares verticales cálidos
                float pillars = 0.55 + 0.45 * sin(uv.x * 60.0 + uTilt.x * 8.0);
                base = base * half3(pillars);
                base = mix(base, base * half3(1.15, 1.02, 0.80), 0.5);
            } else if (uFoilType > 2.5) {     // SV_ULTRA: muy saturado
                float l2 = dot(base, half3(0.3333));
                base = mix(half3(l2), base, 1.35);
            }
            // SV_HOLO (2): arcoíris estándar, sin cambio.
        }

        // Máscara procedimental (luminancia del arte).
        float lum = dot(src.rgb, half3(0.299, 0.587, 0.114));
        float artMask = smoothstep(0.28, 0.95, lum);
        float proc = clamp(baseCover + artMask * (1.0 - baseCover), 0.0, 1.0);
        // Máscara REAL (alfa de la capa foil).
        float real = uMask.eval(fragCoord * uMaskScale).a;
        float mask = mix(proc, real, uHasMask);

        float2 c = float2(0.5) + uTilt * 0.5;
        float glare = smoothstep(0.7, 0.0, distance(uv, c));

        float spark = glitter(uv, 60.0) * (0.4 + glare) * sparkleAmt * mask;

        half3 holo = base * half3(0.05 + glare * 0.45) * mask + half3(glare * 0.15 * mask);

        // Capa etch REAL (grabado): patrón oficial que titila con el tilt, sustituye al
        // glitter procedimental en las cartas grabadas (Ultra/SIR).
        float etchV = uEtch.eval(fragCoord * uEtchScale).a;
        float etchS = etchV * (0.45 + glare) * uHasEtch * mask;
        holo += base * half3(etchS) * 0.5;
        spark *= (1.0 - uHasEtch);

        // Mezcla tipo SCREEN: añade brillo pero NO blanquea las zonas ya claras del arte
        // (así el texto sigue legible aunque la máscara cubra toda la carta).
        half3 addc = (holo + half3(spark)) * amp * uIntensity;
        half3 col = src.rgb + addc * (half3(1.0) - src.rgb);
        return half4(col, src.a);
    }
"""

/**
 * Holo **sin giroscopio**: para colección, mano y tablero, donde hay muchas cartas a la vez y
 * no queremos el parallax 3D. El brillo lo mueve un barrido lento por tiempo (elíptico) en
 * lugar del tilt del dispositivo. Mismo shader que [holoOverlay]; en <33 o [Finish.NONE] es no-op.
 *
 * @param speed multiplicador de velocidad del barrido (1 = ~4 s por vuelta).
 */
fun Modifier.holoAmbient(
    finish: Finish,
    mask: Bitmap? = null,
    etch: Bitmap? = null,
    foilType: Float = -1f,
    intensity: Float = 1f,
    speed: Float = 1f,
): Modifier =
    if (finish == Finish.NONE || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) this
    else composed {
        val phase by produceState(0f) {
            val start = withFrameNanos { it }
            while (true) {
                withFrameNanos { now -> value = (now - start) / 1_000_000_000f * speed }
            }
        }
        // Barrido elíptico suave: recorre el frente como una luz ambiental que pasa.
        val sweep = Tilt(
            x = 0.6f * kotlin.math.sin(phase * 1.6f),
            y = 0.45f * kotlin.math.cos(phase * 1.2f),
        )
        holoOverlay33(sweep, finish.shaderProfile, mask, etch, foilType, intensity)
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Modifier.holoOverlay33(
    tilt: Tilt,
    profile: Float,
    mask: Bitmap?,
    etch: Bitmap?,
    foilType: Float,
    intensity: Float,
): Modifier = composed {
    val shader = remember { RuntimeShader(HOLO_AGSL) }
    val dummy = remember { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { setPixel(0, 0, -1) } }
    val maskShader = remember(mask) {
        BitmapShader(mask ?: dummy, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    val etchShader = remember(etch) {
        BitmapShader(etch ?: dummy, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    val time by produceState(0f) {
        val start = withFrameNanos { it }
        while (true) { withFrameNanos { now -> value = (now - start) / 1_000_000_000f } }
    }
    graphicsLayer {
        val mw = (mask?.width ?: 1).toFloat()
        val mh = (mask?.height ?: 1).toFloat()
        shader.setInputShader("uMask", maskShader)
        shader.setFloatUniform("uHasMask", if (mask != null) 1f else 0f)
        shader.setFloatUniform("uMaskScale", mw / size.width, mh / size.height)
        val ew = (etch?.width ?: 1).toFloat()
        val eh = (etch?.height ?: 1).toFloat()
        shader.setInputShader("uEtch", etchShader)
        shader.setFloatUniform("uHasEtch", if (etch != null) 1f else 0f)
        shader.setFloatUniform("uEtchScale", ew / size.width, eh / size.height)
        shader.setFloatUniform("uSize", size.width, size.height)
        shader.setFloatUniform("uTilt", tilt.x, tilt.y)
        shader.setFloatUniform("uIntensity", intensity)
        shader.setFloatUniform("uProfile", profile)
        shader.setFloatUniform("uFoilType", foilType)
        shader.setFloatUniform("uTime", time)
        renderEffect = RenderEffect
            .createRuntimeShaderEffect(shader, "uContent")
            .asComposeRenderEffect()
        clip = true
    }
}

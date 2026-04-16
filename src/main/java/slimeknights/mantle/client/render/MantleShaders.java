package slimeknights.mantle.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import lombok.Getter;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.config.Config;

import javax.annotation.Nullable;
import java.io.IOException;

/** Handles any custom shaders registered by Mantle. */
public class MantleShaders {
  @Nullable
  @Getter
  private static ShaderInstance blockFullBrightShader;
  @Nullable
  @Getter
  private static ShaderInstance fluidShader;

  /** Gets the shader to use for {@link MantleRenderTypes#FLUID_SHADER}, checking the config option to select which shader to use. */
  @Nullable
  public static ShaderInstance getConfiguredFluidShader() {
    return shouldUseCustomFluidShader() ? fluidShader : GameRenderer.getPositionColorTexLightmapShader();
  }

  /** Returns true if the custom fluid shader is safe to use in the current environment. */
  private static boolean shouldUseCustomFluidShader() {
    return Config.ENABLE_FLUID_FOG_FIX.get() && !isShaderPackActive();
  }

  /**
   * Iris/Oculus shader packs do not handle Mantle's custom fluid shader correctly.
   * If a shader pack is active, fall back to the vanilla shader automatically.
   */
  private static boolean isShaderPackActive() {
    if (!ModList.get().isLoaded("iris") && !ModList.get().isLoaded("oculus")) {
      return false;
    }
    try {
      Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
      Object irisApi = irisApiClass.getMethod("getInstance").invoke(null);
      Object active = irisApiClass.getMethod("isShaderPackInUse").invoke(irisApi);
      return active instanceof Boolean enabled && enabled;
    } catch (ReflectiveOperationException | LinkageError ignored) {
      // If the API is unavailable or shaded differently, prefer the safer fallback for shader mods.
      return true;
    }
  }

  @SubscribeEvent
  static void registerShaders(RegisterShadersEvent event) throws IOException {
    event.registerShader(
      new ShaderInstance(event.getResourceProvider(), Mantle.getResource("block_fullbright"), DefaultVertexFormat.BLOCK),
      shader -> blockFullBrightShader = shader
    );
    event.registerShader(
      new ShaderInstance(event.getResourceProvider(), Mantle.getResource("fluid"), DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
      shader -> fluidShader = shader
    );
  }
}

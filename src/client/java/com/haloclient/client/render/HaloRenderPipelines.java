package com.haloclient.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

public class HaloRenderPipelines {
    
    public static final VertexFormatElement CUSTOM_DATA = VertexFormatElement.register(7, 0, VertexFormatElement.Type.FLOAT, false, 4);
    public static final VertexFormatElement COLOR2 = VertexFormatElement.register(8, 0, VertexFormatElement.Type.UBYTE, true, 4);
    public static final VertexFormatElement SHADOW_PROPS = VertexFormatElement.register(9, 0, VertexFormatElement.Type.FLOAT, false, 4);
    
    public static final VertexFormat ROUNDED_RECT_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("CustomData", CUSTOM_DATA)
            .add("Color2", COLOR2)
            .add("ShadowProps", SHADOW_PROPS)
            .build();

    public static final RenderPipeline ROUNDED_RECT = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("halo", "pipeline/rounded_rect"))
                    .withVertexShader(ResourceLocation.fromNamespaceAndPath("halo", "core/rounded_rect"))
                    .withFragmentShader(ResourceLocation.fromNamespaceAndPath("halo", "core/rounded_rect"))
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
                    .build()
    );

    public static final RenderPipeline ROUNDED_BLUR = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("halo", "pipeline/rounded_blur"))
                    .withVertexShader(ResourceLocation.fromNamespaceAndPath("halo", "core/rounded_blur"))
                    .withFragmentShader(ResourceLocation.fromNamespaceAndPath("halo", "core/rounded_blur"))
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
                    .build()
    );

    public static final RenderPipeline LIQUID_GLASS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("halo", "pipeline/liquid_glass"))
                    .withVertexShader(ResourceLocation.fromNamespaceAndPath("halo", "core/liquid_glass"))
                    .withFragmentShader(ResourceLocation.fromNamespaceAndPath("halo", "core/liquid_glass"))
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
                    .build()
    );

    public static final RenderPipeline FONT = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("halo", "pipeline/font"))
                    .withVertexShader(ResourceLocation.fromNamespaceAndPath("halo", "core/font"))
                    .withFragmentShader(ResourceLocation.fromNamespaceAndPath("halo", "core/font"))
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
                    .build()
    );

    public static final RenderPipeline MSDF_FONT = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("halo", "pipeline/msdf_font"))
                    .withVertexShader(ResourceLocation.fromNamespaceAndPath("halo", "core/msdf_font"))
                    .withFragmentShader(ResourceLocation.fromNamespaceAndPath("halo", "core/msdf_font"))
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
                    .build()
    );


    public static final RenderPipeline TEXTURE_BLUR = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("halo", "pipeline/texture_blur"))
                    .withVertexShader(ResourceLocation.fromNamespaceAndPath("halo", "core/texture_blur"))
                    .withFragmentShader(ResourceLocation.fromNamespaceAndPath("halo", "core/texture_blur"))
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
                    .build()
    );

    public static final RenderPipeline IMAGE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("halo", "pipeline/image"))
                    .withVertexShader(ResourceLocation.fromNamespaceAndPath("halo", "core/image"))
                    .withFragmentShader(ResourceLocation.fromNamespaceAndPath("halo", "core/image"))
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
                    .build()
    );

    public static final RenderPipeline SPINNER = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath("halo", "pipeline/spinner"))
                    .withVertexShader(ResourceLocation.fromNamespaceAndPath("halo", "core/rounded_rect"))
                    .withFragmentShader(ResourceLocation.fromNamespaceAndPath("halo", "core/spinner"))
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
                    .build()
    );

    public static void init() {
    }
}

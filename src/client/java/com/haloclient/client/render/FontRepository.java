package com.haloclient.client.render;


import java.io.InputStream;
import java.util.HashMap;

public final class FontRepository {

    private static final HashMap<String, NVGTextRenderer> TEXT_RENDERER_MAP = new HashMap<>();

    public static NVGTextRenderer getFont(final String name) {
        if (TEXT_RENDERER_MAP.containsKey(name))
            return TEXT_RENDERER_MAP.get(name);

        final InputStream pathURL = FontRepository.class.getResourceAsStream("/assets/halo/fonts/" + name + ".ttf");

        if (pathURL != null) {
            TEXT_RENDERER_MAP.put(name, new NVGTextRenderer(name, pathURL));

            return TEXT_RENDERER_MAP.get(name);
        }

        throw new RuntimeException("Font not found: " + name);
    }
}
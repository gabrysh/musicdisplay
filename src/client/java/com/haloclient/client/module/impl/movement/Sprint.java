package com.haloclient.client.module.impl.movement;

import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Always sprints", Category.MOVEMENT);
        setKey(GLFW.GLFW_KEY_V);
    }

    // Actual logic would be in a Tick event, but for now we just show the structure
}

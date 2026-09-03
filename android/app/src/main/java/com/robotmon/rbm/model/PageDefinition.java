package com.robotmon.rbm.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Ported from an entry of the {@code Page} table in index.js. */
public class PageDefinition {
    public final String key;
    public final String name;
    public final List<PageColorSample> colors;
    public final ButtonPoint back;
    public final ButtonPoint next;
    /** Extra named buttons some pages expose, e.g. "tsums" or "store". */
    public final Map<String, ButtonPoint> extras;
    /** Side-effect run when this page is first detected. Only "switchToStartupMode" is used today. */
    public final String onDetectAction;

    public PageDefinition(String key, String name, List<PageColorSample> colors,
                          ButtonPoint back, ButtonPoint next) {
        this(key, name, colors, back, next, Collections.emptyMap(), null);
    }

    public PageDefinition(String key, String name, List<PageColorSample> colors,
                          ButtonPoint back, ButtonPoint next, Map<String, ButtonPoint> extras,
                          String onDetectAction) {
        this.key = key;
        this.name = name;
        this.colors = colors;
        this.back = back;
        this.next = next;
        this.extras = extras;
        this.onDetectAction = onDetectAction;
    }

    public ButtonPoint extra(String name) {
        return extras.get(name);
    }
}
package com.robotmon.rbm.bot;

/**
 * One row of the settings table, ported from an entry in index.html's
 * {@code settings} array: a storage/UI key, a human-readable title, a type
 * that decides which widget renders it, and (for numbers) a step/min/max
 * range or (for dropdowns) the list of choices.
 */
public class SettingDef {

    public enum Type { BOOLEAN, NUMBER, DROPDOWN }

    public final String key;
    public final String title;
    public final Type type;

    public final boolean defaultBoolean;
    public final int defaultNumber;
    public final int min;
    public final int max;
    public final int step;
    public final String defaultDropdownKey;
    public final String[] dropdownKeys;
    public final String[] dropdownTitles;

    private SettingDef(String key, String title, Type type, boolean defaultBoolean, int defaultNumber,
                       int min, int max, int step, String defaultDropdownKey,
                       String[] dropdownKeys, String[] dropdownTitles) {
        this.key = key;
        this.title = title;
        this.type = type;
        this.defaultBoolean = defaultBoolean;
        this.defaultNumber = defaultNumber;
        this.min = min;
        this.max = max;
        this.step = step;
        this.defaultDropdownKey = defaultDropdownKey;
        this.dropdownKeys = dropdownKeys;
        this.dropdownTitles = dropdownTitles;
    }

    public static SettingDef bool(String key, String title, boolean defaultValue) {
        return new SettingDef(key, title, Type.BOOLEAN, defaultValue, 0, 0, 0, 0, null, null, null);
    }

    public static SettingDef number(String key, String title, int defaultValue, int min, int max, int step) {
        return new SettingDef(key, title, Type.NUMBER, false, defaultValue, min, max, step, null, null, null);
    }

    public static SettingDef dropdown(String key, String title, String defaultKey, String[] keys, String[] titles) {
        return new SettingDef(key, title, Type.DROPDOWN, false, 0, 0, 0, 0, defaultKey, keys, titles);
    }
}
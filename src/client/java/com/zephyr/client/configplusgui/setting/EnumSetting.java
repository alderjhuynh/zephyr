package com.zephyr.client.configplusgui.setting;

public final class EnumSetting<T extends Enum<T>> extends Setting<T> {
    private final Class<T> enumType;
    private final T[] values;

    public EnumSetting(String name, T defaultValue) {
        super(name, defaultValue);
        this.enumType = defaultValue.getDeclaringClass();
        this.values = enumType.getEnumConstants();
    }

    public Class<T> getEnumType() {
        return enumType;
    }

    /** All constants of this setting's enum, in declaration order. */
    public T[] getValues() {
        return values;
    }

    public void set(T value) {
        setValue(value == null ? get() : value);
    }

    /** Advances to the next constant, wrapping back to the first after the last. */
    public void cycle() {
        T current = get();
        setValue(values[(current.ordinal() + 1) % values.length]);
    }

    /** Display label for the current value, e.g. {@code HASTE -> "Haste"}. */
    public String getDisplayValue() {
        return prettify(get().name());
    }

    private static String prettify(String enumName) {
        String[] parts = enumName.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }
}

package xyz.refinedev.api.storage.utils;

import lombok.experimental.UtilityClass;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * <p>
 * This class is the property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this class without permission is not allowed.<br>
 * </p>
 *
 * @author Drizzy
 * @version StorageAPI
 * @since 12/8/2024
 */

@UtilityClass
public class ReflectionUtils {

    private static final Logger LOGGER = LogManager.getLogger(ReflectionUtils.class);

    public Field getField(String[] split, Object instance) {
        try {
            Field field = instance.getClass().getField(toFieldName(split[split.length - 1]));
            setAccessible(field);
            return field;
        } catch (Exception e) {
            LOGGER.error("Error (Invalid Field): {}", e.getMessage(), e);
            return null;
        }
    }

    public String toFieldName(String node) {
        return node.toUpperCase().replaceAll("-", "_");
    }

    public String toNodeName(String field) {
        return field.toUpperCase().replace("_", "-");
    }

    public void setAccessible(Field field) {
        setAccessibleNonFinal(field);
    }

    /**
     * Make the given field accessible and remove the final modifier from it.
     *
     * @param field {@link Field field}
     */
    public void setAccessibleNonFinal(Field field) {
        // let's make the field accessible
        field.setAccessible(true);

        // next we change the modifier in the Field instance to
        // not be final anymore, thus tricking reflection into
        // letting us modify the static final field
        if (Modifier.isFinal(field.getModifiers())) {
            try {
                if (getVersion() > 11) {
                    // Requires ImagineBreaker to do this shit
                    // blank out the final bit in the modifiers int
                    ((MethodHandles.Lookup) LOOKUP_FIELD.get(null))
                            .findSetter(Field.class, "modifiers", int.class)
                            .invokeExact(field, field.getModifiers() & ~Modifier.FINAL);
                } else {
                    // Normal Java 8 reflection breaker
                    MODIFIERS_FIELD.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                }
            } catch (Throwable e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
    public static int getVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        } return Integer.parseInt(version);
    }

    private static Field LOOKUP_FIELD;
    private static Field MODIFIERS_FIELD;

    static {
        if (getVersion() > 11) {
            try {
                LOOKUP_FIELD = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP"); // This one has trusted access
                LOOKUP_FIELD.setAccessible(true);
            } catch (Throwable e) {
                System.out.println("[Carbon] Failed to find trusted lookup field.");
                e.printStackTrace();
            }
        } else {
            try {
                MODIFIERS_FIELD = Field.class.getDeclaredField("modifiers");
                MODIFIERS_FIELD.setAccessible(true);
            } catch (Throwable e) {
                System.out.println("[Carbon] Failed to find modifiers field.");
                e.printStackTrace();
            }
        }

    }
}

package com.sypztep.temporature.config;

import com.sypztep.temporature.Temporature;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigSyncUtil {
    private ConfigSyncUtil() {}
    private static final Map<Class<?>, List<Field>> SYNC_FIELDS = new ConcurrentHashMap<>();

    public static <T> void applyFrom(T src, T dst) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(dst, "dst");

        if (!src.getClass().equals(dst.getClass())) {
            throw new IllegalArgumentException("src and dst must be same class");
        }

        for (Field field : syncFields(src.getClass())) {
            try {
                field.set(dst, field.get(src));
            } catch (IllegalAccessException e) {
                Temporature.LOGGER.error(
                        "Failed to sync config field '{}'",
                        field.getName(),
                        e
                );
            }
        }
    }

    public static int syncHashCode(Object config) {
        Objects.requireNonNull(config, "config");

        int result = 1;

        for (Field field : syncFields(config.getClass())) {
            try {
                result = 31 * result + Objects.hashCode(field.get(config));
            } catch (IllegalAccessException e) {
                Temporature.LOGGER.error(
                        "Failed to hash config field '{}'",
                        field.getName(),
                        e
                );
            }
        }

        return result;
    }

    private static List<Field> syncFields(Class<?> type) {
        return SYNC_FIELDS.computeIfAbsent(type, ConfigSyncUtil::scanSyncFields);
    }

    private static List<Field> scanSyncFields(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RequireSync.class))
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .filter(f -> !Modifier.isFinal(f.getModifiers()))
                .sorted(Comparator.comparing(Field::getName))
                .peek(f -> f.setAccessible(true))
                .toList();
    }
}
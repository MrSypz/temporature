package com.sypztep.temporature.config;

import java.lang.annotation.*;

/**
 * Marks a config field to be included in server→client sync.
 * Fields with this annotation are automatically copied by
 * ConfigSyncUtil.applyFrom() and included in hashCode().
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RequireSync {}

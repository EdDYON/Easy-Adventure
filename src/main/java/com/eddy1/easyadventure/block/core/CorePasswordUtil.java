package com.eddy1.easyadventure.block.core;

import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class CorePasswordUtil {
    public static final int MAX_PASSWORD_LENGTH = 32;

    private CorePasswordUtil() {
    }

    public static @Nullable String normalize(@Nullable String password) {
        if (password == null) {
            return null;
        }
        String trimmed = password.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > MAX_PASSWORD_LENGTH ? trimmed.substring(0, MAX_PASSWORD_LENGTH) : trimmed;
    }

    public static @Nullable String hash(@Nullable UUID coreUuid, @Nullable String password) {
        String normalized = normalize(password);
        if (normalized == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (coreUuid != null) {
                digest.update(coreUuid.toString().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) ':');
            }
            digest.update(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public static boolean matches(@Nullable UUID coreUuid, @Nullable String password, @Nullable String expectedHash) {
        if (expectedHash == null || expectedHash.isBlank()) {
            return false;
        }
        String actualHash = hash(coreUuid, password);
        return actualHash != null && expectedHash.equals(actualHash);
    }
}

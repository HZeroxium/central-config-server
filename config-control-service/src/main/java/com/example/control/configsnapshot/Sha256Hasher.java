package com.example.control.configsnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Sha256Hasher {
  private Sha256Hasher() {}

  public static String hash(String canonical) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      return null;
    }
  }
}



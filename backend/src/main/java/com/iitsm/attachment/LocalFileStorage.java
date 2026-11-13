package com.iitsm.attachment;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorage implements FileStorage {
  private final Path root;

  public LocalFileStorage(@Value("${app.storage}") String p) throws IOException {
    root = Path.of(p).toAbsolutePath().normalize();
    Files.createDirectories(root);
  }

  private Path path(String key) {
    if (!key.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException("Invalid storage key");
    return root.resolve(key);
  }

  public String put(byte[] bytes) throws IOException {
    String key = UUID.randomUUID().toString();
    Files.write(path(key), bytes, StandardOpenOption.CREATE_NEW);
    return key;
  }

  public byte[] get(String key) throws IOException {
    return Files.readAllBytes(path(key));
  }

  public void delete(String key) throws IOException {
    Files.deleteIfExists(path(key));
  }
}

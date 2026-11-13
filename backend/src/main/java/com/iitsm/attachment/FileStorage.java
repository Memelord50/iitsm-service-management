package com.iitsm.attachment;

import java.io.IOException;

public interface FileStorage {
  String put(byte[] bytes) throws IOException;

  byte[] get(String key) throws IOException;

  void delete(String key) throws IOException;
}

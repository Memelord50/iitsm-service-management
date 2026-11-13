package com.iitsm.common;

public class Problem extends RuntimeException {
  public final int status;

  public Problem(int status, String message) {
    super(message);
    this.status = status;
  }

  public static void require(boolean ok, String message) {
    if (!ok) throw new Problem(422, message);
  }
}

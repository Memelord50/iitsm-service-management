package com.iitsm.assignment;

import java.util.Set;

public interface StrategyAdvisor {
  String recommend(String context, Set<String> enabled) throws Exception;
}

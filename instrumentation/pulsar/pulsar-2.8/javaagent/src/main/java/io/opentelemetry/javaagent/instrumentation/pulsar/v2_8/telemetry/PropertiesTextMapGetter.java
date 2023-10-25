/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry;

import io.opentelemetry.context.propagation.TextMapGetter;
import javax.annotation.Nullable;
import java.util.Map;

enum PropertiesTextMapGetter implements TextMapGetter<Map<String, String>> {
  INSTANCE;

  @Override
  public Iterable<String> keys(Map<String, String> carrier) {
    return carrier.keySet();
  }

  @Nullable
  @Override
  public String get(@Nullable Map<String, String> carrier, String key) {
    return null == carrier ? null : carrier.get(key);
  }
}

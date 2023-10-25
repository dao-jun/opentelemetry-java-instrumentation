/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pulsar.v2_8;

import static io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry.PulsarSingletons.extractContextFromProperties;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.pulsar.functions.api.Context;
import org.apache.pulsar.functions.api.Record;

// Enhance Pulsar function instances.
public class FunctionApiInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("org.apache.pulsar.functions.api.Function"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(named("process"))
            .and(takesArguments(2))
            .and(takesArgument(1, named("org.apache.pulsar.functions.api.Context"))),
        ConsumerImplInstrumentation.class.getName() + "$FunctionProcessAroundInterceptor");
  }

  @SuppressWarnings("unused")
  public static class FunctionProcessAroundInterceptor {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void before(@Advice.Argument(value = 1) Context context,
        @Advice.Local("otelScope") Scope otelScope) {
      Record<?> record = context.getCurrentRecord();
      if (record == null) {
        return;
      }

      Map<String, String> properties = record.getProperties();
      if (properties == null || properties.isEmpty()) {
        return;
      }

      io.opentelemetry.context.Context otelCtx = extractContextFromProperties(properties);
      otelScope = otelCtx.makeCurrent();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void after(@Advice.Local("otelScope") Scope otelScope) {
      if (otelScope != null) {
        otelScope.close();
      }
    }
  }
}

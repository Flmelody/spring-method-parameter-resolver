/*
 * Copyright (C) 2023 Flmelody.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flmelody.spring.web.standard;

import org.flmelody.spring.web.converter.EmptyHttpMessageConverter;
import org.flmelody.spring.web.converter.EnhancedHttpMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author esotericman
 */
public abstract class EnhancedMethodResolver extends AbstractMessageConverterMethodProcessor
    implements WebMethodArgumentResolver, InitializingBean {
  private static final Logger logger = LoggerFactory.getLogger(EnhancedMethodResolver.class);
  protected final ConfigurableListableBeanFactory configurableBeanFactory;

  protected final NamingStrategy namingStrategy;
  protected final List<ValueStrategy> valueStrategies;
  protected final List<NamingStrategyHandler> namingStrategyHandlers = new ArrayList<>();
  protected final List<ValueStrategyHandler> valueStrategyHandlers = new ArrayList<>();

  protected EnhancedMethodResolver(ConfigurableListableBeanFactory configurableBeanFactory) {
    this(configurableBeanFactory, null);
  }

  protected EnhancedMethodResolver(
      ConfigurableListableBeanFactory configurableBeanFactory, NamingStrategy namingStrategy) {
    this(configurableBeanFactory, Collections.emptyList(), namingStrategy, null);
  }

  protected EnhancedMethodResolver(
      ConfigurableListableBeanFactory configurableBeanFactory,
      NamingStrategy namingStrategy,
      List<ValueStrategy> valueStrategies) {
    this(configurableBeanFactory, Collections.emptyList(), namingStrategy, valueStrategies);
  }

  protected EnhancedMethodResolver(
      ConfigurableListableBeanFactory configurableBeanFactory,
      List<Object> requestResponseBodyAdvices,
      NamingStrategy namingStrategy,
      List<ValueStrategy> valueStrategies) {
    super(
        new ArrayList<>(Collections.singletonList(new EmptyHttpMessageConverter())),
        null,
        requestResponseBodyAdvices);
    this.configurableBeanFactory = configurableBeanFactory;
    this.namingStrategy = namingStrategy;
    this.valueStrategies = valueStrategies;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.configurableBeanFactory != null) {
      try {
        this.namingStrategyHandlers.addAll(
            configurableBeanFactory.getBeansOfType(NamingStrategyHandler.class).values().stream()
                .sorted(new AnnotationAwareOrderComparator())
                .collect(Collectors.toList()));
        this.valueStrategyHandlers.addAll(
            configurableBeanFactory.getBeansOfType(ValueStrategyHandler.class).values().stream()
                .sorted(new AnnotationAwareOrderComparator())
                .collect(Collectors.toList()));
        @SuppressWarnings("rawtypes")
        List<EnhancedHttpMessageConverter<?>> enhancedHttpMessageConverters =
            configurableBeanFactory
                .getBeansOfType(EnhancedHttpMessageConverter.class)
                .values()
                .stream()
                .sorted(new AnnotationAwareOrderComparator())
                .map(
                    (Function<EnhancedHttpMessageConverter, EnhancedHttpMessageConverter<?>>)
                        enhancedHttpMessageConverter ->
                            (EnhancedHttpMessageConverter<?>) enhancedHttpMessageConverter)
                .collect(Collectors.toList());
        this.messageConverters.addAll(enhancedHttpMessageConverters);
      } catch (BeansException ignored) {
      }
    }
  }

  protected final String convertNamingConversion(NamingStrategy namingStrategy, String name) {
    for (NamingStrategyHandler namingStrategyHandler : this.namingStrategyHandlers) {
      if (namingStrategyHandler.supportNamingStrategy(namingStrategy)) {
        return namingStrategyHandler.convertNamingConvention(name);
      }
    }
    logger.warn("No suitable naming converter was found, the default name will be used");
    return name;
  }

  protected final Object convertValue(
      MethodParameter parameter, ValueStrategy valueStrategy, Object value) {
    for (ValueStrategyHandler valueStrategyHandler : this.valueStrategyHandlers) {
      if (valueStrategyHandler.supportValueStrategy(parameter, valueStrategy)) {
        return valueStrategyHandler.convertValue(value);
      }
    }
    logger.warn("No suitable value converter was found, the default value will be used");
    return value;
  }
}

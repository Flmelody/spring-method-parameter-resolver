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

package org.flmelody.spring.web.configuration;

import org.flmelody.spring.web.annotation.EnableWebResolver;
import org.flmelody.spring.web.converter.DefaultGsonHttpMessageConverter;
import org.flmelody.spring.web.converter.DefaultMappingJackson2HttpMessageConverter;
import org.flmelody.spring.web.converter.DefaultStringHttpMessageConverter;
import org.flmelody.spring.web.resolver.WebBodyMethodArgumentResolverFactory;
import org.flmelody.spring.web.standard.NamingStrategy;
import org.flmelody.spring.web.standard.ValueStrategy;
import org.flmelody.spring.web.standard.WebHandlerMethodReturnValueHandler;
import org.flmelody.spring.web.standard.support.SnakeNamingStrategyHandler;
import org.flmelody.spring.web.standard.WebMethodArgumentResolver;
import org.flmelody.spring.web.resolver.WebParamMethodArgumentResolver;
import org.flmelody.spring.web.standard.util.DetectorUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import org.springframework.util.ObjectUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author esotericman
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer, BeanFactoryAware {
  private ConfigurableListableBeanFactory beanFactory;

  @Override
  public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
    if (beanFactory instanceof ConfigurableListableBeanFactory) {
      this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }
  }

  @Bean
  public DefaultStringHttpMessageConverter defaultStringHttpMessageConverter() {
    return new DefaultStringHttpMessageConverter();
  }

  @Bean
  public SnakeNamingStrategyHandler snakeNamingStrategyHandler() {
    return new SnakeNamingStrategyHandler();
  }

  @Override
  public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
    if (this.beanFactory == null) {
      return;
    }
    resolvers.addAll(beanFactory.getBeansOfType(WebMethodArgumentResolver.class).values());
  }

  @Override
  public void addReturnValueHandlers(@NonNull List<HandlerMethodReturnValueHandler> handlers) {
    if (this.beanFactory == null) {
      return;
    }
    handlers.addAll(beanFactory.getBeansOfType(WebHandlerMethodReturnValueHandler.class).values());
  }

  public static class WebConfigurationRegistrar
      implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

    private ConfigurableListableBeanFactory beanFactory;

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
      if (beanFactory instanceof ConfigurableListableBeanFactory) {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
      }
    }

    @Override
    public void registerBeanDefinitions(
        @NonNull AnnotationMetadata importingClassMetadata,
        @NonNull BeanDefinitionRegistry registry) {
      if (this.beanFactory == null) {
        return;
      }

      Map<String, Object> annotationAttributes =
          importingClassMetadata.getAnnotationAttributes(EnableWebResolver.class.getName());
      if (annotationAttributes == null) {
        return;
      }

      NamingStrategy namingStrategy =
          NamingStrategy.valueOf(String.valueOf(annotationAttributes.get("namingStrategy")));
      List<ValueStrategy> valueStrategies =
          new ArrayList<>(
              Arrays.asList((ValueStrategy[]) annotationAttributes.get("valueStrategies")));

      // Register json library if possibly
      tryRegisterJsonBeanIfMissing(registry, namingStrategy);

      registerSyntheticBeanIfMissing(
          registry,
          "webParamMethodArgumentResolver",
          WebParamMethodArgumentResolver.class,
          () ->
              new WebParamMethodArgumentResolver(
                  WebConfigurationRegistrar.this.beanFactory,
                  true,
                  namingStrategy,
                  valueStrategies));
      registerSyntheticBeanIfMissing(
          registry,
          "webBodyMethodArgumentResolverFactory",
          WebBodyMethodArgumentResolverFactory.class,
          () ->
              new WebBodyMethodArgumentResolverFactory(
                  WebConfigurationRegistrar.this.beanFactory, namingStrategy, valueStrategies));
    }

    private void tryRegisterJsonBeanIfMissing(
        BeanDefinitionRegistry registry, NamingStrategy globalNamingStrategy) {
      if (DetectorUtil.jacksonPresent) {
        registerSyntheticBeanIfMissing(
            registry,
            "jacksonMessageConverter",
            DefaultMappingJackson2HttpMessageConverter.class,
            () -> new DefaultMappingJackson2HttpMessageConverter(globalNamingStrategy));
      } else if (DetectorUtil.gsonPresent) {
        registerSyntheticBeanIfMissing(
            registry,
            "gsonMessageConverter",
            DefaultGsonHttpMessageConverter.class,
            () -> new DefaultGsonHttpMessageConverter(globalNamingStrategy));
      }
    }

    private <T> void registerSyntheticBeanIfMissing(
        BeanDefinitionRegistry registry,
        @NonNull String name,
        @NonNull Class<T> beanClass,
        Supplier<T> instanceSupplier) {
      if (ObjectUtils.isEmpty(this.beanFactory.getBeanNamesForType(beanClass, true, false))) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass, instanceSupplier);
        beanDefinition.setSynthetic(true);
        registry.registerBeanDefinition(name, beanDefinition);
      }
    }
  }
}

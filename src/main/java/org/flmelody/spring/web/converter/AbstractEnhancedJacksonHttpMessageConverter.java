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

package org.flmelody.spring.web.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.gson.Gson;
import org.flmelody.spring.web.standard.NamingStrategy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;

/**
 * @author esotericman
 */
public abstract class AbstractEnhancedJacksonHttpMessageConverter
    extends MappingJackson2HttpMessageConverter
    implements EnhancedHttpMessageConverter<Object>, BeanFactoryAware, InitializingBean {
  private final NamingStrategy namingStrategy;
  protected ConfigurableListableBeanFactory beanFactory;

  public AbstractEnhancedJacksonHttpMessageConverter(NamingStrategy namingStrategy) {
    super();
    this.namingStrategy = namingStrategy;
  }

  public AbstractEnhancedJacksonHttpMessageConverter(
      NamingStrategy namingStrategy, ObjectMapper objectMapper) {
    super(objectMapper);
    this.namingStrategy = namingStrategy;
  }

  @Override
  public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
    if (beanFactory instanceof ConfigurableListableBeanFactory) {
      this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (beanFactory != null) {
      try {
        this.setObjectMapper(
            beanFactory
                .getBean(ObjectMapper.class)
                .copy()
                .setPropertyNamingStrategy(transformStrategy()));
      } catch (BeansException ignored) {
      }
    }
  }

  private PropertyNamingStrategy transformStrategy() {
    if (NamingStrategy.NONE.equals(namingStrategy)) {
      return PropertyNamingStrategies.LOWER_CAMEL_CASE;
    }
    switch (namingStrategy) {
      case UPPER_CAMEL_CASE:
        return PropertyNamingStrategies.UPPER_CAMEL_CASE;
      case SNAKE_CASE:
        return PropertyNamingStrategies.SNAKE_CASE;
      default:
        return PropertyNamingStrategies.LOWER_CAMEL_CASE;
    }
  }

  @Override
  public int getOrder() {
    return 0;
  }
}

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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import org.flmelody.spring.web.standard.NamingStrategy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.lang.NonNull;

/**
 * @author esotericman
 */
public abstract class AbstractEnhancedGsonHttpMessageConverter extends GsonHttpMessageConverter
    implements EnhancedHttpMessageConverter<Object>, BeanFactoryAware, InitializingBean {

  private final NamingStrategy namingStrategy;
  protected ConfigurableListableBeanFactory beanFactory;

  public AbstractEnhancedGsonHttpMessageConverter(NamingStrategy namingStrategy) {
    super();
    this.namingStrategy = namingStrategy;
  }

  public AbstractEnhancedGsonHttpMessageConverter(NamingStrategy namingStrategy, Gson gson) {
    super(gson);
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
        this.setGson(
            beanFactory
                .getBean(Gson.class)
                .newBuilder()
                .setFieldNamingStrategy(transformStrategy())
                .create());
      } catch (BeansException ignored) {
      }
    }
  }

  private FieldNamingPolicy transformStrategy() {
    if (NamingStrategy.NONE.equals(namingStrategy)) {
      return FieldNamingPolicy.LOWER_CASE_WITH_DASHES;
    }
    switch (namingStrategy) {
      case UPPER_CAMEL_CASE:
        return FieldNamingPolicy.UPPER_CAMEL_CASE;
      case SNAKE_CASE:
        return FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
      default:
        return FieldNamingPolicy.IDENTITY;
    }
  }

  @Override
  public int getOrder() {
    return 0;
  }
}

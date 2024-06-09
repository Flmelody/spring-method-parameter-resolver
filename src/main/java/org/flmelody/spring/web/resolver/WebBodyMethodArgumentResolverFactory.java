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

package org.flmelody.spring.web.resolver;

import org.flmelody.spring.web.standard.NamingStrategy;
import org.flmelody.spring.web.standard.ValueStrategy;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * @author esotericman
 */
public class WebBodyMethodArgumentResolverFactory
    implements FactoryBean<WebBodyMethodArgumentResolver> {

  private final ConfigurableListableBeanFactory beanFactory;
  private final NamingStrategy namingStrategy;

  private final List<ValueStrategy> valueStrategies;

  public WebBodyMethodArgumentResolverFactory(
      ConfigurableListableBeanFactory configurableBeanFactory,
      NamingStrategy namingStrategy,
      List<ValueStrategy> valueStrategies) {
    this.beanFactory = configurableBeanFactory;
    this.namingStrategy = namingStrategy;
    this.valueStrategies = valueStrategies;
  }

  @Override
  public WebBodyMethodArgumentResolver getObject() throws Exception {
    if (beanFactory == null) {
      return null;
    }
    List<Object> requestResponseBodyAdvices = new ArrayList<>();
    requestResponseBodyAdvices.addAll(beanFactory.getBeansOfType(RequestBodyAdvice.class).values());
    requestResponseBodyAdvices.addAll(
        beanFactory.getBeansOfType(ResponseBodyAdvice.class).values());

    WebBodyMethodArgumentResolver webBodyMethodArgumentResolver =
        new WebBodyMethodArgumentResolver(
            beanFactory, requestResponseBodyAdvices, namingStrategy, valueStrategies);
    webBodyMethodArgumentResolver.afterPropertiesSet();
    return webBodyMethodArgumentResolver;
  }

  @Override
  public Class<?> getObjectType() {
    return WebBodyMethodArgumentResolver.class;
  }
}

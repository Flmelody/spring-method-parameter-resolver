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

import org.springframework.core.Ordered;

/**
 * Implement this interface to custom extend the naming conversion rules, of course, provided that
 * the scope of the existing naming rules, in addition you need to register the instance to the
 * BeanFactory.
 *
 * @author esotericman
 */
public interface NamingStrategyHandler extends Ordered {

  /**
   * Whether the current naming strategy is supported.
   *
   * @param namingStrategy namingStrategy
   * @return is it supported
   */
  boolean supportNamingStrategy(NamingStrategy namingStrategy);

  /**
   * Convert name to defined naming convention.
   *
   * @param name name
   * @return result name
   */
  String convertNamingConvention(String name);

  /**
   * default order for this handler
   *
   * @return order
   */
  @Override
  default int getOrder() {
    return 0;
  }
}

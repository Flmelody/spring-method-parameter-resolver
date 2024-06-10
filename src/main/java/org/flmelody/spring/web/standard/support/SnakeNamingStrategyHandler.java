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

package org.flmelody.spring.web.standard.support;

import org.flmelody.spring.web.standard.NamingStrategy;
import org.flmelody.spring.web.standard.NamingStrategyHandler;

/**
 * @author esotericman
 */
public class SnakeNamingStrategyHandler implements NamingStrategyHandler {

  @Override
  public boolean supportNamingStrategy(NamingStrategy namingStrategy) {
    return NamingStrategy.SNAKE_CASE.equals(namingStrategy);
  }

  @Override
  public String convertNamingConvention(String name) {
    if (name == null) return null; // garbage in, garbage out
    int length = name.length();
    StringBuilder result = new StringBuilder(length * 2);
    int resultLength = 0;
    boolean wasPrevTranslated = false;
    for (int i = 0; i < length; i++) {
      char c = name.charAt(i);
      if (i > 0 || c != '_') // skip first starting underscore
      {
        if (Character.isUpperCase(c)) {
          if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
            result.append('_');
            resultLength++;
          }
          c = Character.toLowerCase(c);
          wasPrevTranslated = true;
        } else {
          wasPrevTranslated = false;
        }
        result.append(c);
        resultLength++;
      }
    }
    return resultLength > 0 ? result.toString() : name;
  }
}

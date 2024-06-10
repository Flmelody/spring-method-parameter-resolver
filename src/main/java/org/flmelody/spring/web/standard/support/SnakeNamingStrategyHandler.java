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

import com.google.common.base.CaseFormat;
import org.flmelody.spring.web.standard.NamingStrategy;

import java.util.regex.Pattern;

/**
 * @author esotericman
 */
public class SnakeNamingStrategyHandler implements NamingStrategyHandler {
  /**
   * Lower camel pattern
   */
  protected static final Pattern LOWER_CAMEL = Pattern.compile("^[a-z]+[a-zA-Z|0-9]*[a-z|0-9]+$");
  /**
   * Upper camel pattern
   */
  protected static final Pattern UPPER_CAMEL = Pattern.compile("^[A-Z]+[a-zA-Z|0-9]*[a-z|0-9]+$");

  @Override
  public boolean supportNamingStrategy(NamingStrategy namingStrategy) {
    return NamingStrategy.SNAKE_CASE.equals(namingStrategy);
  }

  @Override
  public String convertNamingConvention(String value) {
    String result;
    if (LOWER_CAMEL.matcher(value).find()) {
      result = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert(value);
    } else if (UPPER_CAMEL.matcher(value).find()) {
      result = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert(value);
    } else {
      result = value;
    }
    return result == null ? value : result;
  }
}

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

import com.google.gson.Gson;
import org.flmelody.spring.web.standard.NamingStrategy;
import org.springframework.lang.NonNull;

import java.io.Writer;
import java.lang.reflect.Type;

/**
 * @author esotericman
 */
public class DefaultGsonHttpMessageConverter extends AbstractEnhancedGsonHttpMessageConverter {
  public DefaultGsonHttpMessageConverter(NamingStrategy namingStrategy) {
    super(namingStrategy);
  }

  public DefaultGsonHttpMessageConverter(NamingStrategy namingStrategy, Gson gson) {
    super(namingStrategy, gson);
  }

  @Override
  protected void writeInternal(@NonNull Object object, Type type, @NonNull Writer writer)
      throws Exception {
    super.writeInternal(object, type, writer);
  }
}

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

package org.flmelody.spring.web.standard.util;

import org.springframework.util.ClassUtils;

/**
 * @author esotericman
 */
public final class DetectorUtil {

  public static final boolean jacksonPresent;
  public static final boolean gsonPresent;

  static {
    ClassLoader classLoader = DetectorUtil.class.getClassLoader();
    jacksonPresent =
        ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader)
            && ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
    gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
  }

  private DetectorUtil() {}
}

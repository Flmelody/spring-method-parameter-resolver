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

package org.flmelody.spring.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.flmelody.spring.web.standard.NamingStrategy;
import org.flmelody.spring.web.standard.ValueStrategy;

/**
 * @author esotericman
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebRequestBody {

  /**
   * The naming strategy of request body fields
   *
   * @return naming strategy
   */
  NamingStrategy namingStrategy() default NamingStrategy.NONE;

  /**
   * Enable value strategy or not
   *
   * @return value strategy enabled status
   */
  boolean valueStrategy() default true;

  /**
   * Whether body content is required.
   *
   * <p>Default is {@code true}, leading to an exception thrown in case there is not body content.
   * Switch this to {@code false} if you prefer {@code null} to be passed when the body content is
   * {@code null}.
   *
   * @since 3.2
   * @return is it required
   */
  boolean required() default true;
}

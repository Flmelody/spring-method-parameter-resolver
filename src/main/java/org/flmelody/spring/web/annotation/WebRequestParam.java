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

import org.flmelody.spring.web.standard.NamingStrategy;
import org.flmelody.spring.web.standard.ValueStrategy;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Similar to @{@link RequestParam}, but with the additional parameter naming transformation.
 *
 * @author esotericman
 */
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebRequestParam {

  /**
   * Alias for {@link #name}.
   *
   * @return param name
   */
  @AliasFor("name")
  String value() default "";

  /**
   * The name of the request parameter to bind to.
   *
   * @since 4.2
   * @return param name
   */
  @AliasFor("value")
  String name() default "";

  /**
   * The naming strategy of request parameter name
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
   * Whether the parameter is required.
   *
   * <p>Defaults to {@code true}, leading to an exception being thrown if the parameter is missing
   * in the request. Switch this to {@code false} if you prefer a {@code null} value if the
   * parameter is not present in the request.
   *
   * <p>Alternatively, provide a {@link #defaultValue}, which implicitly sets this flag to {@code
   * false}.
   *
   * @return is it required
   */
  boolean required() default true;

  /**
   * The default value to use as a fallback when the request parameter is not provided or has an
   * empty value.
   *
   * <p>Supplying a default value implicitly sets {@link #required} to {@code false}.
   *
   * @return default value for this param
   */
  String defaultValue() default ValueConstants.DEFAULT_NONE;
}

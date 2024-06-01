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

/**
 * Pre-configured naming conventions, which of course I copied from Google Guava.
 *
 * @author esotericman
 */
public enum NamingStrategy {

  /** Default naming convention is none. */
  NONE,

  /** Naming convention, e.g., "lowerCamel". */
  LOWER_CAMEL_CASE,

  /** Naming convention, e.g., "UpperCamel". */
  UPPER_CAMEL_CASE,

  /** Naming convention, e.g., "snake_case" */
  SNAKE_CASE,

  /** Naming convention, e.g., "UPPER_UNDERSCORE". */
  UPPER_UNDERSCORE,

  /** Naming convention, e.g., "lower-hyphen". */
  LOWER_HYPHEN;
}

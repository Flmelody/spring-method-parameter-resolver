/*
 * Copyright (C) 2023 the original author or authors.
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

import org.flmelody.spring.web.annotation.WebRequestParam;
import org.flmelody.spring.web.annotation.WebRestController;
import org.flmelody.spring.web.standard.NamingStrategy;
import org.flmelody.spring.web.standard.ValueStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.RequestParamMapMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.Part;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @see org.springframework.web.method.annotation.RequestParamMethodArgumentResolver
 * @author esotericman
 */
public class WebParamMethodArgumentResolver extends AbstractWebParamMethodArgumentResolver {
  private static final Logger logger =
      LoggerFactory.getLogger(WebParamMethodArgumentResolver.class);
  private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);

  public WebParamMethodArgumentResolver(
      ConfigurableListableBeanFactory configurableBeanFactory, boolean useDefaultResolution) {
    super(configurableBeanFactory, useDefaultResolution);
  }

  public WebParamMethodArgumentResolver(
      ConfigurableListableBeanFactory configurableBeanFactory,
      boolean useDefaultResolution,
      NamingStrategy namingStrategy) {
    super(configurableBeanFactory, useDefaultResolution, namingStrategy);
  }

  public WebParamMethodArgumentResolver(
      ConfigurableListableBeanFactory configurableBeanFactory,
      boolean useDefaultResolution,
      NamingStrategy namingStrategy,
      List<ValueStrategy> valueStrategies) {
    super(configurableBeanFactory, useDefaultResolution, namingStrategy, valueStrategies);
  }

  /**
   * Supports the following:
   *
   * <ul>
   *   <li>@RequestParam-annotated method arguments. This excludes {@link Map} params where the
   *       annotation does not specify a name. See {@link RequestParamMapMethodArgumentResolver}
   *       instead for such params.
   *   <li>Arguments of type {@link MultipartFile} unless annotated with @{@link RequestPart}.
   *   <li>Arguments of type {@code Part} unless annotated with @{@link RequestPart}.
   *   <li>In default resolution mode, simple type arguments even if not with @{@link
   *       WebRequestParam}.
   * </ul>
   */
  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    if (parameter.hasParameterAnnotation(WebRequestParam.class)) {
      if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
        WebRequestParam requestParam = parameter.getParameterAnnotation(WebRequestParam.class);
        return (requestParam != null && StringUtils.hasText(requestParam.name()));
      } else {
        return true;
      }
    } else {
      if (parameter.hasParameterAnnotation(RequestPart.class)) {
        return false;
      }
      parameter = parameter.nestedIfOptional();
      if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
        return true;
      } else if (this.useDefaultResolution) {
        return BeanUtils.isSimpleProperty(parameter.getNestedParameterType());
      } else {
        return false;
      }
    }
  }

  @Override
  @NonNull
  protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
    WebRequestParam ann = parameter.getParameterAnnotation(WebRequestParam.class);
    return (ann != null ? new WebParamNamedValueInfo(ann) : new WebParamNamedValueInfo());
  }

  @Override
  protected Object handleResolvedValue(
      Object arg,
      String name,
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest) {
    WebRequestParam ann = parameter.getParameterAnnotation(WebRequestParam.class);
    if (AnnotatedElementUtils.hasAnnotation(
        parameter.getContainingClass(), WebRestController.class)) {
      WebRestController mergedAnnotation =
          AnnotatedElementUtils.findMergedAnnotation(
              parameter.getContainingClass(), WebRestController.class);
      Assert.notNull(mergedAnnotation, "WebRestController is null!");
      if (mergedAnnotation.valueStrategies().length > 0
          && Arrays.stream(mergedAnnotation.valueStrategies())
              .noneMatch(ValueStrategy.REQUEST_PARAM::equals)) {
        return arg;
      }
    }
    if (ann != null
        && valueStrategies.contains(ValueStrategy.REQUEST_PARAM)
        && ann.valueStrategy()) {
      return convertValue(parameter, ValueStrategy.REQUEST_PARAM, arg);
    }
    return arg;
  }

  @Override
  protected String resolveName(String name, MethodParameter parameter) {
    WebRequestParam ann = parameter.getParameterAnnotation(WebRequestParam.class);
    String actualName;
    if (ann != null && !StringUtils.hasText(ann.name())) {
      NamingStrategy strategy = ann.namingStrategy();
      if (NamingStrategy.NONE.equals(strategy)) {
        strategy = this.namingStrategy;
      }
      actualName = convertNamingConversion(strategy, name);
    } else {
      actualName = name;
    }
    return actualName;
  }

  @Override
  public void contributeMethodArgument(
      MethodParameter parameter,
      @Nullable Object value,
      @NonNull UriComponentsBuilder builder,
      @NonNull Map<String, Object> uriVariables,
      @Nullable ConversionService conversionService) {

    Class<?> paramType = parameter.getNestedParameterType();
    if (Map.class.isAssignableFrom(paramType)
        || MultipartFile.class == paramType
        || Part.class == paramType) {
      return;
    }

    WebRequestParam webRequestParam = parameter.getParameterAnnotation(WebRequestParam.class);
    String name =
        (webRequestParam != null && StringUtils.hasLength(webRequestParam.name())
            ? webRequestParam.name()
            : parameter.getParameterName());
    Assert.state(name != null, "Unresolvable parameter name");

    parameter = parameter.nestedIfOptional();
    if (value instanceof Optional) {
      value = ((Optional<?>) value).orElse(null);
    }

    if (value == null) {
      if (webRequestParam != null
          && (!webRequestParam.required()
              || !webRequestParam.defaultValue().equals(ValueConstants.DEFAULT_NONE))) {
        return;
      }
      builder.queryParam(name);
    } else if (value instanceof Collection) {
      for (Object element : (Collection<?>) value) {
        element = formatUriValue(conversionService, TypeDescriptor.nested(parameter, 1), element);
        builder.queryParam(name, element);
      }
    } else {
      builder.queryParam(
          name, formatUriValue(conversionService, new TypeDescriptor(parameter), value));
    }
  }

  @Nullable
  protected String formatUriValue(
      @Nullable ConversionService cs, @Nullable TypeDescriptor sourceType, @Nullable Object value) {

    if (value == null) {
      return null;
    } else if (value instanceof String) {
      return (String) value;
    } else if (cs != null) {
      return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
    } else {
      return value.toString();
    }
  }

  private static class WebParamNamedValueInfo extends NamedValueInfo {

    public WebParamNamedValueInfo() {
      super("", false, ValueConstants.DEFAULT_NONE);
    }

    public WebParamNamedValueInfo(WebRequestParam annotation) {
      super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
    }
  }
}

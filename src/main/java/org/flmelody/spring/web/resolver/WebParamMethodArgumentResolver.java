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

import org.flmelody.spring.web.annotation.WebParam;
import org.flmelody.spring.web.standard.NamingStrategy;
import org.flmelody.spring.web.standard.support.NamingStrategyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMapMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author esotericman
 * @since spring 5.3
 */
public class WebParamMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver
    implements WebMethodArgumentResolver, UriComponentsContributor, InitializingBean {
  private static final Logger logger =
      LoggerFactory.getLogger(WebParamMethodArgumentResolver.class);
  private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);

  private ConfigurableListableBeanFactory configurableBeanFactory;
  private final boolean useDefaultResolution;

  private final NamingStrategy namingStrategy;

  private final List<NamingStrategyHandler> namingStrategyHandlers = new ArrayList<>();

  public WebParamMethodArgumentResolver(
      NamingStrategy namingStrategy,
      ConfigurableListableBeanFactory beanFactory,
      boolean useDefaultResolution) {
    super(beanFactory);
    this.configurableBeanFactory = beanFactory;
    this.namingStrategy = namingStrategy;
    this.useDefaultResolution = useDefaultResolution;
  }

  /**
   * Create a new {@link RequestParamMethodArgumentResolver} instance.
   *
   * @param useDefaultResolution in default resolution mode a method argument that is a simple type,
   *     as defined in {@link BeanUtils#isSimpleProperty}, is treated as a request parameter even if
   *     it isn't annotated, the request parameter name is derived from the method parameter name.
   */
  public WebParamMethodArgumentResolver(boolean useDefaultResolution) {
    this.useDefaultResolution = useDefaultResolution;
    this.namingStrategy = null;
  }

  /**
   * Create a new {@link RequestParamMethodArgumentResolver} instance.
   *
   * @param beanFactory a bean factory used for resolving ${...} placeholder and #{...} SpEL
   *     expressions in default values, or {@code null} if default values are not expected to
   *     contain expressions
   * @param useDefaultResolution in default resolution mode a method argument that is a simple type,
   *     as defined in {@link BeanUtils#isSimpleProperty}, is treated as a request parameter even if
   *     it isn't annotated, the request parameter name is derived from the method parameter name.
   */
  public WebParamMethodArgumentResolver(
      @Nullable ConfigurableBeanFactory beanFactory, boolean useDefaultResolution) {

    super(beanFactory);
    this.useDefaultResolution = useDefaultResolution;
    this.namingStrategy = null;
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
   *   <li>In default resolution mode, simple type arguments even if not with @{@link WebParam}.
   * </ul>
   */
  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    if (parameter.hasParameterAnnotation(WebParam.class)) {
      if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
        WebParam requestParam = parameter.getParameterAnnotation(WebParam.class);
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
    WebParam ann = parameter.getParameterAnnotation(WebParam.class);
    return (ann != null ? new WebParamNamedValueInfo(ann) : new WebParamNamedValueInfo());
  }

  @Override
  @Nullable
  protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request)
      throws Exception {
    WebParam ann = parameter.getParameterAnnotation(WebParam.class);
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
    HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);

    if (servletRequest != null) {
      Object mpArg =
          MultipartResolutionDelegate.resolveMultipartArgument(
              actualName, parameter, servletRequest);
      if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
        return mpArg;
      }
    }

    Object arg = null;
    MultipartRequest multipartRequest = request.getNativeRequest(MultipartRequest.class);
    if (multipartRequest != null) {
      List<MultipartFile> files = multipartRequest.getFiles(actualName);
      if (!files.isEmpty()) {
        arg = (files.size() == 1 ? files.get(0) : files);
      }
    }
    if (arg == null) {
      String[] paramValues = request.getParameterValues(actualName);
      if (paramValues != null) {
        arg = (paramValues.length == 1 ? paramValues[0] : paramValues);
      }
    }
    return arg;
  }

  @Override
  protected void handleMissingValue(
      String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

    handleMissingValueInternal(name, parameter, request, false);
  }

  @Override
  protected void handleMissingValueAfterConversion(
      String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

    handleMissingValueInternal(name, parameter, request, true);
  }

  protected void handleMissingValueInternal(
      String name,
      MethodParameter parameter,
      NativeWebRequest request,
      boolean missingAfterConversion)
      throws Exception {

    HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
    if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
      if (servletRequest == null
          || !MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
        throw new MultipartException("Current request is not a multipart request");
      } else {
        throw new MissingServletRequestPartException(name);
      }
    } else {
      // Compatible with spring 5.3.6 before
      if (ClassUtils.hasConstructor(
          MissingServletRequestParameterException.class,
          String.class,
          String.class,
          boolean.class)) {
        throw new MissingServletRequestParameterException(
            name, parameter.getNestedParameterType().getSimpleName(), missingAfterConversion);
      }
      throw new MissingServletRequestParameterException(
          name, parameter.getNestedParameterType().getSimpleName());
    }
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

    WebParam webParam = parameter.getParameterAnnotation(WebParam.class);
    String name =
        (webParam != null && StringUtils.hasLength(webParam.name())
            ? webParam.name()
            : parameter.getParameterName());
    Assert.state(name != null, "Unresolvable parameter name");

    parameter = parameter.nestedIfOptional();
    if (value instanceof Optional) {
      value = ((Optional<?>) value).orElse(null);
    }

    if (value == null) {
      if (webParam != null
          && (!webParam.required()
              || !webParam.defaultValue().equals(ValueConstants.DEFAULT_NONE))) {
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

  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.configurableBeanFactory != null) {
      try {
        Map<String, NamingStrategyHandler> beansOfType =
            configurableBeanFactory.getBeansOfType(NamingStrategyHandler.class);
        this.namingStrategyHandlers.addAll(beansOfType.values());
      } catch (BeansException ignored) {
      }
    }
  }

  private String convertNamingConversion(NamingStrategy namingStrategy, String value) {
    for (NamingStrategyHandler namingStrategyHandler : this.namingStrategyHandlers) {
      if (namingStrategyHandler.supportNamingStrategy(namingStrategy)) {
        return namingStrategyHandler.convertNamingConvention(value);
      }
    }
    logger.warn("No suitable naming converter was found, the default value will be used");
    return value;
  }

  private static class WebParamNamedValueInfo extends NamedValueInfo {

    public WebParamNamedValueInfo() {
      super("", false, ValueConstants.DEFAULT_NONE);
    }

    public WebParamNamedValueInfo(WebParam annotation) {
      super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
    }
  }
}

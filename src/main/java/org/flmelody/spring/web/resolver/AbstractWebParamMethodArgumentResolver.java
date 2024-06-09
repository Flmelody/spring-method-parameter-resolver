/*
 * Copyright author or original authors.
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

import org.flmelody.spring.web.standard.EnhancedMethodResolver;
import org.flmelody.spring.web.standard.NamingStrategy;
import org.flmelody.spring.web.standard.ValueStrategy;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @see org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver
 * @author esotericman
 */
public abstract class AbstractWebParamMethodArgumentResolver extends EnhancedMethodResolver
    implements UriComponentsContributor {
  protected final boolean useDefaultResolution;
  @Nullable private BeanExpressionContext expressionContext;

  private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache =
      new ConcurrentHashMap<>(256);

  protected AbstractWebParamMethodArgumentResolver(
      ConfigurableListableBeanFactory configurableBeanFactory, boolean useDefaultResolution) {
    this(configurableBeanFactory, useDefaultResolution, null);
  }

  protected AbstractWebParamMethodArgumentResolver(
      ConfigurableListableBeanFactory configurableBeanFactory,
      boolean useDefaultResolution,
      NamingStrategy namingStrategy) {
    this(configurableBeanFactory, useDefaultResolution, namingStrategy, null);
  }

  protected AbstractWebParamMethodArgumentResolver(
      ConfigurableListableBeanFactory configurableBeanFactory,
      boolean useDefaultResolution,
      NamingStrategy namingStrategy,
      List<ValueStrategy> valueStrategies) {
    super(configurableBeanFactory, namingStrategy, valueStrategies);
    this.useDefaultResolution = useDefaultResolution;
  }

  protected String resolveName(String name, MethodParameter parameter) {
    return name;
  }

  @Override
  @Nullable
  public final Object resolveArgument(
      @NonNull MethodParameter parameter,
      @Nullable ModelAndViewContainer mavContainer,
      @NonNull NativeWebRequest webRequest,
      @Nullable WebDataBinderFactory binderFactory)
      throws Exception {

    NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
    MethodParameter nestedParameter = parameter.nestedIfOptional();

    Object resolvedName = resolveEmbeddedValuesAndExpressions(namedValueInfo.name);
    if (resolvedName == null) {
      throw new IllegalArgumentException(
          "Specified name must not resolve to null: [" + namedValueInfo.name + "]");
    }

    Object arg = resolveName(resolvedName.toString(), nestedParameter, webRequest);
    if (arg == null) {
      if (namedValueInfo.defaultValue != null) {
        arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
      } else if (namedValueInfo.required && !nestedParameter.isOptional()) {
        handleMissingValue(namedValueInfo.name, nestedParameter, webRequest);
      }
      arg = handleNullValue(namedValueInfo.name, arg, nestedParameter.getNestedParameterType());
    } else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
      arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
    }

    if (binderFactory != null) {
      WebDataBinder binder = binderFactory.createBinder(webRequest, null, namedValueInfo.name);
      try {
        arg = binder.convertIfNecessary(arg, parameter.getParameterType(), parameter);
      } catch (ConversionNotSupportedException ex) {
        throw new MethodArgumentConversionNotSupportedException(
            arg, ex.getRequiredType(), namedValueInfo.name, parameter, ex.getCause());
      } catch (TypeMismatchException ex) {
        throw new MethodArgumentTypeMismatchException(
            arg, ex.getRequiredType(), namedValueInfo.name, parameter, ex.getCause());
      }
      // Check for null value after conversion of incoming argument value
      if (arg == null
          && namedValueInfo.defaultValue == null
          && namedValueInfo.required
          && !nestedParameter.isOptional()) {
        handleMissingValueAfterConversion(namedValueInfo.name, nestedParameter, webRequest);
      }
    }

    return handleResolvedValue(arg, namedValueInfo.name, parameter, mavContainer, webRequest);

    //    return arg;
  }

  @Override
  public boolean supportsReturnType(@NonNull MethodParameter returnType) {
    return false;
  }

  @Override
  public void handleReturnValue(
      Object returnValue,
      @NonNull MethodParameter returnType,
      @NonNull ModelAndViewContainer mavContainer,
      @NonNull NativeWebRequest webRequest)
      throws Exception {}

  /** Obtain the named value for the given method parameter. */
  private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
    NamedValueInfo namedValueInfo = this.namedValueInfoCache.get(parameter);
    if (namedValueInfo == null) {
      namedValueInfo = createNamedValueInfo(parameter);
      namedValueInfo = updateNamedValueInfo(parameter, namedValueInfo);
      this.namedValueInfoCache.put(parameter, namedValueInfo);
    }
    return namedValueInfo;
  }

  /**
   * Create the {@link NamedValueInfo} object for the given method parameter. Implementations
   * typically retrieve the method annotation by means of {@link
   * MethodParameter#getParameterAnnotation(Class)}.
   *
   * @param parameter the method parameter
   * @return the named value information
   */
  protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);

  /** Create a new NamedValueInfo based on the given NamedValueInfo with sanitized values. */
  private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
    String name = info.name;
    if (info.name.isEmpty()) {
      name = parameter.getParameterName();
      if (name == null) {
        throw new IllegalArgumentException(
            "Name for argument of type ["
                + parameter.getNestedParameterType().getName()
                + "] not specified, and parameter name information not found in class file either.");
      }
    }
    String defaultValue =
        (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
    return new NamedValueInfo(name, info.required, defaultValue);
  }

  /**
   * Resolve the given annotation-specified value, potentially containing placeholders and
   * expressions.
   */
  @Nullable
  private Object resolveEmbeddedValuesAndExpressions(String value) {
    if (this.configurableBeanFactory == null || this.expressionContext == null) {
      return value;
    }
    String placeholdersResolved = this.configurableBeanFactory.resolveEmbeddedValue(value);
    BeanExpressionResolver exprResolver = this.configurableBeanFactory.getBeanExpressionResolver();
    if (exprResolver == null) {
      return value;
    }
    return exprResolver.evaluate(placeholdersResolved, this.expressionContext);
  }

  /**
   * Resolve the given parameter type and value name into an argument value.
   *
   * @param name the name of the value being resolved
   * @param parameter the method parameter to resolve to an argument value (pre-nested in case of a
   *     {@link java.util.Optional} declaration)
   * @param request the current request
   * @return the resolved argument (may be {@code null})
   * @throws Exception in case of errors
   */
  @Nullable
  protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request)
      throws Exception {
    String actualName = resolveName(name, parameter);
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

  /**
   * Invoked when a named value is required, but {@link #resolveName(String, MethodParameter,
   * NativeWebRequest)} returned {@code null} and there is no default value. Subclasses typically
   * throw an exception in this case.
   *
   * @param name the name for the value
   * @param parameter the method parameter
   * @param request the current request
   * @since 4.3
   */
  protected void handleMissingValue(
      String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

    handleMissingValueInternal(name, parameter, request, false);
  }

  /**
   * Invoked when a named value is required, but {@link #resolveName(String, MethodParameter,
   * NativeWebRequest)} returned {@code null} and there is no default value. Subclasses typically
   * throw an exception in this case.
   *
   * @param name the name for the value
   * @param parameter the method parameter
   */
  protected void handleMissingValue(String name, MethodParameter parameter)
      throws ServletException {
    throw new ServletRequestBindingException(
        "Missing argument '"
            + name
            + "' for method parameter of type "
            + parameter.getNestedParameterType().getSimpleName());
  }

  /**
   * Invoked when a named value is present but becomes {@code null} after conversion.
   *
   * @param name the name for the value
   * @param parameter the method parameter
   * @param request the current request
   * @since 5.3.6
   */
  protected void handleMissingValueAfterConversion(
      String name, MethodParameter parameter, NativeWebRequest request) throws Exception {

    handleMissingValue(name, parameter, request);
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

  /**
   * A {@code null} results in a {@code false} value for {@code boolean}s or an exception for other
   * primitives.
   */
  @Nullable
  private Object handleNullValue(String name, @Nullable Object value, Class<?> paramType) {
    if (value == null) {
      if (Boolean.TYPE.equals(paramType)) {
        return Boolean.FALSE;
      } else if (paramType.isPrimitive()) {
        throw new IllegalStateException(
            "Optional "
                + paramType.getSimpleName()
                + " parameter '"
                + name
                + "' is present but cannot be translated into a null value due to being declared as a "
                + "primitive type. Consider declaring it as object wrapper for the corresponding primitive type.");
      }
    }
    return value;
  }

  /**
   * Invoked after a value is resolved.
   *
   * @param arg the resolved argument value
   * @param name the argument name
   * @param parameter the argument parameter type
   * @param mavContainer the {@link ModelAndViewContainer} (may be {@code null})
   * @param webRequest the current request
   */
  protected Object handleResolvedValue(
      @Nullable Object arg,
      String name,
      MethodParameter parameter,
      @Nullable ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest) {
    return arg;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    super.afterPropertiesSet();
    this.expressionContext =
        (this.configurableBeanFactory != null
            ? new BeanExpressionContext(configurableBeanFactory, new RequestScope())
            : null);
  }

  /**
   * Represents the information about a named value, including name, whether it's required and a
   * default value.
   */
  protected static class NamedValueInfo {

    private final String name;

    private final boolean required;

    @Nullable private final String defaultValue;

    public NamedValueInfo(String name, boolean required, @Nullable String defaultValue) {
      this.name = name;
      this.required = required;
      this.defaultValue = defaultValue;
    }
  }
}

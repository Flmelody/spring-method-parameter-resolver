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

import org.flmelody.spring.web.annotation.WebRequestBody;
import org.flmelody.spring.web.annotation.WebResponseBody;
import org.flmelody.spring.web.standard.EnhancedMethodResolver;
import org.flmelody.spring.web.standard.NamingStrategy;
import org.flmelody.spring.web.standard.ValueStrategy;
import org.flmelody.spring.web.standard.WebHandlerMethodReturnValueHandler;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author esotericman
 */
public class WebBodyMethodArgumentResolver extends EnhancedMethodResolver
    implements WebHandlerMethodReturnValueHandler {
  public WebBodyMethodArgumentResolver(ConfigurableListableBeanFactory configurableBeanFactory) {
    super(configurableBeanFactory);
  }

  public WebBodyMethodArgumentResolver(
      ConfigurableListableBeanFactory configurableBeanFactory, NamingStrategy namingStrategy) {
    super(configurableBeanFactory, namingStrategy);
  }

  public WebBodyMethodArgumentResolver(
      ConfigurableListableBeanFactory configurableBeanFactory,
      List<Object> requestResponseBodyAdvices,
      NamingStrategy namingStrategy,
      List<ValueStrategy> valueStrategies) {
    super(configurableBeanFactory, requestResponseBodyAdvices, namingStrategy, valueStrategies);
  }

  @Override
  public boolean isAsyncReturnValue(Object returnValue, @NonNull MethodParameter returnType) {
    return supportsReturnType(returnType);
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(WebRequestBody.class);
  }

  @Override
  public boolean supportsReturnType(MethodParameter returnType) {
    return (AnnotatedElementUtils.hasAnnotation(
            returnType.getContainingClass(), WebResponseBody.class)
        || returnType.hasMethodAnnotation(WebResponseBody.class));
  }

  @Override
  public Object resolveArgument(
      @NonNull MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      @NonNull NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory)
      throws Exception {
    parameter = parameter.nestedIfOptional();
    Object arg =
        readWithMessageConverters(webRequest, parameter, parameter.getNestedGenericParameterType());
    String name = Conventions.getVariableNameForParameter(parameter);

    if (binderFactory != null) {
      WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
      if (arg != null) {
        validateIfApplicable(binder, parameter);
        if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
          throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
        }
      }
      if (mavContainer != null) {
        mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
      }
    }

    return adaptArgumentIfNecessary(arg, parameter);
  }

  @Override
  protected <T> Object readWithMessageConverters(
      NativeWebRequest webRequest, @NonNull MethodParameter parameter, @NonNull Type paramType)
      throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {

    HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
    Assert.state(servletRequest != null, "No HttpServletRequest");
    ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(servletRequest);

    Object arg = readWithMessageConverters(inputMessage, parameter, paramType);
    if (arg == null && checkRequired(parameter)) {
      throw new HttpMessageNotReadableException(
          "Required request body is missing: " + parameter.getExecutable().toGenericString(),
          inputMessage);
    }
    return arg;
  }

  protected boolean checkRequired(MethodParameter parameter) {
    WebRequestBody requestBody = parameter.getParameterAnnotation(WebRequestBody.class);
    return (requestBody != null && requestBody.required() && !parameter.isOptional());
  }

  @Override
  public void handleReturnValue(
      Object returnValue,
      @NonNull MethodParameter returnType,
      @NonNull ModelAndViewContainer mavContainer,
      @NonNull NativeWebRequest webRequest)
      throws Exception {
    mavContainer.setRequestHandled(true);
    ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
    ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

    // Try even with null return value. ResponseBodyAdvice could get involved.
    writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);
  }
}

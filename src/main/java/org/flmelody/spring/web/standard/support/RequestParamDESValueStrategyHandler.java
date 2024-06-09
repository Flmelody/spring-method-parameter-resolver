package org.flmelody.spring.web.standard.support;

import org.flmelody.spring.web.annotation.WebRequestParam;
import org.flmelody.spring.web.standard.ValueStrategy;
import org.flmelody.spring.web.standard.ValueStrategyHandler;
import org.springframework.core.MethodParameter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * @author esotericman
 */
public class RequestParamDESValueStrategyHandler implements ValueStrategyHandler {
  private static final String DES_ALGORITHM = "DES";
  private final String key;

  public RequestParamDESValueStrategyHandler(String key) {
    this.key = key;
  }

  @Override
  public boolean supportValueStrategy(MethodParameter parameter, ValueStrategy valueStrategy) {
    return parameter.hasParameterAnnotation(WebRequestParam.class);
  }

  @Override
  public <R, T> R convertValue(T value) {
    if (!(value instanceof String)) {
      //noinspection unchecked
      return (R) value;
    }
    try {
      KeySpec keySpec = new DESKeySpec(key.getBytes());
      SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(DES_ALGORITHM);
      SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
      byte[] decodedData = Base64.getDecoder().decode((String) value);
      Cipher cipher = Cipher.getInstance(DES_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      //noinspection unchecked
      return (R) new String(cipher.doFinal(decodedData));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}

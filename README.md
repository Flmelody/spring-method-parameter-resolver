![GitHub](https://img.shields.io/github/license/Flmelody/spring-method-parameter-resolver)
<img alt="Dynamic XML Badge" src="https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fflmelody%2Fspring-method-parameter-resolver%2Fmaven-metadata.xml&query=%2F%2Fmetadata%2Fversioning%2Fversions%2Fversion%5Bnot(contains(text()%2C'spring6'))%5D%5B1%5D&logo=apachemaven&logoColor=%23a34b08&label=spring-method-parameter-resolver&labelColor=%231ba12d">
<img alt="Dynamic XML Badge" src="https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fflmelody%2Fspring-method-parameter-resolver%2Fmaven-metadata.xml&query=%2F%2Fmetadata%2Fversioning%2Fversions%2Fversion%5Bnot(contains(text()%2C'spring5'))%5D%5B1%5D&logo=apachemaven&logoColor=%23a34b08&label=spring-method-parameter-resolver&labelColor=%231ba12d">
## Instruction

[简体中文](README.zh-CN.md)

This package can help you implement Spring parameter custom binding , such as automatic conversion of parameter names ,
currently provides RequestParam enhancements.

## Choose correct version

This project contains two versions, one for Spring5 and another for Spring6.  
For Spring5 you should use

```groovy kotlin
implementation("org.flmelody:spring-method-parameter-resolver:x.y.z-spring5")
```

For Spring6

```groovy kotlin
implementation("org.flmelody:spring-method-parameter-resolver:x.y.z-spring6")
```

## SpringBoot quick start

- enable resolver, add this `@EnableWebResolver(paramNamingStrategy = NamingStrategy.SNAKE_CASE) `in
  any `@Configuration` class, for example
  ![img.png](img.png)
- use `@WebParam` to replace Spring `@RequestParam`
  Now, your request parameter can accept snake-case parameter name

## Build your own namingStrategyHandler

- implement `NamingStrategyHandler`
- register your naming-strategy handler into bean factory

## Build your own methodArgumentResolver

- implement `WebMethodArgumentResolver`
- register your argument resolver into bean factory
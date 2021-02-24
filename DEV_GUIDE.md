Formplayer Dev Resources
========================

This document is aimed at developers starting out with Formplayer and
particularly those who may not be familiar with Java or Spring. It lays out a set of
resources that can be used as to learn about the technologies used.

Formplayer is a [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/index.html) which
takes an opinionated view of the Spring platform and third-party libraries,
so that you can get started with minimum fuss. Most Spring Boot applications need very
little Spring configuration.

[Spring](https://spring.io/) is a collection of tools and libraries that have been
built to make developing Java applications easier and safer. At it's core is the Spring Framework
which provides the technologies that support the rest of the tools: dependency injection, events, resources, i18n,
validation, data binding, type conversion, SpEL (Spring Expressive Language), AOP (Aspect Oriented Programming)

Developing Formplayer will require some level of knowledge of at least the following core components:
* [dependency injection (IoC)](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans)
* [AOP](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)

Spring Boot ties a number of the components of Spring together in way to meets the needs of most projects. It does
this by using [conventions][boot_conventions], but it does not stop you from overriding the conventions.

[boot_conventions]: https://docs.spring.io/spring-boot/docs/current/reference/html/using-spring-boot.html#using-boot-structuring-your-code

In Formpayer the main features we make use of are:
* [Web MVC framework](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-developing-web-applications)
* [Spring Data for SQL](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-sql)
  * [JPA guide](https://spring.io/guides/gs/accessing-data-jpa/)
  * [JPA reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories)
* [Caching](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-caching)
  * [Caching guide](https://spring.io/guides/gs/caching/)
  * [Caching reference](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)

### Glossary
* [JPA](https://en.wikipedia.org/wiki/Jakarta_Persistence): Jakarta Persistence
* [Hibernate](https://en.wikipedia.org/wiki/Hibernate_(framework)): ORM framework that implements the JPA spec
* [IoC](https://en.wikipedia.org/wiki/Inversion_of_control): Inversion of Control programming principal

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
* [Security](https://docs.spring.io/spring-boot/docs/2.4.3/reference/html/spring-boot-features.html#boot-features-security)
  * [Security guide](#security)
  * [Spring security reference](https://docs.spring.io/spring-security/site/docs/5.4.5/reference/html5/#introduction)

### Security
All Formplayer endpoints other than "/serverup" and "/validate_form" are secured using Spring Security.

Spring security is part of the Spring framework that provides filters and classes for securing an application.
The primary mechanism is a set of request filters that apply the security policy to requests.

The security policy is defined in the [WebSecurityConfig](src/main/java/org/commcare/formplayer/configuration/WebSecurityConfig.java)
class.

Formplayer uses two custom auth filters which are applied to each request. Each filter is responsible for
separate authentication mechanisms and only one filter will be applied to each request.

#### Django session auth
The primary mode is to use the session token provided by Django. This is passed to Formplayer via the
Django session cookie which is accessible to Formplayer since it is running under the same domain.

Formplayer reads the session ID out of the request the `username` and `domain`. It then makes a request to
CommCare HQ for the user details which it validates against the current request.

CommCareSessionAuthFilter
  - generates a `PreAuthenticatedAuthenticationToken` containing:
    - credentials: session cookie value
    - principal: UserDomainPreAuthPrincipal containing username and domain of the request
  - Calls `HQUserDetailsService.loadUserDetails(token)` (via the `AuthenticationManager`)
    - this makes the call the HQ and returns an `HqUserDetailsBean` or raises an exception
  - If everything is successful the `HqUserDetailsBean` is placed into the security context.

### HMAC auth

This mode of authenticating requests is used when requests are performed outside the scope of a user session. For
example, when CommCare HQ makes requests to Formplayer as part of an SMS interaction.

The authentication for this mode is handled by the `HmacAuthFilter` which validates the HMAC in the `X-MAC-DIGEST`
request header. If the HMAC is valid the filter constructs a `HqUserDetailsBean` from the request body or by
fetching a session record from the DB. This user details bean is then placed in the security context.

### Glossary
* [JPA](https://en.wikipedia.org/wiki/Jakarta_Persistence): Jakarta Persistence
* [Hibernate](https://en.wikipedia.org/wiki/Hibernate_(framework)): ORM framework that implements the JPA spec
* [IoC](https://en.wikipedia.org/wiki/Inversion_of_control): Inversion of Control programming principal

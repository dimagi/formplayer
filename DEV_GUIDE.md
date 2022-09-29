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

## Security
All Formplayer endpoints other than "/serverup" are secured using Spring Security.

Spring security is part of the Spring framework that provides filters and classes for securing an application.
The primary mechanism is a set of request filters that apply the security policy to requests.

The security policy is defined in the [WebSecurityConfig](src/main/java/org/commcare/formplayer/configuration/WebSecurityConfig.java)
class.

Formplayer uses two custom auth filters which are applied to each request. Each filter is responsible for
separate authentication mechanisms and only one filter will be applied to each request.

#### Django session auth
The primary mode is to use the session token provided by Django. This is passed to Formplayer via the
Django session cookie which is accessible to Formplayer since it is running under the same domain.

Formplayer reads the session ID out of the request along with the `username` and `domain`.
It then makes a request to CommCare HQ for the user details which it validates against the current request.

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
fetching a session record from the DB. This user details bean is then placed in the security context. If it is not
possible to construct the user details then an anonymous token is placed in the security context with the role
"COMMCARE" to indicate that the HMAC auth passed but with no user details.


## Testing

Formplayer has a lot of legacy tests which use mock controllers (anything inheriting from `BaseTestClass`)
but we have begun the process to migrate test to a more standard Spring Boot architecture. The rest of this
section will discuss the new approach.

### WebMvcTest

Most tests are interacting with the REST controllers. In order to test these without the need to set up
the full server and database we use the `@WebMvcTest` annotation. With this annotation we can autowire
a `MockMvc` object into the test which can be used to make mock requests to the controller.

`@WebMvcTest` does not configure services or the data access layer so those need to be configured manually
or mocked. In most cases we mock those except for tests that are testing those classes directly.

To make sure a specific controller is available for testing use the `@Import` annotation:

```java
@WebMvcTest
@Import(UtilController.class)
class UtilControllerTests {
    @Autowired
    private MockMvc mockMvc;

    // ...
}
```

To provide the dependent services that the controllers require we use a set of configuration classes which
create the bean required by the controllers:

```java
@WebMvcTest
@Import(UtilController.class)
@ContextConfiguration(classes={TestContext.class, CacheConfiguration.class})
class UtilControllerTests {
    // ...
}
```

Some beans require further configuration. This is done using [Junit5 extensions](https://junit.org/junit5/docs/current/user-guide/#extensions)
which hook into the test lifecycle and configure the service mocks:

* [FormDefSessionServiceExtension](src/test/java/org/commcare/formplayer/junit/FormDefSessionServiceExtension.kt)
* [FormSessionServiceExtension](src/test/java/org/commcare/formplayer/junit/FormSessionServiceExtension.kt)
* [RestoreFactoryExtension](src/test/java/org/commcare/formplayer/junit/RestoreFactoryExtension.kt)
* [StorageFactoryExtension](src/test/java/org/commcare/formplayer/junit/StorageFactoryExtension.kt)
* [InitializeStaticsExtension](src/test/java/org/commcare/formplayer/junit/InitializeStaticsExtension.java)

These can be applied to tests using the `@ExtendWith` annotation at the test class level:

```java
@ExtendWith(FormDefSessionServiceExtension.class)
class MyTests {
    // ...
}
```

Some extensions require configuration:

```java
class MyTests {
    @RegisterExtension
    static RestoreFactoryExtension restoreFactoryExt = new RestoreFactoryExtension.builder()
            .withUser("user").withDomain("domain")
            .withRestorePath("custom/restore/file.xml")
            .build();
}
```

We also have some convenience annotations which apply a number of the extensions together:

* [FormSessionTest](src/test/java/org/commcare/formplayer/junit/FormSessionTest.java)

### Mock Requests

Making requests to the controllers can be done directly using the `MockMvc` class. Alternately we have
created a set of request classes for common requests:

* [NewFormRequest](src/test/java/org/commcare/formplayer/junit/request/NewFormRequest.kt)
* [SubmitFormRequest](src/test/java/org/commcare/formplayer/junit/request/SubmitFormRequest.kt)
* [SyncDbRequest](src/test/java/org/commcare/formplayer/junit/request/SyncDbRequest.kt)

These can be used as follows:

```java
NewFormRequest request = new NewFormRequest(mockMvc, webClientMock, "form.xml");
Response response = request.request();
response.andExpect(jsonPath("status").value("success"));
NewFormResponse responseBean = response.bean();
```

Additional utilities:

* [Installer](src/test/java/org/commcare/formplayer/junit/Installer.kt)

## Glossary
* [JPA](https://en.wikipedia.org/wiki/Jakarta_Persistence): Jakarta Persistence
* [Hibernate](https://en.wikipedia.org/wiki/Hibernate_(framework)): ORM framework that implements the JPA spec
* [IoC](https://en.wikipedia.org/wiki/Inversion_of_control): Inversion of Control programming principal

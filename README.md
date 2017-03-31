FormPlayer
===========

FormPlayer is a RESTful XForm entry service written on the [Spring Framework](https://projects.spring.io/spring-framework/).
Given a [user restore](https://confluence.dimagi.com/display/commcarepublic/OTA+Restore+API) and 
an [XForm](http://dimagi.github.io/xform-spec/) FormPlayer enables form entry via JSON calls and responses (detailed below).
These files will often be hosted by a [CommCareHQ](https://www.github.com/dimagi/commcare-hq) server instance. FormPlayer
relies on the [CommCare](https://www.github.com/dimagi/commcare-hq) and [Javarosa](https://www.github.com/dimagi/commcare-hq) 
libraries (included as subrepositories). FormPlayer is built via gradle (wrapper files included). 

### Dependencies
+ FormPlayer caches session instances via Redis
+ FormPlayer stores session instances via Postgres
+ FormPlayer builds SQLite database for each restored user

Building and Running
------------

Download submodule dependencies

    $ git submodule update --init --recursive

To make properties file:

    $ cp config/application.properties.example config/application.properties // Update properties as necessary

Make sure you have the formplayer database created

    $ createdb formplayer -U commcarehq -h localhost  // Update connection info as necessary

To run:

    $ ./gradlew build; java -jar build/libs/formplayer.jar

To test:

    $ ./gradlew test

    # to run a single test
    $ ./gradlew :test --tests tests.NewFormTests.testNewForm

    # to run in continuous mode
    $ ./gradlew test -t

When building on Linux it is sometimes necessary to run:

    $ gradle wrapper
    
Finally, turn on the "Use the new formplayer frontend" feature flag on your CommCareHQ domain
    
### Docs
____________

We automatically generate API documentation using the Swagger plug-in [SpringFox](https://github.com/springfox/springfox)
for Spring. To view the generated docs, run the server (above) and navigate to http://localhost:8080/swagger-ui.html#/ (changing
host and port as appropriate)

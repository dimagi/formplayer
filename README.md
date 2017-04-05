FormPlayer
===========

FormPlayer is a RESTful XForm entry service written on the [Spring Framework](https://projects.spring.io/spring-framework/).
Given a [user restore](https://confluence.dimagi.com/display/commcarepublic/OTA+Restore+API) and 
an [XForm](http://dimagi.github.io/xform-spec/) FormPlayer enables form entry via JSON calls and responses (detailed below).
These files will often be hosted by a [CommCareHQ](https://www.github.com/dimagi/commcare-hq) server instance. Formplayer relies on the [CommCare](https://www.github.com/dimagi/commcare-core) libraries (included as subrepositories). Formplayer is built via gradle (wrapper files included).

### Dependencies
+ Formplayer caches session instances via Redis
+ Formplayer stores session instances via Postgres
+ Formplayer builds SQLite database for each restored user

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

### Contributing

For PRs that just modify code in the Formplayer repo, submit a PR to Formplayer on a separate branch.


#### Contributing changes to commcare

Formplayer also has a dependency on the commcare-core repository. The commcare-core `master` branch is not
stable and Formplayer uses a different branch. The submodule repo `libs/commcare` should always be pointing to
the `formplayer` branch.

#### Updating the CommCare version

When updating Formplayer to have a new release of a CommCare version (e.g. 2.34 to 2.35), a PR should be opened from the `commcare_X.Y` branch into
the `formplayer` branch. Once QA has been finished, merge the PR and update the Formplayer submodule.


### Docs
____________

We automatically generate API documentation using the Swagger plug-in [SpringFox](https://github.com/springfox/springfox)
for Spring. To view the generated docs, run the server (above) and navigate to http://localhost:8080/swagger-ui.html#/ (changing
host and port as appropriate)

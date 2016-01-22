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

To make properties file:

    $ mv config/application.properties.example config/application.properties // Update properties as necessary

To run:

    $ ./gradlew build; java -jar build/libs/formplayer

To test:

    $ ./gradlew test
    
When building on Linux it is sometimes necessary to run:

    $ gradle wrapper
    

Endpoints
------------

### Implemented

####/new_session
+ Download requisite XForm and instantiate FormEntrySession 
+ If necessary, download OTA restore payload and generate the User's SQLite DB
+ Return the XForm description tree, the form title, form languages, and session-id

####/answer
+ Retrieve cached session and enter given answer at given form index
+ Process this in the form entry model, then return the new form tree if accepted or the error message if rejected.
+ Return updated form entry tree if answer accepted, error message otherwise

####/filter_cases
+ Given a user and filter expression (IE predicate expression) return the list of caseIds of that user's cases 
matching that expression
+ Will fetch the user restore from the host server and generate the user's SQLite DB if necessary

####/current
+ Provided with a sessionId, restore that session from the cache or Postgres and return the current decision tree

### TODO

####/submit_all
Submit the set of form indexes and corresponding answer values. Return the completed form instance. Optionally update the touchforms user db. 
####/evaluate_xpath
Used by debugger. Submit an XPath, evaluate against the current instance, and return the value on that path.
####/get_instance
Given a session id, return the instance XML and XMLNS associated with that session.
####/sync_db
Given a userna,e sync that user's DB. 
####/new_repeat
Add a new repeat, presumably after being prompted.
####/edit_repeat
Edit a repeat group
####/delete_repeat
Delete a repeat group
 
xformplayer.Actions.DELETE_REPEAT:

###TBD
####/next
Move to next event, skipping the current. Can't think of how this would be used considering we display all questions at once so wouldn't have the notion of being at one index and skipping the next.
####/set_lang
Set the language to be used by the XForm engine
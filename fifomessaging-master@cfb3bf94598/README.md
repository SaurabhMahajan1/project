# fifomessaging Readme #

This is a demonstrator application - the full application used in production is FifoMessagingImpl

### What is this repository for? ###

* fifomessaging is a Spring Boot application RESTing with Jersey 2, Spring Data, Gradle, Swagger and Cucumber.

### How do I set up? ###

* [Refer to Spring Boot](https://spring.io/guides/gs/spring-boot/)

* Run the following command to build war file

```
clean test jacoco build
```

* To deploy to Tomcat, populate and copy src/main/environment/fifomessaging.properties to tomcat lib directory
and copy build/libs/fifomessaging.war to tomcat webapps directory

* Click [Swagger link for Jersey 2 Rest Services](http://localhost:8080/fifomessaging/docs/index.html).
* Click [WADL link for Jersey 2 Rest Services](http://localhost:8080/fifomessaging/api/application.wadl?detail=true).
* Click [Couchbase console](http://localhost:8091/).
* Test report can be found here (build/reports/tests/index.html)
* Coverage report can be found here (build/reports/coverage/index.html)
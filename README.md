# A sample JPA project 
## With connections to H2, HSQLDB, and SQLITE

JPA testing project - comparing Embedded SQLite, H2, and HSQLDB.

Includes a simple sample of embedded ID's and enums.  
The sample does not try to be an extensive use case of JPA, 
but provides a simple skeleton for trying it out on
the different databases for compatibility.

There is also a simple performance test for comparison included.  It is not very exhaustive, purely illustrative.

Includes a sample to-from DTO.

There is a handy autocloseable, and reentrant JTA Transaction wrapper class.

The samples have heavily used Lombok.

Test assertions use google.truth.

Probably most useful are: 
* [./src/main/resources/META-INF/persistence.xml]
* [./src/main/java/com/jpa/test/util/ClosableTransaction.java]

## Building and running

* To compile and package the jar (also runs the unit tests): mvn package
* To just run the tests: mvn test
* To run the jar: jpa-perf-test.sh or java -jar target/jpa-perf-test.jar
* Options
** -n Number of iterations - default 1000
** -o Number of unique objects for query testing - default 10
** -q Number of unique objects for bulk query testing - default 1000








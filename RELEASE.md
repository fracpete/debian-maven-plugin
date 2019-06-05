# How to make a release

* Run the following command to deploy the artifacts:

  ```
  mvn release:clean release:prepare release:perform site
  ```

* Push all changes

* Change the version in `pom.xml` to the just released one 

* Run the following command to update the documentation on 
  [github](https://fracpete.github.io/debian-maven-plugin/):

  ```
  mvn -Pgithub-site site
  ```

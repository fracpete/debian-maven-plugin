# How to make a release

## Deploy artifacts

* Run the following command to deploy the artifacts:

  ```
  mvn release:clean release:prepare release:perform
  ```

* Push all changes


## Documentation

* Change the version in the `pom.xml` to the correct version.

* Run the following command to update the github.io pages:

  ```
  mvn -Pgithub-site clean site
  ```

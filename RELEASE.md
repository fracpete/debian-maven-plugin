# How to make a release

* Run the following command to deploy the artifacts:

  ```
  mvn -Pgithub-site release:clean release:prepare release:perform site
  ```

* Push all changes

# How to make a release

* Add entry to changelog in [changelog.apt.vm](src/site/apt/changelog.apt.vm)

* Switch to Java 8

* Run the following command to deploy the artifacts:

  ```
  mvn release:clean release:prepare release:perform
  ```

* Push all changes

  ```
  git checkout gh-pages
  git pull
  git checkout master
  git push
  ```

* Change the version in `pom.xml` to the just released one 

* Run the following command to update the documentation on 
  [github](https://fracpete.github.io/debian-maven-plugin/):

  ```
  mvn site
  cp -R target/site/* ../debian-maven-plugin.gh-pages/
  cd ../debian-maven-plugin.gh-pages/
  git pull
  git commit -a -m updated
  git checkout master
  git pull
  git checkout gh-pages
  git push
  cd ../debian-maven-plugin
  git checkout pom.xml
  git pull
  git checkout gh-pages
  git pull
  git checkout master
  ```

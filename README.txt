-------------------
DEBIAN MAVEN PLUGIN
-------------------
http://debian-maven.sf.net/


Related sites
-------------

Sourceforge project page:
http://sourceforge.net/projects/debian-maven/

Sonatype Nexus web interface:
https://oss.sonatype.org/

Sonatype ticket system:
https://issues.sonatype.org/

Sonatype snapshot Maven repository:
https://oss.sonatype.org/content/repositories/snapshots/

Sonatype releases Maven repository:
https://oss.sonatype.org/content/repositories/releases/

Sonatype combined snapshot/releases Maven repository:
https://oss.sonatype.org/content/groups/public/

Sourceforge Maven repository (obsolete):
http://downloads.sourceforge.net/project/debian-maven/m2repo

Sonatype repository usage guide:
https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide


Deploying
---------

Before deploying, add the <server> entries to ~/.m2/settings.xml:

  <server>
    <id>sonatype-nexus-staging</id>
    <username>USERNAME</username>
    <password>PASSWORD</password>
  </server>

  <server>
    <id>sonatype-nexus-snapshots</id>
    <username>USERNAME</username>
    <password>PASSWORD</password>
  </server>

  <server>
    <id>debian-maven.sourceforge.net</id>
    <username>USERNAME,debian-maven</username>
    <password>PASSWORD</password>
  </server>

The first two entries are for sonatype staging and snapshot repositories, to
which artifacts are deployed. The third entry is for deploying the site to
sourceforge.

To publish the snapshot artifacts to sonatype, do:

  mvn -P fullbuild clean deploy

To stage a release, do:

	  mvn -P fullbuild release:clean
	  mvn -P fullbuild -Dusername=SCM_USERNAME release:prepare
	  ssh -t USERNAME,debian-maven@shell.sf.net create
	  mvn -P fullbuild release:perform

where SCM_USERNAME is the (sourceforge) username to access the source
repository, and USERNAME is the (same) username to publish the site.

This will deploy the artifacts to the sonatype staging repository and the
site to sourceforge. You still need to manually promote the package into the
release reepository, which is synchronized to central.

To publish only the site to sourceforge, do:

  ssh -t USERNAME,debian-maven@shell.sf.net create
  mvn site-deploy


Troubleshooting
---------------

If you experience certificate errors, add the following
parameters to the Maven command line:

	-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts
	-Djavax.net.ssl.trustStorePassword=changeit

-------------------
DEBIAN MAVEN PLUGIN
-------------------
http://debian-maven.sf.net/


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

  mvn -P release clean deploy

To stage a release, do:

	  mvn -P release release:clean
	  mvn -P release -Dusername=SCM_USERNAME release:prepare
	  ssh -t USERNAME,debian-maven@shell.sf.net create
	  mvn -P release release:perform

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

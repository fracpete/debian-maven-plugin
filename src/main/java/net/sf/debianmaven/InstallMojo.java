package net.sf.debianmaven;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;

/**
 * Installs the generated Debian package in the current system.
 *
 * Uses external utilities: <a href="http://www.debian.org/doc/manuals/debian-faq/ch-pkgtools.en.html">dpkg</a> and sudo.
 *
 * @goal install
 */
public class InstallMojo extends AbstractDebianMojo
{
	private void runInstall() throws IOException, MojoExecutionException
	{
		if (getPackageFile().exists())
			runProcess(new String[]{"sudo", "dpkg", "--force-confdef", "-i", getPackageFile().toString()}, true);
		else
			getLog().warn("Debian package file not found; doing nothing.");
	}

	protected void executeDebMojo() throws MojoExecutionException
	{
		try
		{
			runInstall();
		}
		catch (IOException e)
		{
			getLog().error(e.toString());
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}

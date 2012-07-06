package nl.holmes.mkdeb;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

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
		runProcess(new String[]{"sudo", "dpkg", "-i", getPackageFile().toString()}, true);
	}

	public void execute() throws MojoExecutionException
	{
		try
		{
			runInstall();
		}
		catch (IOException e)
		{
			throw new MojoExecutionException(e.toString());
		}
	}
}

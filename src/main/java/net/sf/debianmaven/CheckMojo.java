package net.sf.debianmaven;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Checks whether the generated package complies to style rules.
 *
 * Uses external utility: <a href="http://lintian.debian.org/">lintian</a>.
 *
 * @goal check
 * @phase package
 */
public class CheckMojo extends AbstractDebianMojo
{
	private void runLintian() throws IOException, MojoExecutionException
	{
		runProcess(new String[]{"lintian", getPackageFile().toString()}, true);
	}

	protected void executeDebMojo() throws MojoExecutionException
	{
		try
		{
			runLintian();
		}
		catch (IOException e)
		{
			getLog().error(e.toString());
			throw new MojoExecutionException(e.toString());
		}
	}
}

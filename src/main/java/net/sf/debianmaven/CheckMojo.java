package net.sf.debianmaven;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;

/**
 * Checks whether the generated package complies to style rules.
 * <br>
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
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}

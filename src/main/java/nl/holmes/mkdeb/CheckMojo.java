package nl.holmes.mkdeb;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal check
 */
public class CheckMojo extends AbstractDebianMojo
{
	private void runLintian() throws IOException, MojoExecutionException
	{
		runProcess(new String[]{"lintian", getPackageFile().toString()}, true);
	}

	public void execute() throws MojoExecutionException
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

package nl.holmes.mkdeb;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal clean
 */
public class CleanMojo extends AbstractDebianMojo
{
	public void execute() throws MojoExecutionException
	{
		try
		{
			FileUtils.deleteDirectory(stageDir);
			getPackageFile().delete();
		}
		catch (IOException e)
		{
			throw new MojoExecutionException(e.toString());
		}
	}
}

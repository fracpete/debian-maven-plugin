package nl.holmes.mkdeb;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal reprepro-install
 * @phase install
 */
public class RepreproInstallMojo extends AbstractRepreproMojo
{
	/**
	 * @parameter expression="${deb.repository.branch}" default-value="experimental"
	 */
	protected String repositoryBranch;

	private void runReprepro() throws IOException, MojoExecutionException
	{
		runProcess(new String[]{"reprepro", "--confdir", repreproConfigurationDir.toString(), "--basedir", repository.toString(), "includedeb", repositoryBranch, getPackageFile().toString()}, true);
	}

	public void execute() throws MojoExecutionException
	{
		try
		{
			runReprepro();
		}
		catch (IOException e)
		{
			getLog().error(e.toString());
			throw new MojoExecutionException(e.toString());
		}
	}
}

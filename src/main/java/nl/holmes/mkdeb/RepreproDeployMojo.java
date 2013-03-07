package nl.holmes.mkdeb;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal deploy
 * @phase install
 */
public class RepreproDeployMojo extends AbstractRepreproMojo
{
	/**
	 * @parameter expression="${deb.repository.branch}" default-value="experimental"
	 */
	protected String repositoryBranch;

	/**
	 * @parameter expression="${deb.deploy.skipMissing}" default-value="false"
	 * @since 1.0.4
	 */
	protected boolean skipDeployMissing;

	private void runReprepro() throws IOException, MojoExecutionException
	{
		File pkgfile = getPackageFile();
		if (pkgfile.exists())
			runProcess(new String[]{"reprepro", "--confdir", repreproConfigurationDir.toString(), "--basedir", repository.toString(), "includedeb", repositoryBranch, pkgfile.toString()}, true);
		else
			getLog().info("Skipping deployment of non-existent package: "+pkgfile);
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

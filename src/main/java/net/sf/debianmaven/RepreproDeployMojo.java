package net.sf.debianmaven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @goal deploy
 * @phase install
 */
public class RepreproDeployMojo extends AbstractRepreproMojo
{
	private static final String DEFAULT_CODENAME = "experimental";
	/**
	 * @deprecated
	 * @see #repositoryCodenames
	 * @parameter expression="${deb.repository.branch}" default-value="experimental"
	 */
	protected String repositoryBranch;

	/**
	 * @parameter expression="${deb.repository.codenames}"
	 * @since 1.0.7
	 */
	protected String[] codenames;

	/**
	 * @parameter expression="${deb.deploy.skipMissing}" default-value="false"
	 * @since 1.0.4
	 */
	protected boolean skipDeployMissing;

	@SuppressWarnings("unchecked")
	private void runReprepro() throws IOException, MojoExecutionException
	{
		File pkgfile = getPackageFile();
		if (pkgfile.exists())
		{
			List<String> codenames = new ArrayList<String>();
			if (repositoryBranch != null)
			{
				codenames.add(repositoryBranch);
				getLog().warn("parameter 'repositoryBranch' is now deprecated; use 'codenames' instead");
			}
			if (this.codenames != null)
				codenames.addAll(Arrays.asList(this.codenames));
			if (codenames.isEmpty())
				codenames.add(DEFAULT_CODENAME);

			for (String codename : codenames)
				runProcess(new String[]{"reprepro", "--confdir", repreproConfigurationDir.toString(), "--basedir", repository.toString(), "includedeb", codename, pkgfile.toString()}, true);
		}
		else
			getLog().info("Skipping deployment of non-existent package: "+pkgfile);
	}

	protected void executeDebMojo() throws MojoExecutionException
	{
		try
		{
			runReprepro();
		}
		catch (IOException e)
		{
			getLog().error(e.toString());
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}

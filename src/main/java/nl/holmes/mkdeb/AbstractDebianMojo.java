package nl.holmes.mkdeb;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

public abstract class AbstractDebianMojo extends AbstractMojo
{
	/**
	 * @parameter expression="${deb.package.name}" default-value="${project.artifactId}"
	 */
	protected String packageName;

	/**
	 * @parameter expression="${deb.package.version}" default-value="${project.version}"
	 */
	protected String packageVersion;

	/**
	 * @parameter expression="${deb.package.revision}" default-value="1"
	 */
	protected String packageRevision;

	/**
	 * @parameter expression="${deb.package.architecture}" default-value="all"
	 */
	protected String packageArchitecture;

	/**
	 * @parameter expression="${deb.maintainer.name}" default-value="${project.developers[0].name}"
	 */
	protected String maintainerName;

	/**
	 * @parameter expression="${deb.maintainer.email}" default-value="${project.developers[0].email}"
	 */
	protected String maintainerEmail;

	/** @parameter default-value="${basedir}/target" */
	protected File targetDir;

	/** @parameter default-value="${targetDir}/deb" */
	protected File stageDir;

	protected File getPackageFile()
	{
		return new File(targetDir, String.format("%s_%s-%s_all.deb", packageName, packageVersion, packageRevision));
	}
	
	private int runProcess(Process p) throws IOException
	{
		p.getOutputStream().close();
		
		BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
		BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		
		//TODO: this is a possible deadlock 
		for (;;)
		{
			String line = stdout.readLine();
			if (line == null)
				break;
			else
				getLog().info(line);
		}
		
		for (;;)
		{
			String line = stderr.readLine();
			if (line == null)
				break;
			else
				getLog().warn(line);
		}
		
		for (;;)
		{
			try {
				return p.waitFor();
			} catch (InterruptedException e) {
			}
		}
	}

	protected void runProcess(String[] cmd, boolean throw_on_failure) throws IOException, MojoExecutionException
	{
		String cmdline = StringUtils.join(cmd, " ");
		getLog().info("Start process: "+cmdline);
		int exitval = runProcess(Runtime.getRuntime().exec(cmd));
		if (exitval != 0)
		{
			getLog().warn("Exit code "+exitval);
			
			if (throw_on_failure)
				throw new MojoExecutionException("Process returned non-zero exit code: "+cmdline);
		}
	}
}

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
	protected String package_name;

	/**
	 * @parameter expression="${deb.package.version}" default-value="${project.version}"
	 */
	protected String package_version;

	/**
	 * @parameter expression="${deb.package.revision}" default-value="1"
	 */
	protected String package_revision;

	/**
	 * @parameter expression="${deb.package.priority}" default-value="optional"
	 */
	protected String package_priority;

	/**
	 * @parameter expression="${deb.package.section}" default-value="contrib/utils"
	 */
	protected String package_section;

	/**
	 * @parameter expression="${deb.package.title}" default-value="${project.name}"
	 */
	protected String package_title;

	/**
	 * @parameter expression="${deb.package.description}" default-value="${project.description}"
	 */
	protected String package_description;

	/**
	 * @parameter expression="${deb.package.architecture}" default-value="all"
	 */
	protected String package_architecture;

	/**
	 * @parameter expression="${deb.package.depends}" default-value=""
	 */
	protected String package_depends;

	/**
	 * @parameter expression="${deb.project.url}" default-value="${project.organization.url}"
	 */
	protected String project_url;

	/**
	 * @parameter expression="${deb.project.organization}" default-value="${project.organization.name}"
	 */
	protected String project_organization;

	/**
	 * @parameter expression="${deb.maintainer.name}" default-value="${project.developers[0].name}"
	 */
	protected String maintainer_name;

	/**
	 * @parameter expression="${deb.maintainer.email}" default-value="${project.developers[0].email}"
	 */
	protected String maintainer_email;

	/**
	 * @parameter default-value="${project.artifactId}-${project.version}.jar"
	 */
	protected String defaultJar;

	/**
	 * @parameter
	 */
	protected String[] jars;

	protected File sourceDir = new File("src/deb/resources");
	protected File targetDir = new File("target");
	protected File stageDir = new File(targetDir, "deb");

	protected File getPackageFile()
	{
		return new File(targetDir, String.format("%s_%s-%s_all.deb", package_name, package_version, package_revision));
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

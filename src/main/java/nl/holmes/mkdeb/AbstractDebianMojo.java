package nl.holmes.mkdeb;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
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

	/** @parameter default-value="${basedir}/src/deb" */
	protected File sourceDir;

	/** @parameter default-value="${basedir}/target" */
	protected File targetDir;

	/** @parameter default-value="${basedir}/target/deb" */
	protected File stageDir;

	protected File getPackageFile()
	{
		return new File(targetDir, String.format("%s_%s-%s_all.deb", packageName, packageVersion, packageRevision));
	}

	protected void runProcess(String[] cmd, boolean throw_on_failure) throws ExecuteException, IOException, MojoExecutionException
	{
		CommandLine cmdline = new CommandLine(cmd[0]);
		cmdline.addArguments(Arrays.copyOfRange(cmd, 1, cmd.length));

		getLog().info("Start process: "+cmdline);

		PumpStreamHandler streamHandler = new PumpStreamHandler(new LogOutputStream(getLog()));
		DefaultExecutor exec = new DefaultExecutor();
		exec.setStreamHandler(streamHandler);
		int exitval = exec.execute(cmdline);
		if (exitval != 0)
		{
			getLog().warn("Exit code "+exitval);
			
			if (throw_on_failure)
				throw new MojoExecutionException("Process returned non-zero exit code: "+cmdline);
		}
	}
}

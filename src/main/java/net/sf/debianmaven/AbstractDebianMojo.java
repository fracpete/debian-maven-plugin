package net.sf.debianmaven;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

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
	private String packageVersion;

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

	/**
	 * @parameter expression="${deb.package.snapshotRevFile}"
	 * @since 1.0.5
	 */
	private File snapshotRevisionFile = null;

	private static final DateFormat datefmt = new SimpleDateFormat("yyyyMMddHHmm");

	private String snapshotRevision = null;

	protected String processVersion(String version)
	{
		if (snapshotRevision == null)
		{
			Date revtime = snapshotRevisionFile != null
					? new Date(snapshotRevisionFile.lastModified())
					: new Date();

			snapshotRevision = "+" + datefmt.format(revtime);
		}

		return version.replaceAll("-SNAPSHOT", snapshotRevision);
	}

	protected String[] processVersion(String[] versions)
	{
		String[] result = new String[versions.length];
		for (int i=0 ; i<versions.length ; i++)
			result[i] = processVersion(versions[i]);

		return result;
	}

	protected String getPackageVersion()
	{
		return processVersion(packageVersion);
	}

	protected File getPackageFile()
	{
		return new File(targetDir, String.format("%s_%s-%s_all.deb", packageName, getPackageVersion(), packageRevision));
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

package net.sf.debianmaven;

import com.github.fracpete.processoutput4j.core.StreamingProcessOutputType;
import com.github.fracpete.processoutput4j.core.StreamingProcessOwner;
import com.github.fracpete.processoutput4j.output.CollectingProcessOutput;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public abstract class AbstractDebianMojo extends AbstractMojo
{
	private static final String SKIP_DEB_PROPERTY = "skipDeb";
	private static final String RUN_DEB_PROPERTY = "runDeb";

	public static class LogOutput implements StreamingProcessOwner
	{
		private Log log;

		public LogOutput(Log log)
		{
			this.log = log;
		}

		public StreamingProcessOutputType getOutputType() {
			return StreamingProcessOutputType.BOTH;
		}

		public void processOutput(String line, boolean stdout) {
			if (stdout)
				log.info(line);
			else
				log.error(line);
		}
	}


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

	protected File newFile(File dir, String name)
	{
		return new File(dir.getAbsolutePath() + File.separator + name);
	}

	protected String getPackageVersion()
	{
		return processVersion(packageVersion);
	}

	protected File getPackageFile()
	{
		return newFile(targetDir, String.format("%s_%s-%s_all.deb", packageName, getPackageVersion(), packageRevision));
	}

	protected void runProcess(String[] cmd, boolean throw_on_failure) throws IOException, MojoExecutionException
	{
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(cmd);

		getLog().info("Start process: " + Arrays.asList(cmd));

		try
		{
			CollectingProcessOutput output = new CollectingProcessOutput();
			output.monitor(builder);
			int exitval = output.getExitCode();
			if (exitval != 0)
			{
				getLog().warn("Exit code: " + exitval);
				getLog().warn("stderr:\n" + output.getStdErr());
				getLog().warn("stdout:\n" + output.getStdOut());

				if (throw_on_failure)
					throw new MojoExecutionException("Process returned non-zero exit code: " + Arrays.asList(cmd));
			}
		}
		catch (MojoExecutionException e)
		{
			throw e;
		}
		catch (Exception e) {
		  throw new IOException(e);
		}
	}

	protected abstract void executeDebMojo() throws MojoExecutionException;

	public final void execute() throws MojoExecutionException
	{
		if (System.getProperties().containsKey(RUN_DEB_PROPERTY))
		{
			getLog().info("debian-maven execution forced (-DrunDeb)");
			executeDebMojo();
		}
		else if (System.getProperties().containsKey(SKIP_DEB_PROPERTY))
			getLog().info("debian-maven execution skipped (-DskipDeb)");
		else if (!System.getProperty("os.name").equals("Linux"))
			getLog().warn("debian-maven execution skipped (non-linux OS)");
		else
			executeDebMojo();
	}
}

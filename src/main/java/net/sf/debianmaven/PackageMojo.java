package net.sf.debianmaven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Generates a Debian package.
 *
 * Uses Debian utilities: <a href="http://www.debian.org/doc/manuals/debian-faq/ch-pkgtools.en.html">dpkg-deb</a> and fakeroot.
 *
 * @goal package
 * @phase package
 * @requiresDependencyResolution
 */
public class PackageMojo extends AbstractDebianMojo
{
	/**
	 * @required
	 * @parameter expression="${deb.package.priority}" default-value="optional"
	 */
	protected String packagePriority;

	/**
	 * @required
	 * @parameter expression="${deb.package.section}" default-value="contrib/utils"
	 */
	protected String packageSection;

	/**
	 * @required
	 * @parameter expression="${deb.package.title}" default-value="${project.name}"
	 */
	protected String packageTitle;

	/**
	 * @required
	 * @parameter expression="${deb.package.description}" default-value="${project.description}"
	 */
	protected String packageDescription;

	/**
	 * @parameter
	 */
	protected String[] packageDependencies;

	/**
	 * @parameter expression="${deb.project.url}" default-value="${project.organization.url}"
	 */
	protected String projectUrl;

	/**
	 * @parameter expression="${deb.project.organization}" default-value="${project.organization.name}"
	 */
	protected String projectOrganization;

	/**
	 * @parameter expression="${deb.include.jar}"
	 */
	@Deprecated
	protected String includeJar;

	/**
	 * @parameter
	 */
	@Deprecated
	protected String[] includeJars;

	/**
	 * @parameter expression="${deb.exclude.all-jars}"
	 */
	@Deprecated
	protected String excludeAllJars;

	/**
	 * @parameter
	 * @since 1.0.3
	 */
	protected Set<String> includeArtifacts;

	/**
	 * @parameter
	 * @since 1.0.3
	 */
	protected Set<String> excludeArtifacts;

	/**
	 * @parameter default-value="false"
	 * @since 1.0.3
	 */
	protected boolean excludeAllArtifacts;

	/**
	 * @parameter default-value="false"
	 * @since 1.0.3
	 */
	protected boolean excludeAllDependencies;

	/**
	 * @parameter default-value="true"
	 * @since 1.0.3
	 */
	protected boolean includeAttachedArtifacts;

	/**
	 * The Maven project object
	 * 
	 * @parameter expression="${project}"
	 */
	private MavenProject project;

	private File createTargetLibDir()
	{
		File targetLibDir = new File(stageDir, "usr/share/lib/" + packageName);
		targetLibDir.mkdirs();
		return targetLibDir;
	}

	private void createSymlink(File symlink, String target) throws ExecuteException, MojoExecutionException, IOException
	{
		if (symlink.exists())
			symlink.delete();
		runProcess(new String[]{"ln", "-s", target, symlink.toString()}, true);
	}

	private void writeIncludeFile(File targetLibDir, String artifactId, String version, Collection<String> dependencies) throws IOException, MojoExecutionException
	{
		if (dependencies == null)
			dependencies = Collections.emptySet();

		File deplist = new File(targetLibDir, String.format("%s-%s.inc", artifactId, version));
		FileWriter out = new FileWriter(deplist);
		try
		{
			out.write(String.format("artifacts=%s\n", StringUtils.join(new HashSet<String>(dependencies), ":")));
		}
		finally
		{
			out.close();
		}

		createSymlink(new File(targetLibDir, String.format("%s.inc", artifactId)), deplist.getName());
	}

	private boolean includeArtifact(Artifact a)
	{
		boolean doExclude = excludeArtifacts != null && (a.getDependencyTrail() == null || Collections.disjoint(a.getDependencyTrail(), excludeArtifacts));
		if (doExclude)
			return false;

		if (includeArtifacts == null)
			return true;

		if (a.getDependencyTrail() == null)
			return true;

		return Collections.disjoint(a.getDependencyTrail(), includeArtifacts);
	}

	private File copyArtifact(Artifact a, File targetLibDir) throws IOException, MojoExecutionException
	{
		if (a.getFile() == null)
			throw new MojoExecutionException(String.format("No file was built for required artifact: %s:%s:%s", a.getGroupId(), a.getArtifactId(), a.getVersion()));

		getLog().info(String.format("Artifact: %s", a.getFile().getPath()));
		File src = a.getFile();
		File trg = new File(targetLibDir, src.getName());
		FileUtils.copyFile(src, trg);

		String linkname = src.getName().replaceFirst("-"+a.getVersion(), "");
		createSymlink(new File(targetLibDir, linkname), a.getFile().getName());

		return trg;
	}

	@SuppressWarnings("unchecked")
	private void copyAttachedArtifacts() throws FileNotFoundException, IOException, MojoExecutionException
	{
		if (!includeAttachedArtifacts)
		{
			getLog().info("Skipping attached project artifacts.");
			return;
		}

		getLog().info("Copying attached project artifacts.");
		File targetLibDir = createTargetLibDir();

		for (Artifact a : (Collection<Artifact>)project.getAttachedArtifacts())
			copyArtifact(a, targetLibDir);
	}

	@SuppressWarnings("unchecked")
	private void copyArtifacts() throws FileNotFoundException, IOException, MojoExecutionException
	{
		if (excludeAllArtifacts)
		{
			getLog().info("Skipping regular project artifacts and dependencies.");
			return;
		}

		File targetLibDir = createTargetLibDir();

		Collection<Artifact> artifacts = new ArrayList<Artifact>();
		artifacts.add(project.getArtifact());

		if (excludeAllDependencies)
			getLog().info("Copying regular project artifacts but not dependencies.");
		else
		{
			getLog().info("Copying regular project artifacts and dependencies.");
			artifacts.addAll((Collection<Artifact>)project.getRuntimeArtifacts());
		}

		/*
		 * TODO: this code doesn't work as it should due to limitations of Maven API; see also:
		 * http://jira.codehaus.org/browse/MNG-4831
		 */

		Map<String,Artifact> ids = new HashMap<String,Artifact>();
		for (Artifact a : artifacts)
			ids.put(a.getId(), a);

		MultiMap<Artifact,String> deps = new MultiHashMap<Artifact,String>();
		for (Artifact a : artifacts)
		{
			if (includeArtifact(a))
			{
				File trg = copyArtifact(a, targetLibDir);

				if (a.getDependencyTrail() != null)
				{
					for (String id : a.getDependencyTrail())
					{
						Artifact depending = ids.get(id);
						if (depending != null)
							deps.put(depending, trg.getPath().substring(stageDir.getPath().length()));
					}
				}
			}
		}

		for (Artifact a : artifacts)
		{
			if (includeArtifact(a))
				writeIncludeFile(targetLibDir, a.getArtifactId(), a.getVersion(), deps.get(a));
		}
	}

	private void generateCopyright() throws IOException
	{
		File targetDocDir = new File(stageDir, "usr/share/doc/" + packageName);
		targetDocDir.mkdirs();

		PrintWriter out = new PrintWriter(new FileWriter(new File(targetDocDir, "copyright")));
		out.println(packageName);
		out.println(projectUrl);
		out.println();
		out.printf("Copyright %d %s\n", Calendar.getInstance().get(Calendar.YEAR), projectOrganization);
		out.println();
		out.println("The entire code base may be distributed under the terms of the GNU General");
		out.println("Public License (GPL).");
		out.println();
		out.println("See /usr/share/common-licenses/GPL");
		out.close();
	}

	private void generateControl(File target) throws IOException
	{
		getLog().info("Generating control file: "+target);
		PrintWriter out = new PrintWriter(new FileWriter(target));

		out.println("Package: "+packageName);
		out.println("Version: "+getPackageVersion());

		if (packageSection != null)
			out.println("Section: "+packageSection);
		if (packagePriority != null)
			out.println("Priority: "+packagePriority);
		out.println("Architecture: "+packageArchitecture);
		if (packageDependencies != null && packageDependencies.length > 0)
			out.println("Depends: " + StringUtils.join(processVersion(packageDependencies), ", "));

		out.printf("Installed-Size: %d\n", 1 + FileUtils.sizeOfDirectory(stageDir) / 1024);

		if (maintainerName != null || maintainerEmail != null)
		{
			out.print("Maintainer:");
			if (maintainerName != null)
				out.print(" "+maintainerName);
			if (maintainerEmail != null)
				out.printf(" <%s>", maintainerEmail);
			out.println();
		}

		if (projectUrl != null)
			out.println("Homepage: "+projectUrl);

		if (packageTitle != null) {
			if (packageTitle.length() > 60)
			{
				getLog().warn("Package title will be truncated to the upper limit of 60 characters.");
				out.println("Description: "+packageTitle.substring(0, 60));
			}
			else
				out.println("Description: "+packageTitle);

			out.println(getFormattedDescription());
		}

		out.close();
	}
	
	private String getFormattedDescription()
	{
		String desc = packageDescription.trim();
		desc.replaceAll("\\s+", " ");
		
		return " " + desc;
	}
	
	private void generateConffiles(File target) throws IOException
	{
		List<String> conffiles = new Vector<String>();

		File configDir = new File(stageDir, "etc");
		if (configDir.exists())
		{
			Collection<File> files = FileUtils.listFiles(configDir, null, true);
			for (File f : files)
			{
				if (f.isFile())
					conffiles.add(f.toString().substring(stageDir.toString().length()));
			}
		}

		if (conffiles.size() > 0)
		{
			PrintWriter out = new PrintWriter(new FileWriter(target));
			for (String fname : conffiles)
				out.println(fname);
			out.close();
		}
	}

	private void generateMd5Sums(File target) throws IOException
	{
		PrintWriter out = new PrintWriter(new FileWriter(target));
		
		Collection<File> files = FileUtils.listFiles(stageDir, null, true);
		for (File f : files)
		{
			// check whether the file is a non-regular file
			if (!f.isFile())
				continue;
			
			// check whether the file is a possible link
			if (!f.getAbsolutePath().equals(f.getCanonicalPath()))
				continue;

			String fname = f.toString().substring(stageDir.toString().length() + 1);
			if (!fname.startsWith("DEBIAN"))
			{
				FileInputStream fis = new FileInputStream(f);
				String md5 = DigestUtils.md5Hex(fis);
				fis.close();
				
				out.printf("%s  %s\n", md5, fname);
			}
		}
		
		out.close();
	}
	
	private void generateManPages() throws MojoExecutionException, ExecuteException, IOException
	{
		File source = new File(sourceDir, "man");
		if (!source.exists())
		{
			getLog().info("No manual page directory found: "+source);
			return;
		}

		int npages = 0;
		Collection<File> files = FileUtils.listFiles(source, null, true);
		for (File f : files)
		{
			if (f.isFile() && f.getName().matches(".*[.][1-9]$"))
			{
				char section = f.getName().charAt(f.getName().length()-1);
				File target = new File(stageDir, String.format("usr/share/man/man%c/%s.gz", section, f.getName()));
				target.getParentFile().mkdirs();

				CommandLine cmdline = new CommandLine("groff");
				cmdline.addArguments(new String[]{"-man", "-Tascii", f.getPath()});

				getLog().info("Start process: "+cmdline);

				GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(target));
				try
				{
					PumpStreamHandler streamHandler = new PumpStreamHandler(os, new LogOutputStream(getLog()));
					DefaultExecutor exec = new DefaultExecutor();
					exec.setWorkingDirectory(f.getParentFile());
					exec.setStreamHandler(streamHandler);
					int exitval = exec.execute(cmdline);
					if (exitval == 0)
						getLog().info("Manual page generated: "+target.getPath());
					else
					{
						getLog().warn("Exit code "+exitval);
						throw new MojoExecutionException("Process returned non-zero exit code: "+cmdline);
					}
				}
				finally
				{
					os.close();
				}

				npages++;
			}
		}

		if (npages == 0)
			getLog().info("No manual pages found in directory: "+source);
	}

	private void generatePackage() throws IOException, MojoExecutionException
	{
		runProcess(new String[]{"fakeroot", "--", "dpkg-deb", "--build", stageDir.toString(), getPackageFile().toString()}, true);
	}

	private void checkDeprecated(boolean haveParameter, String paramName) throws MojoExecutionException
	{
		if (haveParameter)
			throw new MojoExecutionException("Deprecated parameter used: "+paramName);
	}

	public void execute() throws MojoExecutionException
	{
		checkDeprecated(includeJar != null, "includeJar");
		checkDeprecated(includeJars != null && includeJars.length > 0, "includeJars");
		checkDeprecated(excludeAllJars != null, "excludeAllJars");

		File targetDebDir = new File(stageDir, "DEBIAN");
		if (!targetDebDir.exists() && !targetDebDir.mkdirs())
			throw new MojoExecutionException("Unable to create directory: "+targetDebDir);

		try
		{
			generateManPages();
			copyAttachedArtifacts();
			copyArtifacts();
			generateCopyright();
			generateConffiles(new File(targetDebDir, "conffiles"));
			generateControl(new File(targetDebDir, "control"));
			generateMd5Sums(new File(targetDebDir, "md5sums"));
			generatePackage();
		}
		catch (IOException e)
		{
			getLog().error(e.toString());
			throw new MojoExecutionException(e.toString());
		}
	}
}

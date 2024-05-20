package net.sf.debianmaven;

import com.github.fracpete.processoutput4j.output.CollectingProcessOutput;
import net.sf.debianmaven.utils.DefaultFileCopy;
import net.sf.debianmaven.utils.FileCopy;
import net.sf.debianmaven.utils.FilteredFileCopy;
import net.sf.debianmaven.utils.IOUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Generates a Debian package.
 * <br>
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
	 * @parameter
	 * @since 1.0.7
	 */
	protected Set<String> excludeArtifactsRegExp;

	protected Set<Pattern> excludeArtifactsPattern;

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
	 * @parameter default-value="/usr/lib/{PKGNAME}"
	 * @since 1.0.11
	 */
	protected String libDirectory;

	/**
	 * @parameter default-value="/usr/share/doc/{PKGNAME}"
	 * @since 1.0.11
	 */
	protected String docDirectory;

	/**
	 * @parameter default-value="/usr/share/man"
	 * @since 1.0.11
	 */
	protected String manDirectory;

	/**
	 * @parameter default-value="true"
	 * @since 1.0.11
	 */
	protected boolean createSymLinks;

	/**
	 * @parameter default-value="true"
	 * @since 1.0.11
	 */
	protected boolean createIncludeFiles;

	/**
	 * @parameter default-value="false"
	 * @since 1.0.12
	 */
	protected boolean useDefaultFixPermissions;

	/**
	 * @parameter
	 * @since 1.0.12
	 */
	protected List<FixPermission> fixPermissions;

	protected List<FixPermission> activeFixPermissions;

	/**
	 * @parameter default-value="false"
	 * @since 1.0.12
	 */
	protected boolean useDefaultCopyResources;

	/**
	 * @parameter
	 * @since 1.0.12
	 */
	protected List<CopyResource> copyResources;

	protected List<CopyResource> activeCopyResources;

	/**
	 * @parameter
	 * @since 1.0.13
	 */
	protected FileFiltering fileFiltering;

	/**
	 * @parameter default-value="compile"
	 * @since 1.0.19
	 */
	protected String includeScope;

	protected Set<String> includedScopes;

	/**
	 * @since 1.0.19
	 */
	protected String excludeScope;

	protected Set<String> excludedScopes;

	/**
	 * @parameter default-value="gzip"
	 * @since 1.0.21
	 */
	protected String compressionType;

	/**
	 * @parameter default-value="false"
	 * @since 1.0.23
	 */
	protected boolean attach;

	/**
	 * The Maven project object
	 * 
	 * @parameter expression="${project}"
	 */
	private MavenProject project;

	/**
	 * @component
	 */
	private MavenProjectHelper projectHelper;

	/**
	 * Strips leading and trailing slashes and dots from the path.
	 * @param path the path to strip
	 * @return the clean path
	 */
	private String stripPath(String path)
	{
		// start
		StringBuilder result = new StringBuilder();
		boolean start = true;
		for (int i = 0; i < path.length(); i++)
		{
			char c = path.charAt(i);
			if (start)
			{
				if ((c == '/') || (c == '.'))
					continue;
				start = false;
			}
			result.append(c);
		}
		// end
		path = result.toString();
		result = new StringBuilder();
		boolean end = true;
		for (int i = path.length() - 1; i >= 0; i--)
		{
			char c = path.charAt(i);
			if (end)
			{
				if ((c == '/') || (c == '.'))
					continue;
				end = false;
			}
			result.insert(0, c);
		}
		return result.toString();
	}

	private String getLibDirectory()
	{
		String result;
		if ((libDirectory == null) || libDirectory.isEmpty())
			result = "/usr/lib/{PGKNAME}";
		else
			result = libDirectory;
		result = result.replace("{PKGNAME}", packageName);
		return result;
	}

	private String getDocDirectory()
	{
		String result;
		if ((docDirectory == null) || docDirectory.isEmpty())
			result = "/usr/share/doc/{PGKNAME}";
		else
			result = docDirectory;
		result = result.replace("{PKGNAME}", packageName);
		return result;
	}

	private String getManDirectory()
	{
		String result;
		if ((manDirectory == null) || manDirectory.isEmpty())
			result = "/usr/share/man";
		else
			result = manDirectory;
		result = result.replace("{PKGNAME}", packageName);
		return result;
	}

	private List<FixPermission> getActiveFixPermissions()
	{
		if (activeFixPermissions == null)
		{
			activeFixPermissions = new ArrayList<>();
			if (useDefaultFixPermissions)
			{
				try
				{
					activeFixPermissions.add(new FixPermission(".*\\/bin\\/.*", "rwxr-xr-x"));
					activeFixPermissions.add(new FixPermission(".*\\/sbin\\/.*", "rwxr-xr-x"));
					activeFixPermissions.add(new FixPermission(".*\\/DEBIAN\\/post.*", "rwxr-xr-x"));
					activeFixPermissions.add(new FixPermission(".*\\/DEBIAN\\/pre.*", "rwxr-xr-x"));
				}
				catch (Exception e)
				{
					getLog().error("Failed to setup default fix permissions!", e);
				}
			}
			if (fixPermissions != null)
			{
				for (FixPermission fp : fixPermissions)
				{
					if (fp.isValid())
						activeFixPermissions.add(fp);
					else
						getLog().warn("Fix permission is not valid, skipping: " + fp);
				}
			}
		}
		return activeFixPermissions;
	}

	private List<CopyResource> getActiveCopyResources()
	{
		if (activeCopyResources == null)
		{
			activeCopyResources = new ArrayList<>();
			if (useDefaultCopyResources)
			{
				if ((sourceDir != null) && sourceDir.exists() && new File(sourceDir, "resources").exists())
				{
					File resourcesDir = new File(sourceDir, "resources");
					activeCopyResources.add(
						new CopyResource(
							resourcesDir.getAbsolutePath(),
							stageDir.getAbsolutePath(),
							".*"));
					getLog().info("using default copy resources: " + resourcesDir + " -> " + stageDir);
				}
				else
				{
					getLog().warn("'useDefaultCopyResources' is enabled, but '" + sourceDir + "/resources" + "' does not exist. Skipping!");
				}
			}
			if (copyResources != null)
			{
				activeCopyResources.addAll(copyResources);
			}
		}
		return activeCopyResources;
	}

	private File createTargetLibDir() throws IOException
	{
		File targetLibDir = new File(stageDir, stripPath(getLibDirectory()));
		if (!targetLibDir.mkdirs() && !targetLibDir.exists())
			throw new IOException("Failed to create lib directory: " + targetLibDir);
		return targetLibDir;
	}

	private void createSymlink(File symlink, String target) throws MojoExecutionException, IOException
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
			out.write(String.format("artifacts=%s\n", StringUtils.join(new HashSet<>(dependencies), ":")));
		}
		finally
		{
			out.close();
		}

		if (createSymLinks)
			createSymlink(new File(targetLibDir, String.format("%s.inc", artifactId)), deplist.getName());
	}

	/**
	 * Turns the supplied regular expressions from excludeArtifactsRegExp into {@link Pattern} objects.
	 * @return the compiled Pattern objects
	 */
	private Set<Pattern> getExcludeArtifactsPattern()
	{
		if (excludeArtifactsPattern == null)
		{
			excludeArtifactsPattern = new HashSet<>();
			if (excludeArtifactsRegExp != null)
			{
				for (String regexp : excludeArtifactsRegExp)
				{
					try
					{
						Pattern p = Pattern.compile(regexp);
						excludeArtifactsPattern.add(p);
					}
					catch (Exception e)
					{
						getLog().error("Failed to parse excludeArtifactsPattern '" + regexp + "'!", e);
					}
				}
			}
		}
		return excludeArtifactsPattern;
	}

	/**
	 * Turns the comma-separated list of scopes into a set. empty set means all are included.
	 * @return the set of included scopes
	 */
	private Set<String> getIncludedScopes() {
		if (includedScopes == null)
		{
			includedScopes = new HashSet<>();
			if ((includeScope != null) && !includeScope.isEmpty())
			{
				if (includeScope.contains(","))
				{
					includedScopes.addAll(Arrays.asList(includeScope.split(",")));
				}
				else
				{
					includedScopes.add(includeScope);
				}
			}
		}
		return includedScopes;
	}

	/**
	 * Turns the comma-separated list of scopes into a set. empty set means none are excluded.
	 * @return the set of excluded scopes
	 */
	private Set<String> getExcludedScopes() {
		if (excludedScopes == null)
		{
			excludedScopes = new HashSet<>();
			if ((excludeScope != null) && !excludeScope.isEmpty())
			{
				if (excludeScope.contains(","))
				{
					excludedScopes.addAll(Arrays.asList(excludeScope.split(",")));
				}
				else
				{
					excludedScopes.add(excludeScope);
				}
			}
		}
		return excludedScopes;
	}

	private String getCompressionType() {
		return compressionType == null? "gzip" : compressionType;
	}

	private boolean includeArtifact(Artifact a)
	{
		String aStr = a.getGroupId() + ":" + a.getArtifactId() + ":" + (a.hasClassifier() ? a.getClassifier() : "");
		boolean doExclude = excludeArtifacts != null && (a.getDependencyTrail() == null || Collections.disjoint(a.getDependencyTrail(), excludeArtifacts));
		if (!doExclude && !getExcludeArtifactsPattern().isEmpty())
		{
			for (Pattern p: getExcludeArtifactsPattern())
			{
				if (p.matcher(aStr).matches())
				{
					getLog().debug(aStr + " excluded using pattern: " + p.pattern());
					doExclude = true;
					break;
				}
			}
		}

		if (a.getScope() != null)
		{
			if (!getIncludedScopes().isEmpty() && !getIncludedScopes().contains(a.getScope()))
			{
				getLog().debug(aStr + " (" + a.getScope() + ") not part of included scopes: " + getIncludedScopes());
				doExclude = true;
			}

			if (!getExcludedScopes().isEmpty() && getExcludedScopes().contains(a.getScope()))
			{
				getLog().debug(aStr + " (" + a.getScope() + ") part of excluded scopes: " + getExcludedScopes());
				doExclude = true;
			}
		}

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

		//TODO: which version should we use? trying both versions for now...
		String linkname = src.getName().replaceFirst("-"+a.getBaseVersion(), "");
		if (linkname.equals(src.getName()))
			linkname = linkname.replaceFirst("-"+a.getVersion(), "");

		if (createSymLinks && !linkname.equals(src.getName()))
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
		{
			if (includeArtifact(a))
				copyArtifact(a, targetLibDir);
		}
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

		// consider the current artifact only if it exists (e.g. pom, war packaging generates no artifact)
		if (project.getArtifact().getFile() != null)
			artifacts.add(project.getArtifact());

		if (excludeAllDependencies)
			getLog().info("Copying regular project artifacts but not dependencies.");
		else
		{
			getLog().info("Copying regular project artifacts and dependencies.");
			for (Artifact a : (Collection<Artifact>)project.getArtifacts())
			{
				if (a.getScope().equals("runtime") || a.getScope().equals("compile"))
					artifacts.add(a);
			}
		}

		/*
		 * TODO: this code doesn't work as it should due to limitations of Maven API; see also:
		 * http://jira.codehaus.org/browse/MNG-4831
		 */

		Map<String,Artifact> ids = new HashMap<>();
		for (Artifact a : artifacts)
			ids.put(a.getId(), a);

		MultiMap deps = new MultiValueMap();
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
			if (includeArtifact(a) && createIncludeFiles)
				writeIncludeFile(targetLibDir, a.getArtifactId(), a.getVersion(), (List) deps.get(a));
		}
	}

	private void generateCopyright() throws IOException
	{
		File targetDocDir = new File(stageDir, stripPath(getDocDirectory()));
		if (!targetDocDir.mkdirs() && !targetDocDir.exists())
			throw new IOException("Failed to created doc directory: " + targetDocDir);

		PrintWriter out = new PrintWriter(new FileWriter(new File(targetDocDir, "copyright")));
		out.println("Package: " + packageName);
		out.println("URL: " + projectUrl);
		out.println();
		out.printf("Copyright %d %s\n", Calendar.getInstance().get(Calendar.YEAR), projectOrganization);
		out.println();
		for (Object o: project.getLicenses())
		{
			License l = (License) o;
			out.println(l.getName());
			if ((l.getUrl() != null) && !l.getUrl().isEmpty())
				out.println(l.getUrl());
			if ((l.getComments() != null) && !l.getComments().isEmpty())
				out.println(l.getComments());
			out.println();
		}
		out.close();
	}

	private void copyResources() throws IOException
	{
		if (fileFiltering == null)
		{
			getLog().debug("File filter is null, instantiating default");
			fileFiltering = new FileFiltering();
		}
		getLog().info("File filtering: " + fileFiltering);

		FileCopy fileCopy;
		if (!fileFiltering.getEnabled())
		{
			fileCopy = new DefaultFileCopy();
		}
		else
		{
			Map<String,String> additional = new HashMap<>();
			additional.put("packageName", packageName);
			additional.put("packageTitle", packageTitle);
			additional.put("packagePriority", packagePriority);
			additional.put("packageSection", packageSection);
			additional.put("packageDescription", packageDescription);
			additional.put("packageArchitecture", packageArchitecture);
			additional.put("packageRevision", packageRevision);
			fileCopy = new FilteredFileCopy(project.getModel(), fileFiltering.getIncludePattern(), fileFiltering.getExcludePattern(), additional);
		}
		fileCopy.setLog(getLog());

		for (CopyResource copyResource: getActiveCopyResources())
		{
			getLog().info("Copy resources using: " + copyResource);
			IOUtils.copy(
				getLog(),
				new File(copyResource.getSource()),
				new File(copyResource.getTarget()),
				copyResource.getIncludePattern(),
				fileCopy);
		}
	}

	private void applyFixPermissions() throws IOException
	{
		List<FixPermission> active = getActiveFixPermissions();
		if (!active.isEmpty())
			IOUtils.applyFixPermissions(getLog(), stageDir, active);
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
		List<String> conffiles = new ArrayList<>();

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

		if (!conffiles.isEmpty())
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
	
	private void generateManPages() throws MojoExecutionException, IOException
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
				File target = new File(stageDir, String.format(stripPath(getManDirectory()) + "/man%c/%s.gz", section, f.getName()));
				if (!target.getParentFile().mkdirs() && !target.exists())
					throw new IOException("Failed to create man pages directory: " + target);

				String[] cmd = new String[]{"groff", "-man", "-Tascii", f.getPath()};
				ProcessBuilder builder = new ProcessBuilder();
				builder.command(cmd);
				builder.directory(f.getParentFile());

				getLog().info("Start process: " + Arrays.asList(cmd));

				GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(target));
				try
				{
					CollectingProcessOutput output = new CollectingProcessOutput();
					output.monitor(builder);
					int exitval = output.getExitCode();
					if (exitval == 0)
						getLog().info("Manual page generated: " + target.getPath());
					else
					{
						getLog().warn("Exit code: " + exitval);
						getLog().warn("stderr:\n" + output.getStdErr());
						getLog().warn("stdout:\n" + output.getStdOut());
						throw new MojoExecutionException("Process returned non-zero exit code: " + Arrays.asList(cmd));
					}
				}
				catch (MojoExecutionException e)
				{
					throw e;
				}
				catch (Exception e)
				{
					throw new IOException(e);
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
		List<String> args = new ArrayList<>();
		args.add("fakeroot");
		args.add("--");
		args.add("dpkg-deb");
		if (!getCompressionType().equals("sysdefault"))
		{
			args.add("-Z");
			args.add(getCompressionType());
		}
		args.add("--build");
		args.add(stageDir.toString());
		args.add(getPackageFile().toString());
		runProcess(args.toArray(new String[0]), true);
	}

	private void checkDeprecated(boolean haveParameter, String paramName) throws MojoExecutionException
	{
		if (haveParameter)
			throw new MojoExecutionException("Deprecated parameter used: "+paramName);
	}

	protected void executeDebMojo() throws MojoExecutionException
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
			copyResources();
			applyFixPermissions();
			generateConffiles(new File(targetDebDir, "conffiles"));
			generateControl(new File(targetDebDir, "control"));
			generateMd5Sums(new File(targetDebDir, "md5sums"));
			generatePackage();
			attachPackage();
		}
		catch (IOException e)
		{
			getLog().error(e.toString());
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void attachPackage()
	{
		if(attach)
		{
			projectHelper.attachArtifact(project, "deb", getPackageFile());
		}
	}
}

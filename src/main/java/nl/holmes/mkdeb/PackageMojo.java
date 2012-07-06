package nl.holmes.mkdeb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Generates a Debian package.
 *
 * Uses Debian utilities: <a href="http://www.debian.org/doc/manuals/debian-faq/ch-pkgtools.en.html">dpkg-deb</a> and fakeroot.
 *
 * @goal package
 * @phase package
 */
public class PackageMojo extends AbstractDebianMojo
{
	private static void copyTree(File sourceDir, File targetDir, Log log)
			throws IOException
	{
		log.debug("Searching for package files in "+sourceDir);
		if (!sourceDir.exists())
		{
			log.warn("Resource directory does not exist: "+sourceDir);
			return;
		}
		
		Collection<File> files = FileUtils.listFiles(sourceDir, null, true);
		for (File src : files) {
			if (src.isDirectory())
				src.mkdirs();
			else if (src.isFile()) {
				String fname = src.toString().substring(
						sourceDir.toString().length() + 1);
				File target = new File(targetDir, fname);

				FileInputStream is = new FileInputStream(src);
				FileOutputStream os = new FileOutputStream(target);

				log.info(String.format("Copying %s.", src.toString()));
				IOUtils.copy(is, os);
				is.close();
				os.close();
			}
		}
	}
	
	private String getStrippedBasename(final String filename)
	{
		int idx = filename.indexOf(packageVersion);
		if (idx < 2)
			return filename;
		
		if (filename.charAt(idx-1) == '-')
			idx--;

		return filename.substring(0, idx);
	}

	private String getExtension(final String filename)
	{
		int idx = filename.lastIndexOf(".");
		if (idx < 1)
			return filename;
		else
			return filename.substring(idx);
	}

	private String stripVersion(final String filename)
	{
		return getStrippedBasename(filename) + getExtension(filename);
	}

	private void copyJars() throws IOException, MojoExecutionException
	{
		List<String> allJars = new Vector<String>();
		if (jars != null)
			allJars.addAll(Arrays.asList(jars));
		else if (defaultJar != null)
			allJars.add(defaultJar);

		File targetLibDir = new File(stageDir, "usr/share/lib/" + packageName);
		targetLibDir.mkdirs();

		for (String jarname : allJars)
		{
			File srcFile = new File(targetDir, jarname);
			String basename = srcFile.getName();
			File targetFile = new File(targetLibDir, basename);
			
			FileInputStream is = new FileInputStream(srcFile);
			FileOutputStream os = new FileOutputStream(targetFile);

			getLog().info(String.format("Copying %s to %s", srcFile.toString(), targetFile.toString()));
			IOUtils.copyLarge(is, os);
			is.close();
			os.close();
			
			String strippedname = stripVersion(basename);
			if (!basename.equals(strippedname))
			{
				File symlink = new File(targetLibDir, strippedname);
				if (symlink.exists())
					symlink.delete();
				runProcess(new String[]{"ln", "-s", basename, symlink.toString()}, true);
			}
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
		out.println("Version: "+packageVersion);

		if (packageSection != null)
			out.println("Section: "+packageSection);
		if (packagePriority != null)
			out.println("Priority: "+packagePriority);
		out.println("Architecture: "+packageArchitecture);
		if (packageDependencies != null && packageDependencies.length > 0)
			out.println("Depends: " + StringUtils.join(packageDependencies, ", "));

		out.printf("Installed-Size: %d\n", 1 + FileUtils.sizeOfDirectory(stageDir) / 1024);

		out.printf("Maintainer: %s <%s>\n", maintainerName, maintainerEmail);
		if (projectUrl != null)
			out.println("Homepage: "+projectUrl);

		if (packageTitle != null) {
			if (packageTitle.length() > 60)
				getLog().warn("Package title is longer than the upper limit of 60 characters.");
			out.println("Description: "+packageTitle);
		}

		out.println(getFormattedDescription());

		out.close();
	}
	
	private String getFormattedDescription()
	{
		String desc = packageDescription.trim();
		desc.replaceAll("\\s+", " ");
		
		return " " + desc;
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
	
	private void generatePackage() throws IOException, MojoExecutionException
	{
		runProcess(new String[]{"fakeroot", "dpkg-deb", "--build", stageDir.toString(), getPackageFile().toString()}, true);
	}

	public void execute() throws MojoExecutionException
	{
		File targetDebDir = new File(stageDir, "DEBIAN");
		if (!targetDebDir.exists() && !targetDebDir.mkdirs())
			throw new MojoExecutionException("Unable to create directory: "+targetDebDir);

		try
		{
			//copyTree(sourceDir, stageDir, getLog());
			copyJars();
			generateCopyright();
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

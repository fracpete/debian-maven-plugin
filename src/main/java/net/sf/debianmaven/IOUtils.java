/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * FileUtils.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package net.sf.debianmaven;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helper class for I/O related operations.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class IOUtils
{

	/**
	 * Applies the patterns to fix file permissions.
	 * @param log for logging output
	 * @param dir the directory to process
	 * @param fixPermissions the permissions to apply
	 * @throws IOException if application of permissions fails
	 */
	public static void applyFixPermissions(Log log, File dir, List<FixPermission> fixPermissions) throws IOException
	{
		log.debug("Fixing permissions in: " + dir);

		// directory itself
		for (FixPermission fixPermission: fixPermissions)
		{
			if (fixPermission.appliesTo(dir))
			{
				log.debug("'" + fixPermission + "' applies to: " + dir);
				fixPermission.applyTo(dir);
			}
		}

		// iterate files/dirs
		File[] files = dir.listFiles();
		if (files != null)
		{
			for (File file: files)
			{
				if (file.isDirectory())
					applyFixPermissions(log, file, fixPermissions);
				else
				{
					for (FixPermission fixPermission: fixPermissions)
					{
						if (fixPermission.appliesTo(file))
						{
							log.debug("'" + fixPermission + "' applies to: " + file);
							fixPermission.applyTo(file);
						}
					}
				}
			}
		}
	}

	/**
	 * Copies or moves files and directories (recursively).
	 * If targetLocation does not exist, it will be created.
	 * <br><br>
	 * Original code from <a href="http://www.java-tips.org/java-se-tips/java.io/how-to-copy-a-directory-from-one-location-to-another-loc.html" target="_blank">Java-Tips.org</a>.
	 *
	 * @param log for logging
	 * @param sourceLocation	the source file/dir
	 * @param targetLocation	the target file/dir
	 * @param move		if true then the source files/dirs get deleted
	 * 				as soon as copying finished
	 * @param atomic		whether to perform an atomic move operation
	 * @param include   the regular expression pattern for including files/dirs
	 * @return			false if failed to delete when moving or failed to create target directory
	 * @throws IOException	if copying/moving fails
	 */
	public static boolean copyOrMove(Log log, File sourceLocation, File targetLocation, boolean move, boolean atomic, Pattern include) throws IOException
	{
		String[] children;
		int i;
		Path source;
		Path target;

		if (sourceLocation.isDirectory()) {
			log.debug("Entering source dir: " + sourceLocation);
			if (!targetLocation.exists()) {
				if (!targetLocation.mkdirs() && !targetLocation.exists())
				{
					log.error("Failed to create target directory: " + targetLocation);
					return false;
				}
			}

			children = sourceLocation.list();
			for (i = 0; i < children.length; i++) {
				if (include.matcher(new File(sourceLocation.getAbsoluteFile(), children[i]).getAbsolutePath()).matches())
				{
					if (!copyOrMove(
						log,
						new File(sourceLocation.getAbsoluteFile(), children[i]),
						new File(targetLocation.getAbsoluteFile(), children[i]),
						move, atomic, include))
						return false;
				}
			}

			if (move)
				return sourceLocation.delete();
			else
				return true;
		}
		else {
			source = FileSystems.getDefault().getPath(sourceLocation.getAbsolutePath());
			if (include.matcher(source.toAbsolutePath().toString()).matches())
			{
				if (targetLocation.isDirectory())
					target = FileSystems.getDefault().getPath(targetLocation.getAbsolutePath() + File.separator + sourceLocation.getName());
				else
					target = FileSystems.getDefault().getPath(targetLocation.getAbsolutePath());
				if (move)
				{
					log.debug("Moving '" + sourceLocation + "' to '" + target + "'");
					if (atomic)
						Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
					else
						Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
				}
				else
				{
					log.debug("Copying '" + sourceLocation + "' to '" + target + "'");
					Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			return true;
		}
	}
}

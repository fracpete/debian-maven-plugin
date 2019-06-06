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
 * FixPermission.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package net.sf.debianmaven;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parameter container for fixing permissions.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class FixPermission implements Serializable
{

	private String include;

	private Pattern includePattern;

	private String permissions;

	private Set<PosixFilePermission> permissionsSet;

	/**
	 * Default constructor.
	 */
	public FixPermission()
	{
		include = null;
		includePattern = null;
		permissions = null;
		permissionsSet = null;
	}

	/**
	 * Uses the provided include, permissions.
	 * @param include the include pattern
	 * @param permissions the POSIX permissions
	 * @throws PatternSyntaxException if include pattern is invalid
	 * @throws IllegalArgumentException if permissions are invalid
	 * @see #setInclude(String)
	 * @see #setPermissions(String)
	 */
	public FixPermission(String include, String permissions) throws PatternSyntaxException, IllegalArgumentException
	{
		this();
		setInclude(include);
		setPermissions(permissions);
	}

	/**
	 * Sets the include regular expression.
	 * @param value the expression
	 * @throws PatternSyntaxException if the string is not a value regular expression
	 */
	public void setInclude(String value) throws PatternSyntaxException
	{
		includePattern = Pattern.compile(value);
		include = value;
	}

	/**
	 * Returns the include regular expression.
	 * @return the expression, null if none set
	 */
	public String getInclude()
	{
		return include;
	}

	/**
	 * Returns the compiled include pattern.
	 * @return the pattern, null if none compiled
	 * @see #setInclude(String)
	 */
	public Pattern getIncludePattern()
	{
		return includePattern;
	}

	/**
	 * Sets the POSIX permissions string (eg 'rwxr-xr-x').
	 * @param value the permissions
	 * @throws IllegalArgumentException if permissions invalid
	 */
	public void setPermissions(String value) throws IllegalArgumentException
	{
		permissionsSet = PosixFilePermissions.fromString(value);
		permissions = value;
	}

	/**
	 * Returns the POSIX permissions string.
	 * @return the permissions, null if none set
	 */
	public String getPermissions()
	{
		return permissions;
	}

	/**
	 * Returns the parsed POSIX file permissions.
	 * @return the permissions, null if none set
	 */
	public Set<PosixFilePermission> getPermissionsSet()
	{
		return permissionsSet;
	}

	/**
	 * Checks whether the setup is valid.
	 * @return true if include pattern and permissions set present
	 */
	public boolean isValid()
	{
		return (includePattern != null) && (permissionsSet != null);
	}

	/**
	 * Checks whether the file is handled by this setup.
	 * @param file the file to check
	 * @return true if handled
	 * @see #isValid()
	 */
	public boolean appliesTo(File file)
	{
		return isValid() && includePattern.matcher(file.getAbsolutePath()).matches();
	}

	/**
	 * Applies the POSIX permissions to the file (if the include pattern triggers).
	 * @param file the file to update
	 * @return true if permissions updated
	 * @throws IOException if applying of permissions failed
	 */
	public boolean applyTo(File file) throws IOException
	{
		if (appliesTo(file))
		{
			Files.setPosixFilePermissions(file.toPath(), permissionsSet);
			return true;
		}
		return false;
	}

	/**
	 * Returns a short description of the setup.
	 * @return the description
	 */
	public String toString()
	{
		return "include=" + (include == null ? "???" : include)
			+ ", permissions=" + (permissions == null ? "???" : permissions);
	}
}

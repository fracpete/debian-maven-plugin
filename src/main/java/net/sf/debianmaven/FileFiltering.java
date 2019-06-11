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
 * FileFiltering.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package net.sf.debianmaven;

import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * For defining file filtering.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class FileFiltering implements Serializable
{
	public final static String DEFAULT_INCLUDE = ".*";

	public final static String DEFAULT_EXCLUDE = ".*\\.(jpg|jpeg|png|svg|zip|jar|pdf)";

	private boolean enabled;

	private String include;
	
	private Pattern includePattern;

	private String exclude;
	
	private Pattern excludePattern;
	
	/**
	 * Default constructor.
	 */
	public FileFiltering()
	{
		enabled = false;
		include = null;
		includePattern = null;
		exclude = null;
		excludePattern = null;
	}

	/**
	 * Initializes with the specified source, target, include pattern.
	 * @param enabled whether filtering is enabled
	 * @param include the regular expression for including files
	 * @param exclude the regular expression for excluding files
	 * @throws PatternSyntaxException if include pattern invalid
	 * @see #setEnabled(boolean)
	 * @see #setInclude(String)
	 * @see #setExclude(String)
	 */
	public FileFiltering(boolean enabled, String include, String exclude) throws PatternSyntaxException
	{
		this();
		setEnabled(enabled);
		setInclude(include);
		setExclude(exclude);
	}

	/**
	 * Sets whether file filtering is enabled.
	 * @param value true if enabled
	 */
	public void setEnabled(boolean value)
	{
		enabled = value;
	}

	/**
	 * Returns whether file filtering is enabled.
	 * @return true if enabled
	 */
	public boolean getEnabled()
	{
		return enabled;
	}
	
	/**
	 * Sets the include regular expression.
	 * @param value the expression
	 * @throws PatternSyntaxException if the string is not a value regular expression
	 */
	public void setInclude(String value) throws PatternSyntaxException
	{
		if ((value == null) || value.isEmpty())
		{
			setInclude(DEFAULT_INCLUDE);
			return;
		}
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
	 * Returns the compiled include pattern. Uses .* if no include pattern was set.
	 * @return the pattern
	 * @see #setInclude(String)
	 */
	public Pattern getIncludePattern()
	{
		if ((include == null) || include.isEmpty())
			setInclude(DEFAULT_INCLUDE);
		return includePattern;
	}
	
	/**
	 * Sets the exclude regular expression.
	 * @param value the expression
	 * @throws PatternSyntaxException if the string is not a value regular expression
	 */
	public void setExclude(String value) throws PatternSyntaxException
	{
		if ((value == null) || value.isEmpty())
		{
			setExclude(DEFAULT_EXCLUDE);
			return;
		}
		excludePattern = Pattern.compile(value);
		exclude = value;
	}

	/**
	 * Returns the exclude regular expression.
	 * @return the expression, null if none set
	 */
	public String getExclude()
	{
		return exclude;
	}

	/**
	 * Returns the compiled exclude pattern. Uses .* if no exclude pattern was set.
	 * @return the pattern
	 * @see #setExclude(String)
	 */
	public Pattern getExcludePattern()
	{
		if ((exclude == null) || exclude.isEmpty())
			setExclude(DEFAULT_EXCLUDE);
		return excludePattern;
	}

	/**
	 * Returns a short description of the setup.
	 * @return the description
	 */
	public String toString()
	{
		return "enabled=" + enabled
			+ ", include=" + (include == null ? DEFAULT_INCLUDE : include)
			+ ", exclude=" + (exclude == null ? DEFAULT_EXCLUDE : exclude);
	}
}

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
 * CopyResource.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package net.sf.debianmaven;

import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * For defining source, target and pattern for copying resources.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class CopyResource implements Serializable
{
	private String source;
	
	private String target;
	
	private String include;
	
	private Pattern includePattern;

	/**
	 * Default constructor.
	 */
	public CopyResource()
	{
		source = null;
		target = null;
		include = null;
		includePattern = null;
	}

	/**
	 * Initializes with the specified source, target, include pattern.
	 * @param source the source
	 * @param target the target
	 * @param include the regular expression
	 * @throws PatternSyntaxException if include pattern invalid
	 * @see #setSource(String)
	 * @see #setTarget(String)
	 * @see #setInclude(String)
	 */
	public CopyResource(String source, String target, String include) throws PatternSyntaxException
	{
		this();
		setSource(source);
		setTarget(target);
		setInclude(include);
	}

	/**
	 * Sets the source.
	 * @param value the dir/file
	 */
	public void setSource(String value) 
	{
		source = value;
	}

	/**
	 * Returns the source.
	 * @return the dir/file, null if none set
	 */
	public String getSource()
	{
		return source;
	}
	
	/**
	 * Sets the target.
	 * @param value the dir/file
	 */
	public void setTarget(String value) 
	{
		target = value;
	}

	/**
	 * Returns the target.
	 * @return the dir/file, null if none set
	 */
	public String getTarget()
	{
		return target;
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
			setInclude(".*");
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
			setInclude(".*");
		return includePattern;
	}

	/**
	 * Returns a short description of the setup.
	 * @return the description
	 */
	public String toString()
	{
		return "source=" + (source == null ? "???" : source)
			+ ", target=" + (target == null ? "???" : target)
			+ ", include=" + (include == null ? ".*" : include);
	}
}

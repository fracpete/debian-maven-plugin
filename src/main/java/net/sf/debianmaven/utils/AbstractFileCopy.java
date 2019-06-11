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
 * AbstractFileCopy.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package net.sf.debianmaven.utils;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * Ancestor for file copying schemes.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public abstract class AbstractFileCopy implements FileCopy
{
	private Log log;

	/**
	 * Sets the log to use.
	 * @param log the log
	 */
  public void setLog(Log log)
  {
  	this.log = log;
  }

	/**
	 * Returns the log in use.
	 * @return the log
	 */
	public Log getLog()
	{
    if (this.log == null)
    {
      log = new SystemStreamLog();
    }
		return log;
	}
}

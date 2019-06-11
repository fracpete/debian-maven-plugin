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
 * DefaultFileCopy.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package net.sf.debianmaven.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Performs verbatim copying of files.
 */
public class DefaultFileCopy extends AbstractFileCopy
{

	/**
	 * Copies input file to output file.
	 *
	 * @param input the input file
	 * @param output the output file
	 * @throws IOException if copying fails
	 */
	public void copy(Path input, Path output) throws IOException
	{
		getLog().debug("Copying: " + input);
		Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
	}
}

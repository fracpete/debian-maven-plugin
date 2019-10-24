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
 * FilteredFileCopy.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package net.sf.debianmaven.utils;

import com.github.fracpete.simplemavenfilefiltering.FilterUtils;
import org.apache.maven.model.Model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Applies filtering while copying a file.
 */
public class FilteredFileCopy extends AbstractFileCopy
{
	private Model model;

	private Pattern include;

	private Pattern exclude;

	private Map<String,String> additional;

	/**
	 * Initializes the file copy.
	 *
	 * @param model the model to use for filtering files
	 * @param include the include pattern for files
	 * @param exclude the exclude pattern for files
	 */
	public FilteredFileCopy(Model model, Pattern include, Pattern exclude, Map<String,String> additional)
	{
		this.model = model;
		this.include = include;
		this.exclude = exclude;
		this.additional = additional;
	}

	/**
	 * Copies input file to output file.
	 *
	 * @param input the input file
	 * @param output the output file
	 * @throws IOException if copying fails
	 */
	public void copy(Path input, Path output) throws IOException
	{
		boolean filter;

		filter = include.matcher(input.toAbsolutePath().toString()).matches()
			&& !exclude.matcher(input.toAbsolutePath().toString()).matches();

		if (filter)
		{
			FilterUtils.filterFile(getLog(), input, output, model, additional);
		}
		else
		{
			getLog().debug("Copying: " + input);
			Files.createDirectories(output.getParent());
			Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
		}
	}

}

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
 * BeanUtils.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package net.sf.debianmaven.utils;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.maven.plugin.logging.Log;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.util.List;

/**
 * Bean related operations.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class BeanUtils
{

	/**
	 * Extracts the properties from an object.
	 *
	 * @param log for logging
	 * @param obj the object to inspect
	 * @return the properties
	 */
	public static PropertyDescriptor[] extractProperties(Log log, Object obj)
	{
    BeanInfo bi;

    if (obj == null)
    {
    	log.warn("Null object provided, can't extract bean properties!");
	    return new PropertyDescriptor[0];
    }

    try {
      bi = Introspector.getBeanInfo(obj.getClass());
      return bi.getPropertyDescriptors();
    }
		catch (Exception e)
		{
			log.error("Failed to extract properties from: " + obj.getClass(), e);
			return new PropertyDescriptor[0];
		}
	}

	/**
	 * Extracts the value from the Object using the specified property path.
	 *
	 * @param log for logging
	 * @param obj the object to extract the value from
	 * @param path the property path, broken up
	 * @return the value, null if failed to located
	 */
	protected static Object extractValue(Log log, Object obj, List<String> path)
	{
		PropertyDescriptor[] props;
		String current;
		int index;
		Object value;

		current = path.get(0);
		index = -1;
		if (current.contains("[") && current.endsWith("]"))
		{
			index = Integer.parseInt(current.substring(current.indexOf("[") + 1, current.indexOf("]")));
			current = current.substring(0, current.indexOf("["));
		}

		props = extractProperties(log, obj);
		for (PropertyDescriptor prop: props)
		{
			if (prop.getDisplayName().equals(current))
			{
				try
				{
					value = prop.getReadMethod().invoke(obj);
				}
				catch (Exception e)
				{
					value = null;
					log.error("Failed to obtain value from path '" + path.get(0) + "!", e);
				}

				if (index > -1)
				{
					if (value.getClass().isArray())
					{
						value = Array.get(value, index);
					}
					else if (value instanceof List)
					{
						value = ((List) value).get(index);
					}
					else
					{
						log.error("Cannot handle type (neither array nor list): "  + value.getClass());
					}
				}

				if (path.size() == 1)
				{
					return value;
				}
				else
				{
					return extractValue(log, value, path.subList(1, path.size()));
				}
			}
		}

		log.warn("Failed to locate path: " + current);
		return null;
	}

	/**
	 * Extracts the value from the Object using the specified property path.
	 *
	 * @param log for logging
	 * @param obj the object to extract the value from
	 * @param path the property path
	 * @return the value, null if failed to located
	 */
	public static Object extractValue(Log log, Object obj, String path)
	{
		String[] parts;

		if (path.contains("."))
		{
			parts = path.split("\\.");
		}
		else
		{
			parts = new String[]{path};
		}

		return extractValue(log, obj, Arrays.asList(parts));
	}
}

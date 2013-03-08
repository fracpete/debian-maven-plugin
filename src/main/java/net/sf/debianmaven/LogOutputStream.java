package net.sf.debianmaven;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.maven.plugin.logging.Log;

public class LogOutputStream extends OutputStream
{
	private final Log backend;
	private StringBuffer buffer = new StringBuffer();
	
	public LogOutputStream(Log backend)
	{
		this.backend = backend;
	}

	@Override
	public void write(int ch) throws IOException
	{
		if ((char)ch == '\n')
		{
			backend.warn(buffer.toString());
			buffer.setLength(0);
		}
		else
			buffer.append((char)ch);
	}
}

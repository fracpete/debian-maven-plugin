package net.sf.debianmaven;

import java.io.File;

public abstract class AbstractRepreproMojo extends AbstractDebianMojo
{
	/**
	 * @parameter expression="${deb.repository.location}"
	 * @required
	 */
	protected File repository;

	/**
	 * @parameter expression="${deb.reprepro.config}"
	 * @required
	 */
	protected File repreproConfigurationDir;
}

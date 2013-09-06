package pt.webdetails.cdf.dd;

import java.util.Locale;

import org.pentaho.platform.api.engine.IPluginResourceLoader;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.messages.LocaleHelper;

import pt.webdetails.cdf.dd.datasources.DataSourceManager;
import pt.webdetails.cdf.dd.datasources.IDataSourceManager;
import pt.webdetails.cdf.dd.plugin.resource.ResourceLoader;
import pt.webdetails.cpf.IPluginCall;
import pt.webdetails.cpf.repository.IRepositoryAccess;
import pt.webdetails.cpf.resources.IResourceLoader;
import pt.webdetails.cpf.utils.IPluginUtils;

public class PentahoCdeEnvironment implements ICdeEnvironment {
	
	private IPluginCall interPluginCall;
	private IPluginUtils pluginUtils;
    private IRepositoryAccess repositoryAccess;
	
	@Override
	public String getApplicationBaseUrl() {
		return PentahoSystem.getApplicationContext().getBaseUrl();
	}

	@Override
	public IDataSourceManager getDataSourceManager() {
		return DataSourceManager.getInstance();
	}

	@Override
	public IPluginCall getInterPluginCall() {
		return interPluginCall;
	}

	@Override
	public Locale getLocale() {
		return LocaleHelper.getLocale();
	}

	@Override
	public IPluginResourceLocationManager getPluginResourceLocationManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPluginUtils getPluginUtils() { 
		return pluginUtils;
	}

	@Override
	public IRepositoryAccess getRepositoryAccess() {
		return repositoryAccess;
	}

	@Override
	public IResourceLoader getResourceLoader() {
		return new ResourceLoader(PentahoSystem.get(IPluginResourceLoader.class, null));
	}

	@Override
	public void init() throws InitializationException {
	}

	@Override
	public void refresh() {		
	}

}

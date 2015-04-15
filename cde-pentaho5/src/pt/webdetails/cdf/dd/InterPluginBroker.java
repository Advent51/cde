package pt.webdetails.cdf.dd;

import org.apache.commons.lang.StringUtils;

import org.pentaho.platform.api.engine.IParameterProvider;
import pt.webdetails.cpf.PluginEnvironment;
import pt.webdetails.cpf.plugin.CorePlugin;
import pt.webdetails.cpf.plugincall.api.IPluginCall;
import pt.webdetails.cpf.plugincall.base.CallParameters;

import java.util.Iterator;

/**
 * at least put cdf stuff here
 */
public class InterPluginBroker {

  public static final String DATA_SOURCE_DEFINITION_METHOD_NAME = "listDataAccessTypes";

  public static String getCdfIncludes(String dashboard, String type, boolean debug, boolean absolute,
                                      String absRoot, String scheme) throws Exception {
    CallParameters params = new CallParameters();
    params.put("dashboardContent", dashboard);
    params.put("debug", debug);
    if (type != null) {
      params.put("dashboardType", type);
    }

    if (!StringUtils.isEmpty(absRoot)) {
      params.put("root", absRoot);
    }
    if (!StringUtils.isEmpty( scheme )) {
      params.put("scheme", scheme);
    }

    params.put( "absolute", absolute );

    //TODO: instantiate directly
    IPluginCall pluginCall = PluginEnvironment.env().getPluginCall( CorePlugin.CDF.getId(), "xcdf", "getHeaders" );
    
    return pluginCall.call( params.getParameters() );

  }

  public static String getDataSourceDefinitions(String plugin, String service, String method, boolean forceRefresh) throws Exception {
    IPluginCall pluginCall = PluginEnvironment.env().getPluginCall( plugin, null, method );
    CallParameters params = new CallParameters();
    params.put( "refreshCache", forceRefresh );
    return pluginCall.call( params.getParameters() );
  }

  public static String getCdfContext(String dashboard, String action, String viewId, IParameterProvider requestParams) throws Exception {
    CallParameters params = new CallParameters();
    params.put("path", dashboard);
    params.put("action", action);
    params.put("viewId", viewId);

    if ( requestParams != null ) {
      Iterator<String> iterator = requestParams.getParameterNames();

      while ( iterator.hasNext() ) {
        String paramName = iterator.next();
        if ( StringUtils.isEmpty( paramName ) ) {
          continue;
        }
        if ( requestParams.hasParameter( paramName ) ) {
          Object paramValue = requestParams.getParameter( paramName );
          if ( paramValue == null ) {
            continue;
          }

          params.put( paramName, StringUtils.join( (String[]) paramValue, null, 0, 1 ) );
        }
      }
    }

    IPluginCall pluginCall = PluginEnvironment.env().getPluginCall( CorePlugin.CDF.getId(), "xcdf", "getContext" );

    return pluginCall.call( params.getParameters() );

  }
}

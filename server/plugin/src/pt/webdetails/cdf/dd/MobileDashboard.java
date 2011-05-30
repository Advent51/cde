/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.webdetails.cdf.dd;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import net.sf.json.JSONObject;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository.ISolutionRepository;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import pt.webdetails.cdf.dd.render.DependenciesManager;
import pt.webdetails.cdf.dd.render.RenderComponents;
import pt.webdetails.cdf.dd.render.StringFilter;
import pt.webdetails.cdf.dd.structure.WcdfDescriptor;
import pt.webdetails.cdf.dd.structure.XmlStructure;
import pt.webdetails.cdf.dd.util.JsonUtils;

// Imports for the cache
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import pt.webdetails.cdf.dd.render.RenderMobileLayout;

/**
 *
 * @author pdpi
 */
public class MobileDashboard extends AbstractDashboard
{
  /* CONSTANTS */

  // Dashboard rendering
  private static Log logger = LogFactory.getLog(Dashboard.class);
  /* FIELDS */
  protected String template, header, content, footer;
  protected Date loaded;
  protected final static String TYPE = "mobile";

  public MobileDashboard(IParameterProvider pathParams, DashboardDesignerContentGenerator generator)
  {
    super(pathParams, generator);
    IPentahoSession userSession = PentahoSessionHolder.getSession();
    final ISolutionRepository solutionRepository = PentahoSystem.get(ISolutionRepository.class, userSession);


    final String absRoot = pathParams.hasParameter("root") ? !pathParams.getParameter("root").toString().isEmpty() ? "http://" + pathParams.getParameter("root").toString() : "" : "";
    final boolean absolute = (!absRoot.isEmpty()) || pathParams.hasParameter("absolute") && pathParams.getParameter("absolute").equals("true");

    final RenderMobileLayout layoutRenderer = new RenderMobileLayout();
    final RenderComponents componentsRenderer = new RenderComponents();

    try
    {
      final JSONObject json = (JSONObject) JsonUtils.readJsonFromInputStream(solutionRepository.getResourceInputStream(dashboardLocation, true));

      json.put("settings", getWcdf().toJSON());
      final JXPathContext doc = JXPathContext.newContext(json);

      final StringBuilder dashboardBody = new StringBuilder();

      dashboardBody.append(layoutRenderer.render(doc));
      dashboardBody.append(componentsRenderer.render(doc));

      // set all dashboard members
      this.content = replaceTokens(dashboardBody.toString(), absolute, absRoot);

      this.header = renderHeaders(pathParams, this.content.toString());
      this.loaded = new Date();
    }
    catch (Exception e)
    {
      logger.error(e);
    }
  }

  @Override
  public String render()
  {
    JSONObject json = new JSONObject();
    JSONObject meta = new JSONObject();
    meta.put("title", getWcdf().getTitle());
    meta.put("filters", "#filters");
    json.put("metadata", meta);
    json.put("content", this.content);
    json.put("headers", this.header + generator.getCdfContext());

    return json.toString(2);
  }

  public String getType()
  {
    return TYPE;
  }
}

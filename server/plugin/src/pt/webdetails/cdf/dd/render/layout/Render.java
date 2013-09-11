/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdf.dd.render.layout;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import pt.webdetails.cdf.dd.render.Renderer;
import pt.webdetails.cdf.dd.util.PropertyBag;
import pt.webdetails.cdf.dd.util.XPathUtils;

public abstract class Render
{
  private JXPathContext node;
  protected static final Log logger = LogFactory.getLog(Render.class);
  private PropertyBag propertyBag;

  public Render(JXPathContext node)
  {
    this.propertyBag = new PropertyBag();
    this.node = node;
  }

  public JXPathContext getNode()
  {
    return node;
  }

  public void setNode(JXPathContext node)
  {
    this.node = node;
  }

  public String getPropertyBagString()
  {
    return propertyBag.getPropertiesString();
  }

  protected boolean hasProperty(String property)
  {
    return XPathUtils.exists(getNode(), "properties/value[../name='" + property + "']");
  }

  protected String getPropertyString(String property)
  {
    return XPathUtils.getStringValue(getNode(), "properties/value[../name='" + property + "']");
  }

  protected Boolean getPropertyBoolean(String property)
  {
    return XPathUtils.getBooleanValue(getNode(), "properties/value[../name='" + property + "']");
  }

  public PropertyBag getPropertyBag()
  {
    return propertyBag;
  }

  public void aliasId(String alias)
  {
    String id = propertyBag.getId();
    if (id != null)
    {
      propertyBag.addId(Renderer.aliasName(alias, id));
    }
  }

  public abstract void processProperties();

  public abstract String renderStart();

  public abstract String renderClose();
}

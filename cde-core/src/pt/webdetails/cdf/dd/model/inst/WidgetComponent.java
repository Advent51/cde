/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdf.dd.model.inst;

import pt.webdetails.cdf.dd.model.core.validation.ValidationException;
import pt.webdetails.cdf.dd.model.meta.WidgetComponentType;
import pt.webdetails.cdf.dd.model.meta.MetaModel;

/**
 * @author dcleao
 */
public final class WidgetComponent extends GenericComponent<WidgetComponentType>
{
  private WidgetComponent(Builder builder, MetaModel metaModel) throws ValidationException
  {
    super(builder, metaModel);
  }

  @Override
  public WidgetComponentType getMeta()
  {
    return super.getMeta();
  }

  public String getWcdfPath()
  {
    return this.tryGetAttributeValue("wcdf", null);
  }
  
  /**
   * Class to create and modify WidgetComponent instances.
   */
  public static class Builder extends GenericComponent.Builder
  {
    @Override
    public WidgetComponent build(MetaModel metaModel) throws ValidationException
    {
      if(metaModel == null) { throw new IllegalArgumentException("metaModel"); }
      
      return new WidgetComponent(this, metaModel);
    }
  }
}

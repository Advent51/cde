/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdf.dd.model.inst.validation;

import org.apache.commons.lang.StringUtils;

/**
 * @author dcleao
 */
public final class DashboardDuplicateComponentError extends ComponentError
{
  private final String _componentName;

  public DashboardDuplicateComponentError(
          String componentName, 
          String dashboardName)
          throws IllegalArgumentException
  {
    super(dashboardName, "Dashboard");
    
    if(StringUtils.isEmpty(componentName)) { throw new IllegalArgumentException("componentName"); }

    this._componentName = componentName;
  }

  public String getComponentName()
  {
    return this._componentName;
  }

  @Override
  public String toString()
  {
    return "Dashboard of id '" + this._componentId + " has a duplicate component with name '" + this._componentName + "'.";
  }
}

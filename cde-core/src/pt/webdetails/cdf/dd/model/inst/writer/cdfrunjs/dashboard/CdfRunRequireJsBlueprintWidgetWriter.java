/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.dashboard;

import pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.dashboard.CdfRunRequireJsDashboardWriter;

/**
 * @author dcleao
 */
public final class CdfRunRequireJsBlueprintWidgetWriter extends CdfRunRequireJsDashboardWriter
{
  protected static final String TYPE = "BlueprintWidget";
  
  public String getType()
  {
    return TYPE;
  }
}
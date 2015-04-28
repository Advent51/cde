/*!
 * Copyright 2002 - 2015 Webdetails, a Pentaho company. All rights reserved.
 *
 * This software was developed by Webdetails and is provided under the terms
 * of the Mozilla Public License, Version 2.0, or any later version. You may not use
 * this file except in compliance with the license. If you need a copy of the license,
 * please go to http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. Please refer to
 * the license for the specific language governing your rights and limitations.
 */

define([
  'cdf/Dashboard.Clean',
  'cde/components/DashboardComponent',
  'cdf/lib/jquery'],
  function(Dashboard, DashboardComponent, $) {

  /**
   * ## The Dashboard Component
   */
  describe("The Dashboard Component #", function() {

    var dashboard = new Dashboard();

    dashboard.addParameter("param1", "");

    dashboard.init();

    // inject sampleObject div
    $("body").append($("<div>").attr("id", "sampleObject"));

    var dashboardComponent = new DashboardComponent({
      type: "DashboardComponent",
      name: "render_test",
      priority: 5,
      dashboardPath: "cde/test/dummyDashboard",
      parameterMapping: [["param1", "dummyParam"]],
      executeAtStart: true,
      htmlObject: "sampleObject",
      listeners: []
    });

    dashboard.addComponent(dashboardComponent);

    /**
     * ## The Dashboard Component # allows a dashboard to execute update
     */
    it("allows a dashboard to execute update", function(done) {

      spyOn(dashboardComponent, 'update').and.callThrough();

      dashboardComponent.once('cdf:postExecution', function() {
        expect(dashboardComponent.update).toHaveBeenCalled();
        done();
      });

      dashboard.update(dashboardComponent);
    });

    /**
     * ## The Dashboard Component # allows the correct use of parameter mapping
     */
    it("allows the correct use of parameter mapping", function(done) {
      var mapTest = "mappingTest";

      dashboardComponent.once('cdf:postExecution', function() {
        dashboardComponent.requiredDashboard.once("dummyParam:fireChange", function() {
          expect(dashboardComponent.requiredDashboard.getParameterValue("dummyParam")).toEqual(mapTest);
          done();  
        });
        dashboard.fireChange("param1", mapTest);
      });

      dashboard.update(dashboardComponent);

    });

  });
});

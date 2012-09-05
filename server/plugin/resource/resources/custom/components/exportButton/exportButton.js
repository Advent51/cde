


var ExportButtonComponent = BaseComponent.extend({

  ph: undefined,
  tc: undefined,
  queryObjectNames: { queryState:null, query:null },
  
/* BUILD THE COMPONENT */

  update: function(){
    
    var myself = this;
    $.extend(this.options,this);

    myself.ph = $("#" + myself.htmlObject);
    myself.ph.empty();
    var bar = $('<span class="exportButton"></span>').appendTo(myself.ph);
    var comp = Dashboards.getComponentByName( "render_" + myself.componentName );
    var overrideParameters = Dashboards.propertiesArrayToObject(myself.parameters);    
    bar.text( myself.label ).click( function(){
      var foundQuery = false;
      for (n in myself.queryObjectNames) {
        if(comp[n] && comp[n] instanceof Query){
            comp[n].exportData(myself.outputType, overrideParameters , myself.getFilterSettings(component) );
            foundQuery = true;
            break;
        }
      }
      if (!foundQuery) { 
        Dashboards.log( myself.name + ": could not find a query object on " + myself.componentName ); 
      }
    });
  },
  
  //add filtering to export if it's a data table
  getFilterSettings: function(component){
    var extraSettings = {};
	if(component.type == "Table"){
	  //var dtOptions = component.getTableOptions(component.chartDefinition);
	  //if(dtOptions.bFilter) {
	  var dtOptions = component.ph.dataTableSettings[0];
	  if(dtOptions.oFeatures.bFilter) {
		var searchInput = component.ph.find('input');
		if(searchInput) {
		  extraSettings.dtFilter = searchInput.val();
		  if(dtOptions.aoColumns){
			var idxs = [];
		    for(var i=0; i< dtOptions.aoColumns.length;i++){
			  if(dtOptions.aoColumns[i].bSearchable){
			    idxs.push(i);
		      }
			}
			extraSettings.dtSearchableColumns = idxs.join(',');
		  }
		}
	  }
	}
	return extraSettings;
  }

});

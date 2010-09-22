
var TableManager = Base.extend({

		id: "",
		tableId: "",
		logger: {},
		title: "Title",
		tableModel: {},
		initialOperations:[],
		isSelectedCell: false,
		hasAdvancedProperties: false,
		selectedCell: [],
		operations: [],
		linkedTableManager: undefined,
		linkedTableManagerOperation: undefined,

		constructor: function(id){
			this.logger = new Logger("TableManager - " + id);
			this.id = id;
			this.tableId = "table-" + id;

			// set a Default Table Model
			this.setTableModel(new TableModel());

			// Register this tablemanager in the global area
			TableManager.register(this);
		},

		init : function(){
			
			this.reset();
			$("#"+this.id).append(this.newTable());
			this.render();

		},

		reset: function(){
			$("#"+this.id).empty();
		},


		render: function(){

			this.logger.debug("Rendering table " + this.getTableId());

			// Create headers;
			var headerRows = $("<tr></tr>");
			var myself = this;
			$.each(myself.getTableModel().getColumnNames(),function(i,val){
					var _header = $("<th class=\"ui-state-default\">"+ val +"</th>");
					if ( typeof myself.getTableModel().getColumnSizes() != 'undefined'){
						_header.attr('width',myself.getTableModel().getColumnSizes()[i]);
					}
					_header.appendTo(headerRows);
				})
			headerRows.appendTo("#"+this.getTableId() + " > thead");

			// Create rows
			var myself=this;
			var data = this.getTableModel().getData() || [];
			$.each(data,function(i,row){
					myself.addRow(row);
				});

			$("#"+this.getTableId()).treeTable();
			this.updateOperations();

		
		},

		newTable: function(args){
			var isLayoutTable = this.tableId == 'table-cdfdd-layout-tree';//TODO:k
		//	var operationsDiv = ;
//TODO: layout also has delete etc...
//FIXME:
			isLayoutTable=false;
			var table = ''+
		//	(isLayoutTable ? ('<div id="'+ this.tableId +'Operations" style="height: 32px" class="cdfdd-operations"></div>') : '') +
			'<table id="'+ this.tableId +'" class="myTreeTable cdfdd ui-reset ui-clearfix ui-component ui-hover-state">\
			<caption class="ui-state-default"><div class="simpleProperties propertiesSelected">'+this.title+'</div>' +
			(!isLayoutTable ? ('<div id="'+ this.tableId +'Operations" style="float: right" class="cdfdd-operations"></div>') : '') +
			(this.hasAdvancedProperties == true ? '<span style="float:left">&nbsp;&nbsp;/&nbsp;&nbsp;</span><div class="advancedProperties propertiesUnSelected">Advanced Properties</div>' : '') +
			'</caption>\
			<thead>\
			</thead>\
			<tbody class="ui-widget-content">\
			</tbody>\
			</table>\
			';

			return table;
		},


		addRow: function(row,pos){
			// Adds row. -1 to add to the end of the list

			// Merge default options here
			// this.logger.debug("Adding row type "+ row.type  +" to table " + this.getTableId());
			var _model = BaseModel.getModel(row.type);
			if(typeof _model != 'undefined')
				this.extendProperties(row,_model.getStub());

			var rowObj = $('<tr></tr>');

			// Get id
			var _id;
			try{
				_id =  this.getTableModel().getRowId()(row);
				rowObj.attr("id",_id);
			}
			catch(e){
				this.logger.error("Error evaluating id expression " + this.getTableModel().getRowId() + ": " + e);
			}


			var _parent;
			var _parentExpression = this.getTableModel().getParentId();
			// parentId?
			try{
				if (typeof _parentExpression != 'undefined'){
					_parent = _parentExpression(row);
					if (typeof _parent != 'undefined' && _parent != IndexManager.ROOTID){
						//this.logger.debug("Row parent: " + _parent );
						rowObj.addClass("child-of-" + _parent);
					}
				}
			}
			catch(e){
				this.logger.error("Error evaluating parent expression " + _parentExpression + ": " + e);
			}


			// Add columns

			for (var i in this.getTableModel().getColumnGetExpressions()){
				ColumnRenderer.render(this.getTableModel(),row , i, rowObj, this.getTableModel().getData());
			}


			var selector = "table.#" + this.getTableId() + " tbody";
			if(pos < 0){
				rowObj.appendTo($(selector));
				$(selector).append(html);
			}
			else{
				var _selector = $(selector + " > tr:eq(" + pos + ")");
				_selector.length == 1?_selector.before(rowObj):rowObj.appendTo($(selector));
			}

			return _id;

		},

		updateTreeTable: function(rowId){

			if ( rowId != IndexManager.ROOTID){

				var _parentQ = $('#'+this.getTableId() + " > tbody > tr#"+ rowId);
				_parentQ.removeClass("initialized");
				_parentQ.removeClass("parent");
				$("> td > span.expander",_parentQ).remove();
				_parentQ.initializeTreeTableNode();
				_parentQ.expand();
			}

		},

		insertAtIdx: function(_stub,insertAtIdx){
		
			// Insert it on the dataModel
			this.getTableModel().getData().splice(insertAtIdx,0,_stub);
			this.getTableModel().getIndexManager().updateIndex();
			var newId = this.addRow(_stub,insertAtIdx);

			// Update treeTable:
			this.updateTreeTable(_stub.parent);

			// focus the newly created line
			this.selectCell(insertAtIdx,1,'simple');

		},

		createOrGetParent: function(category,categoryDesc){
			// Does this exist? If yes, return the last position
			var indexManager = this.getTableModel().getIndexManager();
			var cat = indexManager.getIndex()[category];
			if(typeof cat == 'undefined'){
				// Create it and return the last idx
				var _stub = {
					id: category,
					name: categoryDesc,
					type: "Label",
					typeDesc: "<i>Group</i>",
					parent: IndexManager.ROOTID,
					properties: [{
						name: "Group",
						description: "Group",
						value: categoryDesc,
						type: "Label"
					}]
				};
				insertAtIdx = this.getTableModel().getData().length;
				this.insertAtIdx(_stub,insertAtIdx);
				return insertAtIdx + 1;

			}
			else{
				// Append at the end
				return cat.index + cat.children.length + 1;

			}

		
		},

		updateOperations: function(){

			// Add all initial operation plus row/cell specific operations
			this.setOperations(this.getInitialOperations());

			if(this.isSelectedCell)
				var _ops = CellOperations.getOperationsByType(
					this.getTableModel().getEvaluatedRowType(this.selectedCell[0])
				);
			this.setOperations(this.getOperations().concat(_ops));

			this.logger.debug("Found " + this.getOperations().length + " operations for this cell");
			var _opsNode = $("#"+this.getTableId()+"Operations");
			_opsNode.empty();

			var myself = this;
			$.each(this.getOperations(),function(i,_operation){
					if (typeof _operation != 'undefined')
						_opsNode.append(_operation.getHtml(myself, i));
				});

		},


		cellClick: function(row,col,classType){
			// Update operations

			if(typeof this.getLinkedTableManager() != 'undefined')
				this.getLinkedTableManager().cellUnselected();

			this.isSelectedCell =  true;
			this.selectedCell = [row,col];
			this.updateOperations();
			this.fireDependencies(row,col,classType);

		},

		cellUnselected: function(){
			this.isSelectedCell = false;
			this.cleanSelections();
			this.updateOperations();
			this.cleanDependencies();
			if(typeof this.getLinkedTableManager() != 'undefined')
				this.getLinkedTableManager().cellUnselected();
		},

		selectCell: function(row,col,classType){

			// Unselect

			this.cleanSelections();
			$('#'+this.getTableId() + " > tbody > tr:eq("+ row +")").addClass("ui-state-active");

			// Uncomment following cells to enable td highlight
			//$('#'+this.getTableId() + " > tbody > tr:eq("+ row +") > td:eq("+ col + ")").addClass("ui-state-active");

			// Fire cellClicked; get id
			this.cellClick(row,col,classType);

		},

		cleanSelections: function(){


			$('#'+this.getTableId()).find("tr.ui-state-active").removeClass("ui-state-active"); // Deselect currently ui-state-active rows

			// Uncomment following cells to enable td highlight
			//$('#'+this.getTableId()).find("tr td.ui-state-active").removeClass("ui-state-active"); // Deselect currently ui-state-active rows

		},

		fireDependencies: function(row,col,classType){
			if( typeof this.getLinkedTableManager() != 'undefined' ){

				var data = this.getLinkedTableManagerOperation()(this.getTableModel().getData()[row],classType);

				var tableManager = this.getLinkedTableManager();

				tableManager.getTableModel().setData(data);
				tableManager.cleanSelections();
				tableManager.init();
				//tableManager.selectCell(targetIdx,colIdx);

			}
		},

		cleanDependencies: function(){
			if( typeof this.getLinkedTableManager() != 'undefined' ){
				var tableManager = this.getLinkedTableManager();

				tableManager.getTableModel().setData([]);
				tableManager.cleanSelections();
				tableManager.init();
			}
		},

		extendProperties: function(row,stub){
			 // 1 - get names on original
			 // 2 - get names on stub
			 // 3 - add to the original the ones not on the second
			 var pRow = {};

			 $.each(row.properties,function(i,p){
				 pRow[p.name]=p;
			 });
			 $.each(stub.properties,function(i,s){
				 if(typeof pRow[s.name] == 'undefined')
					 row.properties.push(s);
			 });
		
		},

		// Accessors
		setId: function(id){this.id = id},
		getId: function(){return this.id},
		setTitle: function(title){this.title = title},
		getTitle: function(){return this.title},
		setTableId: function(tableId){this.tableId = tableId},
		getTableId: function(){return this.tableId},
		setTableModel: function(tableModel){this.tableModel = tableModel},
		getTableModel: function(){return this.tableModel},
		setInitialOperations: function(initialOperations){this.initialOperations = initialOperations},
		getInitialOperations: function(){return this.initialOperations},
		setOperations: function(operations){this.operations = operations},
		getOperations: function(){return this.operations},
		setSelectedCell: function(selectedCell){this.selectedCell = selectedCell},
		getSelectedCell: function(){return this.selectedCell},
		setLinkedTableManager: function(linkedTableManager){this.linkedTableManager = linkedTableManager},
		getLinkedTableManager: function(){return this.linkedTableManager},
		setLinkedTableManagerOperation: function(linkedTableManagerOperation){this.linkedTableManagerOperation = linkedTableManagerOperation},
		getLinkedTableManagerOperation: function(){return this.linkedTableManagerOperation}

	},{
		tableManagers: {},

		register: function(tableManager){
			TableManager.tableManagers[tableManager.getTableId()] = tableManager;
		},

		getTableManager: function(id){
			return TableManager.tableManagers[id];
		},

		executeOperation: function(tableManagerId,idx){

			var tableManager = TableManager.getTableManager(tableManagerId);
			tableManager.getOperations()[idx].execute(tableManager);
		},

		globalInit: function(){

			// Enable the table selectors
			$("table.myTreeTable tbody tr td").live("mousedown",function() {
					var myself = $(this);

					// get Current Id:
					var row = myself.parent().prevAll().length;
					var col = myself.prevAll().length;

					var wasSelected = myself.hasClass("selected")
					var _tableManager = TableManager.getTableManager(myself.closest("table").attr("id"));

					if (!wasSelected){
						_tableManager.selectCell(row,col,'simple');
					}
					else{
						_tableManager.cellUnselected();
					}
							
				});
				
			$(".advancedProperties").live('click',function() {
				var myself = $("#table-" + ComponentsPanel.COMPONENTS + " .ui-state-active td");
				if(myself.length > 0){
				var row = myself.parent().prevAll().length;
				var col = myself.prevAll().length;
				var _tableManager = TableManager.getTableManager(myself.closest("table").attr("id"));
				_tableManager.selectCell(row,col,'advanced');
				$(".advancedProperties").attr("class","advancedProperties propertiesSelected");
				$(".advancedProperties").parent().find(".simpleProperties").attr("class","simpleProperties propertiesUnSelected");
				}
			});
		
			$(".simpleProperties").live('click',function() {
				var myself = $("#table-" + ComponentsPanel.COMPONENTS + " .ui-state-active td")
				if(myself.length > 0){
				var row = myself.parent().prevAll().length;
				var col = myself.prevAll().length;
				var _tableManager = TableManager.getTableManager(myself.closest("table").attr("id"));
				_tableManager.selectCell(row,col,'simple');
				$(".advancedProperties").attr("class","advancedProperties propertiesUnSelected");
				$(".advancedProperties").parent().find(".simpleProperties").attr("class","simpleProperties propertiesSelected");
				}
			});
			

		},
		S4: function() {
			return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
		},
		generateGUID: function() {
			return (TableManager.S4()+TableManager.S4()+"-"+TableManager.S4()+
					"-"+TableManager.S4()+"-"+TableManager.S4()+
					"-"+TableManager.S4()+TableManager.S4()+TableManager.S4());
			}


		});


	var TableModel = Base.extend({

			id: "row.id",
			logger: {},
			data: [],
			indexManager: {},
			columnId: undefined,
			columnNames: [],
			columnGetExpressions: [],
			columnTypes: [],
			columnSizes: undefined ,
			editable: undefined,
			columnSetExpressions: [],
			rowId: "row.id",
			parentId: undefined,
			rowType: "row.type",

			constructor: function(id){
				this.logger = new Logger("TableModel" + id);
				this.id = id;
				this.setIndexManager(new IndexManager(this));

				this.init();
			},

			getEvaluatedId: function(rowNumber){

				try{
					var row = this.data[rowNumber];
					return this.getRowId()(row);
				}
				catch(e){
					this.logger.error("Error getting id " + e);
				}

			},


			getEvaluatedRowType: function(rowNumber){

				try{
					var row = this.data[rowNumber];
					return this.getRowType()(row);
				}
				catch(e){
					this.logger.error("Error getting row type: " + e);
				}

			},


			init: function(){
				// Do nothing
			},

			setId: function(id){this.id = id},
			getId: function(){return this.id},
			setData: function(data){this.data = data; this.getIndexManager().updateIndex()},
			getData: function(){return this.data},
			setIndexManager: function(indexManager){this.indexManager = indexManager},
			getIndexManager: function(){return this.indexManager},
			setColumnNames: function(columnNames){this.columnNames = columnNames},
			getColumnNames: function(){return this.columnNames},
			setColumnGetExpressions: function(columnGetExpressions){this.columnGetExpressions = columnGetExpressions},
			getColumnGetExpressions: function(){return this.columnGetExpressions},
			setColumnSetExpressions: function(columnSetExpressions){this.columnSetExpressions = columnSetExpressions},
			getColumnSetExpressions: function(){return this.columnSetExpressions},
			setColumnTypes: function(columnTypes){this.columnTypes = columnTypes},
			getColumnTypes: function(){return this.columnTypes},
			setColumnSizes: function(columnSizes){this.columnSizes = columnSizes},
			getColumnSizes: function(){return this.columnSizes},
			setEditable: function(editable){this.editable = editable},
			getEditable: function(){return this.editable},
			setRowId: function(rowId){this.rowId = rowId},
			getRowId: function(){return this.rowId},
			setRowType: function(rowType){this.rowType = rowType},
			getRowType: function(){return this.rowType},
			setParentId: function(parentId){this.parentId = parentId},
			getParentId: function(){return this.parentId}

		});


	// Properties Table Model

	var PropertiesTableModel = TableModel.extend({

			constructor: function(id){
				this.logger = new Logger("TableModel" + id);
				this.id = id;
				this.setIndexManager(new IndexManager(this));

				this.setColumnNames(['Property','Value']);
				this.setColumnGetExpressions([function(row){return row.description},function(row){return row.value}]);
				this.setColumnSetExpressions([undefined,function(row,value){row.value = value}]);
				this.setColumnTypes(['String', function(row){return row.type}]);
				this.setColumnSizes(['40%','60%']);
				this.setEditable([false, true]);
				this.setRowId(function(row){return TableManager.generateGUID()});
				this.setRowType(function(row){return row.type});

				this.init();
			}


		});


	// Single Instance

	var ColumnRenderer = Base.extend({

		},{

			constructor: null,
			pool: {a:1},
			render: function(tableModel,row, col, placeholder, rows){

				var renderer;


				var _type;
				if(typeof tableModel.getColumnTypes()[col] == 'function'){
					_type = tableModel.getColumnTypes()[col](row);
				}
				else{
					_type = tableModel.getColumnTypes()[col];
				}

				var _getExpression = tableModel.getColumnGetExpressions()[col];
				var _editable = typeof tableModel.getEditable() == 'undefined'? false:tableModel.getEditable()[col];
				var _setExpression = _editable?tableModel.getColumnSetExpressions()[col]:undefined;
				var _getRow = function(id){var row; $.each(rows,function(i,r){if(r.name == id){row = r; return;}}); return row;};

				if(typeof this.pool[_type] == 'undefined'){

					try {
						//alert( "Creating new " + _type +"Renderer");
						eval('renderer = new ' + _type+"Renderer()");
						this.pool[_type] = renderer;
					} catch (e) {
						// Revert to default renderer
						renderer = new CellRenderer();
					}

				}
				else{
					renderer = this.pool[_type];
				}

				return renderer.render(row,placeholder, _getExpression,_setExpression,_editable, _getRow);

			}


		});


	var CellRenderer = Base.extend({


			logger: {},
			row: undefined,
			setExpression: undefined,
			getExpression: undefined,
			placeholder: undefined,

			constructor: function(){
				this.logger = new Logger("CellRenderer");
				this.logger.debug("Creating new CellRenderer");
			},

			// Defaults to a common string type
			render: function(row,placeholder, getExpression,setExpression,editable){
				placeholder.setText(getExpression(row));
			}

		});


	var LabelRenderer = CellRenderer.extend({

			constructor: function(){
				this.base();
				this.logger = new Logger("LabelRenderer");
				this.logger.debug("Creating new LabelRenderer");
			},

			render: function(row,placeholder, getExpression,setExpression,editable){
				$("<td>"+ getExpression(row) +"</td>").appendTo(placeholder);
			},

			validate: function(settings, original){
				return true;
			}

		});


	var StringRenderer = CellRenderer.extend({

			constructor: function(){
				this.base();
				this.logger = new Logger("StringRenderer");
				this.logger.debug("Creating new StringRenderer");
			},

			render: function(row,placeholder, getExpression,setExpression,editable){
				if(editable){
					this.setExpression = setExpression;
					this.row = row;

					var _editArea = $("<td>"+ getExpression(row) +"</td>");
					var myself = this;
					_editArea.editable(function(value,settings){
							myself.logger.debug("Saving new value: " + value );
							myself.setExpression(row,value);


							return value;
						} , {
							cssclass: "cdfddInput",
							select: true,
							onsubmit: function(settings,original){
								return myself.validate($('input',this).val());
							}
						});
					_editArea.appendTo(placeholder);

				}
				else{
					$("<td>"+ getExpression(row) +"</td>").appendTo(placeholder);
				}
			},

			validate: function(settings, original){
				return true;
			}

		});

	var IdRenderer = StringRenderer.extend({

			constructor: function(){
				this.base();
				this.logger = new Logger("IdRenderer");
				this.logger.debug("Creating new IdRenderer");
			},

			validate: function(value){

				if(!value.match(/^[a-zA-Z0-9_.]*$/)){
					$.prompt('Argument '+ value + ' invalid. Can only contain alphanumeric characters and the special _ and . characters');
					return false;
				}
				return true;
			}

		});


	var IntegerRenderer = StringRenderer.extend({

			constructor: function(){
				this.base();
				this.logger = new Logger("IntegerRenderer");
				this.logger.debug("Creating new IntegerRenderer");
			},

			validate: function(value){

				if(!value.match(/^\d*$/)){
					$.prompt('Argument '+ value + ' must be numeric');
					return false;
				}
				return true;
			}

		});
		
	var FloatRenderer = StringRenderer.extend({

			constructor: function(){
				this.base();
				this.logger = new Logger("FloatRenderer");
				this.logger.debug("Creating new FloatRenderer");
			},

			validate: function(value){

				if(!value.match(/^\d*\.?\d*$/)){
					$.prompt('Argument '+ value + ' must be numeric');
					return false;
				}
				return true;
			}
		});

	var SelectRenderer = CellRenderer.extend({

			constructor: function(){
				this.base();
				this.logger = new Logger("SelectRenderer");
				this.logger.debug("Creating new SelectRenderer");
			},

			render: function(row,placeholder, getExpression,setExpression,editable){
				if(editable){
					this.setExpression = setExpression;
					this.getExpression = getExpression;

					this.row = row;

					var _editArea = $("<td>"+this.getFormattedExpression(row, getExpression)+ "</td>");
					var myself = this;
					_editArea.editable(function(value,settings){
							myself.logger.debug("Saving new value: " + value );
							myself.setExpression(row,value);


							return myself.getFormattedExpression(row,getExpression);
						} , {
							cssclass: "cdfddInput",
							data   : this.getData(row),
							type   : 'select',
							submit: 'OK',
							height: 12,
							onsubmit: function(settings,original){
								return myself.validate($('input',this).val());
							}
						});
					_editArea.appendTo(placeholder);

				}
				else{
					$(this.getFormattedExpression(row,getExpression)).appendTo(placeholder);
				}
			},

			validate: function(settings, original){
				return true;
			},

			getData: function(row){
				return '{"A": "Alpha","B":"Beta"}';
			},

			getFormattedExpression: function(row, getExpression){
				return getExpression(row);
			}


		});


	var BooleanRenderer = SelectRenderer.extend({

			getData: function(row){
				return " {'true':'True','false':'False', 'selected':'" + (this.getExpression(row)=="true"?"true":"false") + "'}";
			},

			getFormattedExpression: function(row, getExpression){
				return (getExpression(row)=="true"?"True":"False");
			}
		});

	var SelectMultiRenderer = CellRenderer.extend({

			constructor: function(){
				this.base();
				this.logger = new Logger("SelectMultiRenderer");
				this.logger.debug("Creating new SelectMultiRenderer");
			},

			render: function(row,placeholder, getExpression,setExpression,editable){
				if(editable){
					this.setExpression = setExpression;
					this.getExpression = getExpression;

					this.row = row;

					var _editArea = $("<td>"+this.getFormattedExpression(row, getExpression)+ "</td>");
					var myself = this;
					_editArea.editable(function(value,settings){

							var selector = $(this);
							var value = "['"+selector.find("input").val().replace(/, /g,"','") + "']";
							if (value=="['Select options']"){
								value = "[]";
							}
							myself.logger.debug("Saving new value: " + value );
							myself.setExpression(row,value);


							return myself.getFormattedExpression(row,getExpression);
						} , {
							cssclass: "cdfddInput",
							data   : this.getData(row),
							type   : 'selectMulti',
							submit: 'OK',
							height: 12,
							onsubmit: function(settings,original){
								return myself.validate($('input',this).val());
							}
						});
					_editArea.appendTo(placeholder);

				}
				else{
					$(this.getFormattedExpression(row,getExpression)).appendTo(placeholder);
				}
			},

			validate: function(settings, original){
				return true;
			},

			getData: function(row){
				return '{"A": "Alpha","B":"Beta"}';
			},

			getFormattedExpression: function(row, getExpression){
				return getExpression(row);
			}


		});


	var RoundCornersRenderer = SelectRenderer.extend({

			translationHash: {
				'':'Simple',
				'cdfdd-round':'Round',
				'cdfdd-bevel':'Bevel', 
				'cdfdd-notch':'Notch',
				'cdfdd-bite':'Bite',
				'cdfdd-bevel_top':'Top Bevel',
				'cdfdd-dog_tr':'Dog TR'
			},

			getData: function(row){
				return " {'':'Simple','cdfdd-round':'Round','cdfdd-bevel':'Bevel', 'cdfdd-notch':'Notch','cdfdd-bite':'Bite','cdfdd-bevel top':'cdfdd-Top Bevel','cdfdd-dog tr':'Dog TR','cdfdd-selected':'" + (this.getExpression(row)) + "'}";
			},

			getFormattedExpression: function(row, getExpression){
				return this.translationHash[getExpression(row)];
			}
		});


	var TextAlignRenderer = SelectRenderer.extend({

			translationHash: {
				'':'',
				'left':'Left',
				'center':'Center',
				'right':'Right'
			},

			getData: function(row){
				return " {'':'','left':'Left','center':'Center','right':'Right','selected':'" + (this.getExpression(row)) + "'}";
			},

			getFormattedExpression: function(row, getExpression){
				return this.translationHash[getExpression(row)];
			}
		});


	

	var ColorRenderer = CellRenderer.extend({

			constructor: function(){
				this.base();
				this.logger = new Logger("ColorRenderer");
				this.logger.debug("Creating new ColorRenderer");
				this.id = 0;
			},
			
			getId: function(){
				return this.id++;
			},

			render: function(row,placeholder, getExpression,setExpression,editable,getRow){
				if(editable){
					this.setExpression = setExpression;
					this.row = row;
					this.getRow = getRow;
					this.placeholder = placeholder;
					
					var id = this.getId();
					var inputId = "#colorpicker_input_" + id;
					var checkId = "#colorpicker_check_" + id;
					var _editArea = $('<td><form onsubmit="return false" class="cdfddInput"><input id="colorpicker_check_' + id + '" class="colorcheck" type="checkbox"></input><input id="colorpicker_input_' + id+ '" class="colorinput" readonly="readonly" type="text" size="7"></input></form></td>');
					var myself=this;
					$(checkId ,_editArea).bind("click",function(){

							if($(this).is(":checked")){
								$(inputId,_editArea).attr("disabled",true);
								$(inputId,_editArea).attr("readonly","readonly");
								$(inputId).trigger("click");
							}
							else{
								$(inputId,_editArea).val("");
								$(inputId,_editArea).attr("disabled",true);
								myself.setExpression(row,"");
							}
						});
					this.updateValueState(getExpression(row),_editArea, inputId, checkId);
					$(inputId,_editArea).ColorPicker({
							onSubmit: function(hsb, hex, rgb, el) {
								$(el).val("#"+hex);
								$(el).ColorPickerHide();
								myself.setExpression(row,"#"+hex);
							},
							onBeforeShow: function () {
								$(this).ColorPickerSetColor(this.value.substring(1));
							}
						});
					// $("input",_editArea).ColorPicker();
					_editArea.appendTo(placeholder);

				}
				else{
					$("<td>"+ getExpression(row) +"</td>").appendTo(placeholder);
				}
			},

			updateValueState: function(value,placeholder,inputId,checkId){
				// set checkbox and textarea state
				if (value == ''){
					$(checkId,placeholder).removeAttr("checked");
					$(checkId,placeholder).css("background-color","#ffffff");
					$(inputId,placeholder).attr("disabled",true);
				}
				else{
					$(checkId,placeholder).attr("checked","true");
					$(inputId,placeholder).removeAttr("disabled");
					$(inputId,placeholder).attr("readonly","readonly");
					$(inputId,placeholder).val(value);
				}

			},

			getFormattedExpression: function(row, getExpression){
				return getExpression(row);
			}, 

			validate: function(settings, original){
				return true;
			}

		});

	var TextAreaRenderer = CellRenderer.extend({

			constructor: function(){
				this.base();
				this.logger = new Logger("TextAreaRenderer");
				this.logger.debug("Creating new TextAreaRenderer");
			},

			render: function(row,placeholder, getExpression,setExpression,editable){


				if(editable){
					this.row = row;
					this.getExpression = getExpression;
					this.setExpression = setExpression;
					this.placeholder = placeholder;

					var _editArea = $('<td><div style="float:left"><code></code></div><div class="edit" style="float:right"></div></td>');
					_editArea.find("code").text(this.getFormattedExpression(row, getExpression));
					var myself=this;
					var _prompt = $('<button class="cdfddInput">...</button>').bind("click",function(){
							var _inner = 'Edit<br /><textarea wrap="off" cols="80" class="cdfddEdit" name="textarea">' + myself.getExpression(row) + '</textarea>';
							// Store what we need in a global var
							cdfdd.textarea = [myself,row,getExpression,setExpression,placeholder];
							$.prompt(_inner,{
									buttons: { Ok: true, Cancel: false },
									callback: myself.callback,
									opacity: 0.2,
									prefix:'brownJqi'
								});
						}).appendTo($("div.edit",_editArea));

					_editArea.appendTo(placeholder);

				}
				else{
					$("<td><code/></td>").children("code").text(this.getFormattedExpression(row, getExpression)).appendTo(placeholder);
				}
			},

			callback: function(v,m,f){
				if (v){
					// set value. We need to add a space to prevent a string like function(){}
					// to be interpreted by json as a function instead of a string
					var value = f.textarea;
					if(value.length != 0 && value.substr(value.length-1,1)!=" "){
						value = value+" ";
					}
					cdfdd.textarea[3](cdfdd.textarea[1],value);
					$("code",cdfdd.textarea[4]).text(cdfdd.textarea[0].getFormattedExpression(cdfdd.textarea[1],cdfdd.textarea[2]));
				}
				delete cdfdd.textarea;
			},

			validate: function(settings, original){
				return true;
			},

			getFormattedExpression: function(row, getExpression){
				var _value = getExpression(row);
				if(_value.length > 30){
					_value = _value.substring(0,20) + " (...)";
				}
				return _value;
			}

		});

	var HtmlRenderer = TextAreaRenderer.extend({});

	var ResourceRenderer = TextAreaRenderer.extend({});
	
	var DateRenderer = CellRenderer.extend({

			constructor: function(){
				this.base();
				this.logger = new Logger("DateRenderer");
				this.logger.debug("Creating new DateRenderer");
			},
			
			render: function(row,placeholder, getExpression,setExpression,editable){
				if(editable){
					this.setExpression = setExpression;
					this.getExpression = getExpression;
					this.row = row;
					

					var _editArea = $("<td>"+this.getFormattedExpression(row, getExpression)+ "</td>");
					var myself = this;
					_editArea.editable(function(value,settings){
							myself.logger.debug("Saving new value: " + value );
							myself.setExpression(row,value);
							return myself.getFormattedExpression(row,getExpression);
						} , {
							cssclass: "cdfddInput",
							data   : this.getData(row),
							type   : 'select',
							submit: 'OK',
							height: 12,
							onsubmit: function(settings,original){
								var selectedValue = $(this.children()[0]).val();
								if(selectedValue == 'pickDate'){
									myself.pickDate($(this.children()[0]));
									return false;
								}
								return myself.validate();
							}
						});
					_editArea.appendTo(placeholder);

				}
				else{
					$(this.getFormattedExpression(row,getExpression)).appendTo(placeholder);
				}
			},
			
			pickDate: function(input){
				this.datePicker = $("<input/>").css("width","80px");
				$(input).replaceWith(this.datePicker);
				this.datePicker.datepicker({dateFormat: 'yy-mm-dd',
					changeMonth: true,
					changeYear: true,
					onSelect:function(date, input) {}
				});
				this.datePicker.datepicker('show');
			},
			
			validate: function(settings, original){
				return true;
			},
			
			getData: function(row){
				var data = Panel.getPanel(ComponentsPanel.MAIN_PANEL).getParameters(); 
				var _str = "{'today':'Today','yesterday':'Yesterday','pickDate':'Pick Date', 'selected':'" + (this.getExpression(row)) + "'}";
				
				return _str;
			},
			
			getFormattedExpression: function(row, getExpression){
				var selectedValue = getExpression(row) ;
				if(selectedValue == 'pickDate')
					return this.toDateString(this.datePicker.datepicker('getDate'));
				
				var date = new Date();
				if(selectedValue == "yesterday" )
					date.setDate(date.getDate()-1);
				
				return  this.toDateString(date);
			},
			
			toDateString: function(d){
				var currentMonth = "0" + (d.getMonth() + 1);
				var currentDay = "0" + (d.getDate());
				return d.getFullYear() + "-" + (currentMonth.substring(currentMonth.length-2, currentMonth.length)) + "-" + (currentDay.substring(currentDay.length-2, currentDay.length));
			}


		});
		
	var DateRangeRenderer = DateRenderer.extend({
		
		pickDate: function(input){
				this.datePicker = $("<input/>").css("width","80px");
				$(input).replaceWith(this.datePicker);
				
				var offset = this.datePicker.offset();
				var myself = this;
				
				var a = this.datePicker.daterangepicker({
						posX: offset.left-400, 
						posY: offset.top-100, 
						dateFormat: 'yy-mm-dd',
						onDateSelect: function(rangeA, rangeB) {
							myself.rangeA = rangeA;
							myself.rangeB = rangeB;
						}
				}); 
				
				this.datePicker.click();
			},
			
		getData: function(row){
				var data = Panel.getPanel(ComponentsPanel.MAIN_PANEL).getParameters(); 
				var _str = "{'monthToDay':'Month to day','yearToDay':'Year to day','pickDate':'Pick Dates', 'selected':'" + (this.getExpression(row)) + "'}";
				
				return _str;
			},
			
		getFormattedExpression: function(row, getExpression){
				var selectedValue = getExpression(row) ;
				if(selectedValue == 'pickDate'){
					return  this.rangeA + " - " + this.rangeB;
				}
				
				var date = new Date()
				if(selectedValue == "monthToDay" )
					date.setDate(1);
				else if(selectedValue == "yearToDay" ){
					date.setMonth(0);
					date.setDate(1);
				}
				
				return  this.toDateString(date) + " " + this.toDateString(new Date());
			}
	
	});
	
	var ResourceFileRenderer = CellRenderer.extend({

			constructor: function(){
				this.base();
				this.logger = new Logger("ResourceFileRenderer");
				this.logger.debug("Creating new ResourceFileRenderer");
			},

			render: function(row,placeholder, getExpression,setExpression,editable, getRow){
				if(editable){
					this.setExpression = setExpression;
					this.row = row;
					var content = $('<td></td>');
					var _editArea = $('<div class="cdfdd-resourceFileNameRender" >'+ getExpression(row) +'</div>');
					var _fileExplorer = $('<button class="cdfdd-resourceFileExplorerRender">...</button>');
					content.append(_editArea);
					content.append(_fileExplorer);
					
					var myself = this;
					_editArea.editable(function(value,settings){
							myself.logger.debug("Saving new value: " + value );
							myself.setExpression(row,value);
							return value;
						} , {
							cssclass: "cdfddInput",
							select: true,
							onsubmit: function(settings,original){
								return myself.validate($('input',this).val());
							}
						});
					
					var fileExtensions = getRow("resourceType").value == "Css" ? ".css" : ".js";
					_fileExplorer.bind('click',function(){
						
						var fileExplorercontent = 'Choose Resouce:<div id="container_id" class="urltargetfolderexplorer"></div>';
						var selectedFile = "";

						$.prompt(fileExplorercontent,{
							loaded: function(){
								selectedFile = "";
								$('#container_id').fileTree(
								{root: '/',script: CDFDDDataUrl.replace("Syncronize","ExploreFolder?fileExtensions="+fileExtensions),expandSpeed: 1000, collapseSpeed: 1000, multiFolder: false,folderClick: 
								function(obj,folder){if($(".selectedFolder").length > 0)$(".selectedFolder").attr("class","");$(obj).attr("class","selectedFolder");}}, 
								function(file) {selectedFile = "../pentaho-cdf/GetCDFResource?resource=" + file;$(".selectedFile").attr("class","");$("a[rel='" + file + "']").attr("class","selectedFile");});
							},
							buttons: { Ok: true, Cancel: false },
							opacity: 0.2,
							callback: function(v,m,f){
								if(v && selectedFile.length > 0){
									_editArea.text(selectedFile);
									myself.setExpression(row,selectedFile);
								}
							}});});
					
					content.appendTo(placeholder);
				}
				else{
					$("<td>"+ getExpression(row) +"</td>").appendTo(placeholder);
				}
			},

			validate: function(settings, original){
				return true;
			}

		});



var editor;
$(document).ready(function() {
	editor = new AgentEditor();
	
	$("#btn-delete-agent").on("click", function() {
		editor.deleteEntity("agents/");
	});
	
	$("#agentTypeId").trigger("change");
	
	$('[data-toggle="tooltip"]').tooltip();
	$.slidebars();
});


var AgentEditor = function() {
	var _this = this;
	this.prepareTranslations([]);
	this.addVocabularySource("parentAgents", "agents/query/", "excl=" + this.entityId);

	this.registerParentAgentTypeahead($('#parentAgentIdSelector'));
	
	this.lists["identifierList"] = new CollectionEditorList({
		listSelector: "#lst-agent-provided-identifiers",
		newRowUrl: __util.composeUrl("agents/includes/editIdentifier"),
		newRowCallback: function(row) {
			_this.registerFormControlSelectionEvents($(row));
		}
	});
	
	this.tables["addresses"] = new CollectionEditorTable({
		tableSelector: "#tbl-agent-addresses",
		newRowUrl: __util.composeUrl("agents/includes/editAddress"),
		newRowCallback: function(row) {
			_this.registerFormControlSelectionEvents($(row));
			$("#chk-toggle-hints").trigger("change");
		}
	});
	
	this.lists["images"] = new CollectionEditorList({
		listSelector: "#lst-agent-images",
		newRowUrl: __util.composeUrl("agents/includes/editImage"),
		newRowCallback: function(row) {
			_this.registerFormControlSelectionEvents($(row));
		}
	});
	
	this.initRightsContainer();
	this.registerNavFormControlEvents();
	this.registerFormControlSelectionEvents($("form"));
};

AgentEditor.prototype = new BaseEditor();

AgentEditor.prototype.deleteAgent = function() {
	var _this = this;
	$.ajax({
        url: __util.composeUrl("agents/" + _this.entityId + "/delete"),
        type: "POST",
        success: function(data) {
        	window.location.reload();
        },
        error: function(textStatus) { 
        	alert("Could not delete agent: " + textStatus);
        }
	});
};

AgentEditor.prototype.registerParentAgentTypeahead = function(element) {
	var _this = this;
	this.registerTypeahead(element, "parentAgents", "none", 8, 
			function(data) { return "<p>" + _this.renderAgentSuggestion(data) + "</p>"; },
			function(t, suggestion) {
				_this.handleParentAgentSelection(true, suggestion.entityId,
						"<a href='" + suggestion.entityId + "'>" +
						"<button type=\"button\" class=\"btn btn-xs btn-link pull-right\">" +
							"<span class=\"glyphicon glyphicon-link\" aria-hidden=\"true\"></span>" +
						"</button>" + _this.renderAgentSuggestion(suggestion) + "</a>");
			}
	);
	element.closest(".form-group").find(".collection-reset").on("click", function() { 
		_this.handleParentAgentSelection(false, "", "<span></span>"); 
	});
};

AgentEditor.prototype.handleParentAgentSelection = function(select, entityId, html) {
	$("#parentAgentId").val(entityId);
	$(".parentAgent-display p").html(html);
	
	if (select) {
		$(".parentAgent-display").removeClass("hide");
		$(".parentAgent-display-null").addClass("hide");
	} else {
		$(".parentAgent-display").addClass("hide");
		$(".parentAgent-display-null").removeClass("hide");
	}
};
	
AgentEditor.prototype.renderAgentSuggestion = function(agent) {
	return  "<strong>" + agent.name + " " + (agent.foreName!==undefined ? agent.foreName : "") + "</strong><br />" +
			"<small><em>ID:" + agent.entityId + "</em></small>" +
			(agent.address!=null && agent.address!="" ? "<br />" + agent.address : "");
};

AgentEditor.prototype.handleAgentTypeChange = function(select) {
	var isNatural = $("#agentTypeId option[value='" + select.value + "']").attr("data-natural")=="true"; 
	
	if (isNatural) {
		$(".agent-nonnatural-only").css("display", "none");
		$(".agent-natural-only").css("display", "");
	} else {
		$(".agent-nonnatural-only").css("display", "");
		$(".agent-natural-only").css("display", "none");
	}
};
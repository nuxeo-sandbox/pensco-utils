var API = (function(API) {

	var nuxeoClient;

	API.config = function () {
	  //Instantiate Nuxeo Client
	  nuxeoClient = new nuxeo.Client({
	  	timeout: 3000}
	  );
	  // Client schema and timeout configuration
	  nuxeoClient.schema("*");
	};

	API.search = function(data) {
	  nuxeoClient.repositoryName('default').
		operation('API-GenerateMergedCustomerStatements').
		params(data).
		execute(callbackGeneratePDF);
	  $("#inputForm").attr("class", "ui loading form segment");
	};

	////////////////////////////// CALLBACK FUNCTIONS

	function callbackGeneratePDF(error, result) {

	  $("#inputForm").attr("class", "ui form segment");

	  if (error) {
		throwError("Cannot run generate pdf -> " + error);
		throw error;
	  }
	  console.log('Download details are ' + result);
	  var url = result.properties["file:content"].data;
	  download(url);
	};

	return API;

})({});


////////////////////////////// INIT
$(document).ready(function () {
	$('#fromDate').datetimepicker({
	  timepicker:false,
	  format:'d/m/Y'});
	$('#toDate').datetimepicker({
      timepicker:false,
      format:'d/m/Y'});

	$('#submitButton').click(function () {
		var data = new Object();
			$("#inputForm :input").each(function () {
			var name = $(this).attr('name');
			var input = $(this).val();
			data[name] = input;
		});
		API.search(data);
	});

	$('#clearButton').click(function () {
		$('#inputForm').form('clear')
	});

	API.config();
});


// Show modal for error
function throwError(error) {
  $('#errorValue').append('No Statement found');
  $('#modalError').modal('show');
};

//Download file
function download(file)
{
 window.location=file;
};

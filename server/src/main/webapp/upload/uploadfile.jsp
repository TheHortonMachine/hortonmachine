<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Upload Page</title>


<link rel="stylesheet" type="text/css"
	href="http://ajax.googleapis.com/ajax/libs/dojo/1.11.2/dojo/resources/dojo.css">
<link rel="stylesheet" type="text/css"
	href="http://ajax.googleapis.com/ajax/libs/dojo/1.11.2/dijit/themes/tundra/tundra.css">

<script src="//ajax.googleapis.com/ajax/libs/dojo/1.11.2/dojo/dojo.js"
	djConfig="parseOnLoad: true">
	
</script>

<!-- 
// in case of local dojo install
<style type="text/css"> 
@import "dojo-release-1.11.2/dijit/themes/tundra/tundra.css";
@import "dojo-release-1.11.2/dojo/resources/dojo.css"
</style>


<script type="text/javascript" src="dojo-release-1.11.2/dojo/dojo.js"
	djConfig="parseOnLoad: true">
	
</script>
-->

<script type="text/javascript">
	dojo.require("dijit.ProgressBar");

	function doProgress() {
		var button = window.document.getElementById("submitButton");
		button.disabled = true;
		var max = 100;
		var prog = 0;
		var counter = 0;
		getProgress();
		doProgressLoop(prog, max, counter);
	}

	function doProgressLoop(prog, max, counter) {
		var x = dojo.byId('progress-content').innerHTML;
		var y = parseInt(x);
		if (!isNaN(y)) {
			prog = y;
		}
		jsProgress.update({
			maximum : max,
			progress : prog
		});
		counter = counter + 1;
		dojo.byId('counter').innerHTML = counter;
		if (prog < 100) {
			setTimeout("getProgress()", 500);
			setTimeout("doProgressLoop(" + prog + "," + max + "," + counter
					+ ")", 1000);
		}
	}

	function getProgress() {
		dojo
				.xhrGet({
					url : 'progress', // this calls the '/upload/progress' servlet
					load : function(data) {
						dojo.byId('progress-content').innerHTML = data;
					},
					error : function(data) {
						dojo.byId('progress-content').innerHTML = "Error retrieving progress";
					}
				});
	}
</script>
</head>
<body>
	<div>
		<form name="form1" id="form1" action="upload_file_progress" 
			method="post" enctype="multipart/form-data">
			<input type="hidden" name="hiddenfield1" value="ok"> File to
			upload:</input> <input type="file" size="50" name="file1"> <br /> <input
				type="button" value="Upload" id="submitButton"
				onclick="form1.submit();doProgress();">
		</form>
	</div>
	<div class="tundra">
		Progress:
		<div dojoType="dijit.ProgressBar" style="width: 300px"
			jsId="jsProgress" id="downloadProgress"></div>
	</div>

	<br />
	<br />
	<br />
	<div style="visibility: visible">
		Content from Progress Servlet: <span id="progress-content">---</span><br />
		Counter: <span id="counter">---</span><br />
	</div>
</body>
</html>
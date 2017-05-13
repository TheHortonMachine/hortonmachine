<%@ page
	language="java"
	contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" 
	"http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta
	http-equiv="content-type"
	content="text/html;
      charset=windows-1252">
<title>${param.title}</title>
<%
    String isAdminStr = request.getParameter("isAdmin");
			boolean isAdmin = Boolean.parseBoolean(isAdminStr);
%>
<%@ include file="jquery.jsp"%>
<link
	rel="stylesheet"
	type="text/css"
	href="jgtools.css" />
<script>
    $(".button").button();
    $(".combo").selectmenu();
</script>
</head>
<body>
	<div id="menubar">
		<ul>
			<li><div class="logo"></div></li>
			<%
			    if (isAdmin) {
			%>
			<li><a
				class="active"
				href="admin.jsp">Home</a></li>
			<li class="dropdown"><a
				href="#"
				class="dropbtn">Maps</a>
				<div class="dropdown-content">
					<a href="dataviewer_admin_openlayers.jsp">Shapefiles Openlayers</a>
					<a href="dataviewer_admin.jsp">Shapefiles Nasa World Wind</a>
				</div></li>
			<%
			    } else {
			%>
			<li><a
				class="active"
				href="welcome.jsp">Home</a></li>
			<li class="dropdown"><a
				href="#"
				class="dropbtn">Maps</a>
				<div class="dropdown-content">
					<a href="dataviewer_openlayers.jsp">Shapefiles Openlayers</a>
					<a href="dataviewer.jsp">Shapefiles Nasa World Wind</a>
				</div></li>
			<li><a
				class="active"
				href="admin.jsp">Admin</a></li>
			<%
			    }
			%>
		</ul>
	</div>
	<br>
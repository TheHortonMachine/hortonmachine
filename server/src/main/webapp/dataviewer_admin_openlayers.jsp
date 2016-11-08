<%@ page
	language="java"
	contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" 
	"http://www.w3.org/TR/html4/loose.dtd">
<jsp:include page="header.jsp">
	<jsp:param
		name="title"
		value="Data Viewer" />
	<jsp:param
		name="isAdmin"
		value="true" />
</jsp:include>

<center>
	<jsp:include page="dataviewer_content_openlayers.jsp">
		<jsp:param
			name="isAdmin"
			value="true" />
	</jsp:include>
</center>
<%@ include file="footer.jsp"%>
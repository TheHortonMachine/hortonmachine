<%@ page
	language="java"
	contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" 
	"http://www.w3.org/TR/html4/loose.dtd">
<%@page import="java.util.Enumeration"%>
<jsp:include page="header.jsp">
	<jsp:param
		name="title"
		value="Simple JGrasstools Server" />
	<jsp:param
		name="isAdmin"
		value="false" />
</jsp:include>

<%@ include file="initial_content.jsp"%>
<%@ include file="footer.jsp"%>
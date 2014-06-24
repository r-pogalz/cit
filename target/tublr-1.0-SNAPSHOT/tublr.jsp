<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page import="com.google.appengine.api.datastore.DatastoreService"%>
<%@ page
	import="com.google.appengine.api.datastore.DatastoreServiceFactory"%>
<%@ page import="com.google.appengine.api.datastore.Entity"%>
<%@ page import="com.google.appengine.api.datastore.FetchOptions"%>
<%@ page import="com.google.appengine.api.datastore.Key"%>
<%@ page import="com.google.appengine.api.datastore.KeyFactory"%>
<%@ page import="com.google.appengine.api.datastore.Query"%>
<%@ page
	import="com.google.appengine.api.blobstore.BlobstoreServiceFactory"%>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService"%>
<%@ page import="java.util.List"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">

<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"></meta>
<title>cc-g11-tublr</title>
<link href="/stylesheets/default.css" rel="stylesheet" type="text/css"
	media="all"></link>
</head>

<%
	BlobstoreService blobstoreService = BlobstoreServiceFactory
			.getBlobstoreService();
%>

<body>
	<div id="header-wrap">
		<div id="header">
			<img src="/images/tublr.png" alt="tublr"></img>
			<h3>A photo-sharing service by TU-Berlin!</h3>
		</div>
	</div>
	<c:if test="${hasError}">
		<div>
			<center>
				<font size="3" color="red">${errorMsg}</font>
			</center>
		</div>
	</c:if>

	<div class="post_form_wrap">
		<form class="post_form" action="<%= blobstoreService.createUploadUrl("/tublr") %>" method="post"
			enctype="multipart/form-data">

			<h3>Post a new image:</h3>
			Image: <input name="image" type="file" size="35" maxlength="5000000" />
			<br /> Message [optional]: <input name="text" type="text" size="30"
				maxlength="30" />
			<p>
				<input type="submit" value="Post!" />
			</p>
			<input type="hidden" name="type" value="post" />
		</form>
		<hr />
	</div>

	<%
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Query query = new Query("Post").addSort("currentness",
				Query.SortDirection.DESCENDING);
		List<Entity> posts = datastore.prepare(query).asList(
				FetchOptions.Builder.withLimit(20));
		if (!posts.isEmpty()) {
			for (Entity post : posts) {
				pageContext.setAttribute("postContent",
						post.getProperty("text"));
				pageContext.setAttribute("postCreated",
						post.getProperty("date"));
				pageContext
						.setAttribute("postKeyId", post.getKey().getId());
				pageContext.setAttribute("postImageUrl",
						post.getProperty("imageUrl"));
	%>

	<div id="content-wrapper">
		<div class="post">
			<div class="post_data">
				<div class="post_image">
					<a href="${fn:escapeXml(postImageUrl)}"><img
						src="${fn:escapeXml(postImageUrl)}=s150" /></a>
				</div>
				<h4>Posted on ${fn:escapeXml(postCreated)}:</h4>
				<div class="post_text">${fn:escapeXml(postContent)}</div>
			</div>

			<%
				Query childQuery = new Query("Comment");
						childQuery.setAncestor(post.getKey());
						childQuery.addSort("date", Query.SortDirection.ASCENDING);
						List<Entity> comments = datastore.prepare(childQuery)
								.asList(FetchOptions.Builder.withDefaults());

						if (!comments.isEmpty()) {
							for (Entity comment : comments) {
								pageContext.setAttribute("commentContent",
										comment.getProperty("text"));
								pageContext.setAttribute("commentCreated",
										comment.getProperty("date"));
								pageContext.setAttribute("commentImageUrl",
										comment.getProperty("imageUrl"));
			%>

			<div class="comment_data">
				<div class="comment_image">
					<a href="${fn:escapeXml(commentImageUrl)}"><img
						src="${fn:escapeXml(commentImageUrl)}=s75" /></a>
				</div>
				<h4>Comment posted on ${fn:escapeXml(commentCreated)}:</h4>
				<div class="comment_text">${fn:escapeXml(commentContent)}</div>
			</div>

			<%
				}
						}
			%>

			<div class="comment_form">
				<form action="<%= blobstoreService.createUploadUrl("/tublr") %>" method="post" enctype="multipart/form-data">
					Image: <input name="image" type="file" size="35"
						maxlength="5000000" /> <br /> Message [optional]: <input
						name="text" type="text" size="30" maxlength="30" /><br /> <input
						type="submit" value="Post Comment!" /> <input type="hidden"
						name="type" value="comment" /><input type="hidden"
						name="postKeyId" value="${postKeyId}" />
				</form>
			</div>
		</div>
		<hr />
	</div>
	<%
		}
		}
	%>
</body>
</html>

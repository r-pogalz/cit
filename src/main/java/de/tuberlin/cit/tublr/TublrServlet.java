package de.tuberlin.cit.tublr;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;

/**
 * Servlet that handles POST requests for posting and image and optionally a
 * comment. The image is stored to the blobstore. ImageUrl, comment and
 * additional information (date, currentness of posts) are stored to the
 * datastore.
 * 
 */
public class TublrServlet extends HttpServlet {

	private final static Logger LOGGER = Logger.getLogger(TublrServlet.class
			.getName());

	private static final long serialVersionUID = 5260278144752672664L;

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		final BlobstoreService blobstoreService = BlobstoreServiceFactory
				.getBlobstoreService();
		final Map<String, List<BlobKey>> blobs = blobstoreService
				.getUploads(req);

		final BlobKey blobKey = blobs.get("image").get(0);

		final ImagesService imagesService = ImagesServiceFactory
				.getImagesService();
		final BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
		if (blobInfo.getSize() == 0) {
			setMissingImageError(req);
			req.getRequestDispatcher("/tublr.jsp").forward(req, resp);
			return;
		}

		final String imageUrl = imagesService
				.getServingUrl(ServingUrlOptions.Builder.withBlobKey(blobKey));
		final String text = req.getParameter("text");
		final String type = req.getParameter("type");

		if ("post".equals(type)) {
			savePost(text, imageUrl);
		} else if ("comment".equals(type)) {
			final Long postKeyId = Long
					.parseLong(req.getParameter("postKeyId"));
			saveComment(text, postKeyId, imageUrl);
		}
		resp.sendRedirect("/tublr.jsp");
	}

	private void setMissingImageError(HttpServletRequest req) {
		req.setAttribute("errorMsg", "Missing image!");
		req.setAttribute("hasError", true);
	}

	private void saveComment(String text, long postKeyId, String imageUrl)
			throws ServletException {
		final Date now = new Date();
		final Key key = KeyFactory.createKey("Post", postKeyId);
		final Entity comment = new Entity("Comment", key);
		comment.setProperty("date", now);
		comment.setProperty("text", text);
		comment.setProperty("imageUrl", imageUrl);

		final DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		datastore.put(comment);

		updatePost(now, key, datastore);
	}

	private void updatePost(final Date now, final Key key,
			final DatastoreService datastore) throws ServletException {
		try {
			final Entity post = datastore.get(key);
			post.setProperty("currentness", now);
			datastore.put(post);
		} catch (EntityNotFoundException e) {
			LOGGER.log(Level.SEVERE, "Error during updating post.", e);
			throw new ServletException(e);
		}
	}

	private void savePost(String text, String imageUrl) {
		LOGGER.info("saving post.");
		final Date now = new Date();
		final Entity post = new Entity("Post");
		post.setProperty("date", now);
		post.setProperty("text", text);
		post.setProperty("currentness", now);
		post.setProperty("imageUrl", imageUrl);

		final DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		datastore.put(post);

		LOGGER.info("post successfully stored");
	}
}

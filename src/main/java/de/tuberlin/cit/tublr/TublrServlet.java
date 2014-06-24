package de.tuberlin.cit.tublr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

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

public class TublrServlet extends HttpServlet {

	private static final long serialVersionUID = 5260278144752672664L;

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		final BlobstoreService blobstoreService = BlobstoreServiceFactory
				.getBlobstoreService();
		final Map<String, List<BlobKey>> blobs = blobstoreService
				.getUploads(req);
		final List<BlobKey> blobKeys = blobs.get("image");
		if (CollectionUtils.isEmpty(blobKeys)) {
			req.setAttribute("errorMsg", "Missing image!");
			req.setAttribute("hasError", true);
			req.getRequestDispatcher("/tublr.jsp").forward(req, resp);
			return;
		}

		final BlobKey blobKey = blobKeys.get(0);
		String text = null;
		String type = null;
		Long postKeyId = null;
		try {
			final ServletFileUpload upload = new ServletFileUpload();
			final FileItemIterator iterator = upload.getItemIterator(req);
			while (iterator.hasNext()) {
				final FileItemStream item = iterator.next();
				final InputStream stream = item.openStream();

				if (item.isFormField()) {
					if ("text".equals(item.getFieldName())) {
						text = Streams.asString(stream);
					}
					if ("type".equals(item.getFieldName())) {
						type = Streams.asString(stream);
					}
					if ("postKeyId".equals(item.getFieldName())) {
						postKeyId = Long.parseLong(Streams.asString(stream));
					}
				}
			}
		} catch (Exception ex) {
			throw new ServletException(ex);
		}

		final ImagesService imagesService = ImagesServiceFactory
				.getImagesService();
		final String imageUrl = imagesService
				.getServingUrl(ServingUrlOptions.Builder.withBlobKey(blobKey));

		if ("post".equals(type)) {
			savePost(text, imageUrl);
		} else if ("comment".equals(type)) {
			saveComment(text, postKeyId, imageUrl);
		}
		resp.sendRedirect("/tublr.jsp");
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
			throw new ServletException(e);
		}
	}

	private void savePost(String text, String imageUrl) {
		final Date now = new Date();
		final Entity post = new Entity("Post");
		post.setProperty("date", now);
		post.setProperty("text", text);
		post.setProperty("currentness", now);
		post.setProperty("imageUrl", imageUrl);

		final DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		datastore.put(post);
	}
}

package de.tuberlin.cit.tublr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;

public class TublrServlet extends HttpServlet {

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String text = null;
		String type = null;
		Long postKeyId = null;
		BlobKey blobKey = null;

		try {
			ServletFileUpload upload = new ServletFileUpload();

			FileItemIterator iterator = upload.getItemIterator(req);
			while (iterator.hasNext()) {
				FileItemStream item = iterator.next();
				InputStream stream = item.openStream();

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
				} else {
					FileService fileService = FileServiceFactory
							.getFileService();

					AppEngineFile file;

					String mimeType = item.getContentType();

					file = fileService.createNewBlobFile(mimeType);

					boolean lock = true;

					FileWriteChannel writeChannel = fileService
							.openWriteChannel(file, lock);

					int len;

					byte[] buffer = new byte[8192];

					while ((len = stream.read(buffer, 0, buffer.length)) != -1) {

						ByteBuffer buf = ByteBuffer.wrap(buffer, 0, len);

						writeChannel.write(buf);

					}

					writeChannel.closeFinally();

					blobKey = fileService.getBlobKey(file);
				}
			}
		} catch (Exception ex) {
			throw new ServletException(ex);
		}

		final ImagesService imagesService = ImagesServiceFactory
				.getImagesService();

		try {
			String imageUrl = imagesService.getServingUrl(blobKey);

			if ("post".equals(type)) {
				savePost(text, imageUrl);
			} else if ("comment".equals(type)) {
				saveComment(text, postKeyId, imageUrl);
			}
			resp.sendRedirect("/tublr.jsp");
		} catch (IllegalArgumentException e) {
			req.setAttribute("errorMsg", "Missing image!");
			req.setAttribute("hasError", true);
			req.getRequestDispatcher("/tublr.jsp").forward(req, resp);
		}
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

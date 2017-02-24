package org.osc.core.server.resolver.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

import aQute.bnd.service.url.TaggedData;
import aQute.bnd.service.url.URLConnector;

public class JarURLConnector implements URLConnector {

	@Override
	public TaggedData connectTagged(URL url) throws Exception {
		URLConnection connection = url.openConnection();
		if (connection instanceof JarURLConnection) {
            connection.setUseCaches(false);
        }
		return new TaggedData(connection, connection.getInputStream());
	}

	@Override
	public InputStream connect(URL url) throws IOException, Exception {
		return connectTagged(url).getInputStream();
	}

	@Override
	public TaggedData connectTagged(URL url, String tag) throws Exception {
		return connectTagged(url);
	}

}

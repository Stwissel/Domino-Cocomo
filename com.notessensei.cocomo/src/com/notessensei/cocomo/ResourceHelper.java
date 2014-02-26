/** ========================================================================= *
 * Copyright (C) 2014,      IBM Corporation ( http://www.ibm.com/ )           *
 *                            All rights reserved.                            *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== */

package com.notessensei.cocomo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * ResourceHelper provides class to load files from the resources directory of
 * the JAR file into strings and streams
 * 
 * @author stw
 * 
 */
public class ResourceHelper {

	/**
	 * How many byte to read when reading a file
	 */
	public static final int	RESOURCE_READ_BYTESIZE	= 1024;

	/**
	 * Returns a JAR resource as an input stream
	 * 
	 * @param resourceName
	 * @return the resouces
	 */
	public InputStream resourceToInputStream(final String resourceName) {
		InputStream in = null;
		String fullSourcePath = "resources/" + resourceName;
		/*
		 * Attempt to retrieve all resouces. didn't really work did it?
		 */
		ArrayList<String> sources = new ArrayList<String>();
		sources.add("*.*");
		sources.add("/*.*");
		sources.add("*.xml");
		sources.add("/resources/*.*");
		sources.add("resources/*.*");
		// Test insertion
		Enumeration<URL> e;
		try {
			for (String r : sources) {
				e = this.getClass().getClassLoader().getResources(r);

				while (e.hasMoreElements()) {
					URL u = e.nextElement();
					System.out.println(u.toString());
				}

				e = ClassLoader.getSystemResources(r);
				while (e.hasMoreElements()) {
					URL u = e.nextElement();
					System.out.println(u.toString());
				}

			}

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		in = this.getClass().getResourceAsStream(fullSourcePath);

		if (in == null) {
			System.out.println("Resource not found:" + fullSourcePath);
			return null;
		}

		return in;

	}

	/**
	 * @param resourceName
	 * @return the resource we were looking for
	 */
	public String resourceToString(final String resourceName) {

		StringBuffer b = new StringBuffer();
		InputStream in = null;

		try {

			in = this.resourceToInputStream(resourceName);

			if (in != null) {

				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				char[] buf = new char[ResourceHelper.RESOURCE_READ_BYTESIZE];

				int numRead = 0;
				while ((numRead = reader.read(buf)) != -1) {
					String readData = String.valueOf(buf, 0, numRead);
					b.append(readData);
					buf = new char[ResourceHelper.RESOURCE_READ_BYTESIZE];
				}

				reader.close();
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return b.toString();

	}

	/**
	 * @param source
	 * @return A path with a separator at the end
	 */
	public static String pathWithSeperator(final String source) {
		char delimiter = File.separatorChar;

		if (source.charAt(source.length() - 1) != delimiter) {
			return source + File.separator;
		}

		return source;
	}

	/**
	 * Copies a resource from the Jar into the file system
	 * 
	 * @param targetDirectory
	 * @param resourceName
	 * @return success true/false
	 */
	public boolean resourceToFileSystem(final String targetDirectory, final String resourceName) {
		boolean result = true;
		String fullTargetPath = targetDirectory + File.separator + resourceName;

		InputStream in = null;
		OutputStream out = null;
		try {
			in = this.resourceToInputStream(resourceName);
			out = new FileOutputStream(fullTargetPath);
			byte[] buffer = new byte[4096];
			int n = 0;
			while (-1 != (n = in.read(buffer))) {
				out.write(buffer, 0, n);
			}
			in.close();
			out.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			result = false;
		} catch (IOException e) {
			e.printStackTrace();
			result = false;
		}

		return result;

	}

}

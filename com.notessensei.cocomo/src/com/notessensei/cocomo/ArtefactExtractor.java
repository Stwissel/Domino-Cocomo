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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author stw
 * 
 */
public class ArtefactExtractor {

	public static String help() {
		return "Usage:java -jar cocomo.jar sourceDir ReportFile [CommandFile]";
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		if (args.length < 2) {
			System.out.println(ArtefactExtractor.help());
			System.exit(1);
		}

		String sourceDir = ResourceHelper.pathWithSeperator(args[0]);
		String resultFile = args[1];

		File morituri = new File(resultFile);
		if (morituri.exists()) {
			morituri.delete();
		}

		ArtefactExtractor ae = new ArtefactExtractor(sourceDir, resultFile);

		if (args.length > 2) {
			String commandFile = args[2];
			ae.setCommandFile(commandFile);
		}
		ae.extract();

		System.out.println("Done!");

	}

	public void setCommandFile(String commandFile) {
		this.commandFileName = commandFile;

	}

	private final String					reportFileName;
	private final File						rootDir;
	private final Collection<String>		tagsForLOC;
	private final Collection<String>		xmlExtensions;
	private final Map<String, Set<String>>	reportMappings;
	private final Map<String, String>		sourceTypes;
	private String							commandFileName	= null;

	public ArtefactExtractor(String sourceDir, String resultFileName) {
		this.reportFileName = resultFileName;
		this.rootDir = new File(ResourceHelper.pathWithSeperator(sourceDir));
		this.tagsForLOC = this.populateTagsForLoc();
		this.xmlExtensions = this.populateXmlExtension();
		this.reportMappings = this.populateReportMappings();
		this.sourceTypes = this.populateSourceTypes();
	}

	/**
	 * Entry routine to scan all applications in one directory
	 * 
	 * @throws IOException
	 */
	public void extract() throws IOException {
		if (!this.rootDir.isDirectory()) {
			System.err.println("You need to point to the directory above the On-Disk-Projects!");
			System.exit(1);
		}

		File result = new File(this.reportFileName);
		FileOutputStream out = new FileOutputStream(result);
		PrintWriter pw = new PrintWriter(out);
		this.writeResultHeader(pw);

		if (this.commandFileName != null) {
			Scanner commandScanner = new Scanner(new File(this.commandFileName));
			while (commandScanner.hasNextLine()) {
				String nextLine = commandScanner.nextLine().trim();
				if (!nextLine.startsWith("#") && !nextLine.equals("")) {
					File f = new File(nextLine);
					if (f.isDirectory()) {
						System.out.println("Working on application: " + f.getName());
						this.analyzeOneApplication(f, pw);
					}
				}
			}
		} else {

			for (File f : this.rootDir.listFiles()) {
				if (f.isDirectory() && !f.getName().startsWith(".")) {
					System.out.println("Working on application: " + f.getName());
					this.analyzeOneApplication(f, pw);
					pw.flush();
				}
			}
		}
		pw.flush();
		pw.close();
		out.close();

	}

	private void analyzeOneApplication(File appDir, PrintWriter pw) throws FileNotFoundException {
		// On this level we only have directories we are interested in
		final ArtefactResult result = new ArtefactResult();
		for (File subDir : appDir.listFiles()) {
			if (subDir.isDirectory()) {
				this.scanDirectory(subDir, result);
			}
		}

		// Write out the raw metrics file in case someone has ideas
		File metrics = new File(appDir.getAbsolutePath() + "app.metrics");
		PrintWriter mw = new PrintWriter(new FileOutputStream(metrics));
		mw.write(result.toString());
		mw.flush();
		mw.close();

		pw.write(appDir.getName());
		pw.write(",");
		pw.write(result.getResults(this.reportMappings));
		pw.write("\n");

	}

	/**
	 * Checks if the current line of code contains any keywords we want to count
	 * 
	 * @param workLine
	 */
	private void inspectCodeLine(String inputLine, ArtefactResult result) {
		String workLine = inputLine.trim().toLowerCase();

		for (String curkey : this.sourceTypes.keySet()) {
			if (workLine.contains(curkey.toLowerCase())) {
				result.add(this.sourceTypes.get(curkey));
			}
		}
	}

	private boolean isElementFromTemplate(Element nodeElement) {

		// We need to check if this is an inherited element,
		// so we check for the attribute fromtemplate

		if (nodeElement.hasAttribute("fromtemplate")) {
			String fromTemplate = nodeElement.getAttribute("fromtemplate");
			if (!fromTemplate.trim().equals("")) {
				// has the attribute and it isn't empty
				return true;
			}
		}
		// Not from template, since we didn't find the property
		return false;
	}

	/**
	 * Process that one element
	 * 
	 * @param element
	 * @param result
	 * @param extension
	 */
	private void iterateAndCount(Element element, ArtefactResult result, String extension) {

		NodeList nodeList = element.getChildNodes();

		for (int i = 0, size = nodeList.getLength(); i < size; i++) {
			Node node = nodeList.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element nodeElement = (Element) node;
				String curName = node.getNodeName();

				// Check for template
				boolean elementIsFromTemplate = this.isElementFromTemplate(nodeElement);

				if (elementIsFromTemplate) {
					String fromTemplate = nodeElement.getAttribute("fromtemplate");
					result.add("template_" + fromTemplate);
					return;
				}

				result.add(curName);
				result.add(extension + "_" + curName);

				this.updateLOC(nodeElement, result);
				this.iterateAndCount(nodeElement, result, extension);
			}
		}
	}

	/**
	 * All columns we want to have in the final report, and the raw tags that
	 * make up the columns, Loads values from a properties file, if it can't
	 * find it a JAR internal one is used but written out
	 * 
	 * @return
	 */
	private Map<String, Set<String>> populateReportMappings() {
		Map<String, Set<String>> result = new TreeMap<String, Set<String>>();
		Properties workingProperties = new Properties();

		String propFileName = this.getClass().getName() + ".properties";
		File propFile = new File(propFileName);
		boolean success = false;
		if (propFile.exists()) {
			// Loading properties from File
			try {
				workingProperties.load(new FileInputStream(propFile));
				success = true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (!success) {
			System.out.println("Using defaults for Report definition:" + propFile.getAbsolutePath());
			InputStream in = this.getClass().getResourceAsStream("defaultReport.properties");
			try {
				workingProperties.load(in);
				in.close();
				workingProperties.store(new FileOutputStream(propFile), "***** Default values ******");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		for (Map.Entry<Object, Object> entry : workingProperties.entrySet()) {
			String key = entry.getKey().toString();
			String[] rawValues = entry.getValue().toString().split(",");
			TreeSet<String> values = new TreeSet<String>();
			for (String v : rawValues) {
				if (!v.trim().equals("")) {
					values.add(v.trim());
				}
			}
			result.put(key, values);
		}

		return result;
	}

	private Map<String, String> populateSourceTypes() {
		Map<String, String> result = new TreeMap<String, String>();
		InputStream in = this.getClass().getResourceAsStream("SourceType.properties");
		Properties workingProperties = new Properties();
		try {
			workingProperties.load(in);
			for (Map.Entry<Object, Object> me : workingProperties.entrySet()) {
				result.put(me.getKey().toString(), me.getValue().toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * All Tag names that contain code
	 * 
	 * @return
	 */
	private Collection<String> populateTagsForLoc() {
		Collection<String> result = new TreeSet<String>();
		InputStream in = this.getClass().getResourceAsStream("LocTags.properties");
		Scanner s = new Scanner(in);
		while (s.hasNextLine()) {
			String w = s.nextLine().trim();
			if (!w.startsWith("#") && !w.equals("")) {
				result.add(w);
			}
		}

		return result;
	}

	/**
	 * All File extensions we know that contain XML parsable content
	 * 
	 * @return
	 */
	private Collection<String> populateXmlExtension() {
		Collection<String> result = new TreeSet<String>();
		InputStream in = this.getClass().getResourceAsStream("xmlExtensions.properties");
		Scanner s = new Scanner(in);
		while (s.hasNextLine()) {
			String w = s.nextLine().trim();
			if (!w.startsWith("#") && !w.equals("")) {
				result.add(w);
			}
		}
		return result;
	}

	private void scanCodeFile(File f, ArtefactResult result, String extension) throws FileNotFoundException {

		int addLoc = 0;
		int addFunctions = 0;
		Scanner sc = new Scanner(f);
		while (sc.hasNextLine()) {
			String curLine = sc.nextLine().trim().toLowerCase();
			if (!curLine.trim().equals("")) {
				// Slightly crude way of getting functions and methods count
				if (curLine.startsWith("sub") || curLine.startsWith("function") || curLine.startsWith("public class")
						|| curLine.startsWith("public") || (curLine.startsWith("private") && curLine.contains("{"))
						|| (curLine.startsWith("protected") && curLine.contains("{"))) {
					addFunctions++;
				}
				addLoc++;
			}
		}
		result.add("LOC_" + extension, addLoc);
		result.add("LOC", addLoc);
		result.add("functions_" + extension, addFunctions);
		result.add("functions", addFunctions);
	}

	/**
	 * Recursive call to process all
	 * 
	 * @param subDir
	 * @param result
	 * @throws FileNotFoundException
	 */
	private void scanDirectory(File subDir, ArtefactResult result) throws FileNotFoundException {

		for (File f : subDir.listFiles()) {
			if (f.isDirectory() && !f.getName().startsWith(".")) {
				this.scanDirectory(f, result);
			} else {
				this.scanOneFile(f, result);
			}
		}

	}

	/**
	 * Scans files based on their file type
	 * 
	 * @param f
	 * @param result
	 * @throws FileNotFoundException
	 */
	private void scanOneFile(File f, ArtefactResult result) throws FileNotFoundException {
		String fname = f.getName();
		int pos = fname.lastIndexOf(".");
		if (pos < 0) {
			// File without extension - we don't process
			return;
		}
		String extension = fname.substring(pos + 1).toLowerCase().trim();

		if (extension.endsWith("metadata")) {
			// We don't need that either
			return;
		}

		// Capture the fact
		result.add("File-" + extension);

		// Special names

		if (fname.equals("database.properties") || this.xmlExtensions.contains(extension)) {
			// Classic XML to process
			this.scanXMLFile(f, result, extension);

		} else if (extension.equals("java") || extension.equals("js") || extension.equals("jss") || extension.equals("lss")) {
			// Java and JavaScript plain source code
			this.scanCodeFile(f, result, extension);

		} else if (extension.equals("properties")) {
			// Plain ASCII Files to count...
			this.scanPlainFile(f, result);

		} else {
			// Just helper file count
			result.add("otherFiles");
		}

	}

	private void scanPlainFile(File f, ArtefactResult result) throws FileNotFoundException {
		int addLoc = 0;
		Scanner sc = new Scanner(f);
		while (sc.hasNextLine()) {
			if (!sc.nextLine().trim().equals("")) {
				addLoc++;
			}
		}
		result.add("LOC_other", addLoc);
		result.add("LOC", addLoc);
	}

	private void scanXMLFile(File f, ArtefactResult result, String extension) {
		Document xDoc = DomHelper.getDomHelper().file2Dom(f.getPath());
		if (xDoc != null) {
			Element element = xDoc.getDocumentElement();
			this.iterateAndCount(element, result, extension);
		}

	}

	/**
	 * Counts element for LOC count if required
	 * 
	 * @param nodeElement
	 */
	private void updateLOC(Element element, ArtefactResult result) {
		String language = element.getNodeName();
		if (this.tagsForLOC.contains(language)) {
			// Extracts the # of lines inside an element
			// typically used for lotuscript and formula elements
			// also checks for code keywords
			int addCount = 0;

			NodeList nodeList = element.getChildNodes();
			for (int i = 0, size = nodeList.getLength(); i < size; i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Node.TEXT_NODE) {

					BufferedReader source = new BufferedReader(new StringReader(node.getNodeValue()));

					String curLine;
					try {
						while ((curLine = source.readLine()) != null) {
							String workLine = curLine.trim().toLowerCase();

							if (!workLine.equals("") && !workLine.startsWith("'")) {
								this.inspectCodeLine(workLine, result);
								addCount += 1;
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			}
			// Now add our findings
			result.add("LOC", addCount);
			result.add("LOC_" + language, addCount);
		}
	}

	private void writeResultHeader(PrintWriter pw) {
		Set<String> headers = new TreeSet<String>();
		headers.addAll(this.reportMappings.keySet());
		pw.append("Application");
		for (String h : headers) {
			pw.append(",");
			pw.write(h);
		}
		pw.append(",Total");
		pw.write("\n");
	}
}

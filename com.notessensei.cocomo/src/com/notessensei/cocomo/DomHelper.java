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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * DomHelper contains commonly used functions for dealing with DOM objects like
 * conversion from String to DOM and from DOM to String or loading of a file
 * into a DOM.
 * 
 * @author stw
 * 
 */
public final class DomHelper {

	/**
	 * How many byte to read when reading a file
	 */
	private static final int	FILE_READ_BYTESIZE	= 1024;

	/**
	 * Our Domhelper singleton
	 */
	private static DomHelper	domHelper			= null;

	/**
	 * Get access to the Domhelper class. We load it only once
	 * 
	 * @return DomHelper --- a singleton DomHelper instance
	 */
	public static final synchronized DomHelper getDomHelper() {
		if (DomHelper.domHelper == null) {
			DomHelper.domHelper = new DomHelper();
		}
		return DomHelper.domHelper;
	}

	/**
	 * Private constructor to ensure the class doesn't get instantiated
	 */
	private DomHelper() {
		// No action needed, just hide the constructor
	}

	/**
	 * Compares two elements
	 * 
	 * @param source
	 *            Source Element
	 * @param target
	 *            Target Element
	 * @return true=equal
	 */
	public synchronized final boolean areElementsEqual(Element source, Element target) {
		return this.areElementsEqual(source, target, null);
	}

	/**
	 * Compares 2 Elements if they are similar. Ignores white space used for the
	 * smart-import functionality of DxlImporter We consider elements equal if
	 * they have the same text, attributes and child elements
	 * 
	 * @param source
	 *            The first element
	 * @param target
	 *            The second element
	 * @param elemetsToExclude
	 *            child elements we don't need to compare
	 * @return true/false
	 */
	public final synchronized boolean areElementsEqual(Element source, Element target, List<String> elemetsToExclude) {
		boolean result = false;

		// If it is an excluded element we don't need to work further
		if (elemetsToExclude != null && elemetsToExclude.contains(source.getNodeName())) {
			return true;
		}

		// Compare attributes
		NamedNodeMap sourceAttributes = source.getAttributes();

		// Check all source vs. target attributes
		for (int i = 0; i < sourceAttributes.getLength(); i++) {
			Node curAttr = sourceAttributes.item(i);
			String curName = curAttr.getNodeName();
			String curVal = curAttr.getNodeValue();
			if (target.hasAttribute(curName)) {
				if (!curVal.equals(target.getAttribute(curName))) {
					// The values are different
					return false;
				}
			} else {
				// We are done. The attribute is missing
				return false;
			}
		}

		NamedNodeMap targetAttributes = target.getAttributes();
		// Now reverse. We only need to check for missing attributes here
		for (int i = 0; i < targetAttributes.getLength(); i++) {
			Node curAttr = targetAttributes.item(i);
			String curName = curAttr.getNodeName();
			if (!source.hasAttribute(curName)) {
				return false;
			}
		}

		// Compare the text
		String sourceText = source.getTextContent();
		String targetText = target.getTextContent();

		if ((sourceText == null && targetText != null) || (sourceText != null && targetText == null)) {
			return false;
		} else if (sourceText == null && targetText == null) {
			return true;
		} else if (sourceText != null && !sourceText.equals(targetText)) {
			return false;
		}

		// Compare children

		NodeList sourceChildren = source.getChildNodes();
		NodeList targetChildren = target.getChildNodes();

		if (sourceChildren.getLength() != targetChildren.getLength()) {
			return false;
		}

		for (int i = 0; i < sourceChildren.getLength(); i++) {
			Node curSourceNode = sourceChildren.item(i);
			Node curTargetNode = targetChildren.item(i);

			if (curSourceNode.getNodeType() != curTargetNode.getNodeType()) {
				return false;
			}

			if ((curSourceNode.getNodeType() == Node.ELEMENT_NODE)
					&& !this.areElementsEqual((Element) curSourceNode, (Element) curTargetNode, elemetsToExclude)) {
				return false;
			}
		}

		return result;
	}

	/**
	 * Creates an empty DOM document for use in auxiliary functions
	 * 
	 * @return empty dom
	 */
	public synchronized final Document createDomDocument() {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
			return doc;
		} catch (ParserConfigurationException e) {
			// No action if it fails
		}
		return null;
	}

	/**
	 * Converts a DOM into a String to handle it outside
	 * 
	 * @param dom
	 *            - a DOM, can be a Document or a Node
	 * @param outFileName
	 *            The name to store too
	 */
	public synchronized final void dom2File(Node dom, String outFileName) {
		// To make sure the file is clean we take the step via the string
		// not high performance but good enough for us
		String stuff = this.dom2String(dom);
		this.string2File(outFileName, stuff);
	}

	/**
	 * Converts a DOM into a String to handle it outside
	 * 
	 * @param dom
	 *            - a DOM
	 * @return the DOM in string format
	 */
	public synchronized final String dom2String(Node dom) {
		String result = null;

		StreamResult xResult = null;
		DOMSource source = null;

		try {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			xResult = new StreamResult(new StringWriter());
			source = new DOMSource(dom);
			// We don't want the XML declaration in front
			transformer.setOutputProperty("omit-xml-declaration", "yes");
			transformer.transform(source, xResult);
			result = xResult.getWriter().toString();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return this.stripEmptyLines(result);
	}

	/**
	 * Parses a string containing XML and returns a DocumentFragment
	 * 
	 * @param doc
	 *            The document the fragment will belong to
	 * @param fragmentSourceFileName
	 *            the file containing the XML fragment (should be proper XML,
	 *            but doesn't need a common root
	 * @return the XML Fragment created containing the nodes of the parsed XML.
	 * 
	 */
	public synchronized final DocumentFragment file2DocumentFragment(Document doc, String fragmentSourceFileName) {

		String fragmentSource = this.file2String(fragmentSourceFileName);
		if (fragmentSource == null) {
			System.err.println("File does not exist: " + fragmentSourceFileName);
			return null;
		}
		return this.string2DocumentFragment(doc, fragmentSource);
	}

	/**
	 * Creates a DOM object based on a String in XML format. Returns null on
	 * failure
	 * 
	 * @param sourceFileName
	 *            Something in XML format
	 * @return a proper DOM
	 */
	public synchronized final Document file2Dom(String sourceFileName) {
		// Create a DOM builder and parse the source
		Document d = null;
		File sourceFile = new File(sourceFileName);
		if (!sourceFile.exists()) {
			System.err.println("No such file: " + sourceFileName);
			return null;
		} else if (sourceFile.isDirectory()) {
			System.err.println(sourceFileName + " is a directory, but must be a file");
			return null;
		}
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false); // Will blow if set to true
		factory.setNamespaceAware(true);

		InputSource source = null;

		try {
			source = new InputSource(new FileReader(sourceFile));
			DocumentBuilder docb = factory.newDocumentBuilder();
			d = docb.parse(source);

		} catch (Exception e) {
			e.printStackTrace();
			d = null;
		}

		if (d == null) {
			System.out.println("DOM from file generation failed:\n" + sourceFile.getAbsolutePath());
		}
		return d;
	}

	/**
	 * Reads the entire content of a file into a string
	 * 
	 * @param inFileName
	 *            the file to be read
	 * @return the resulting string
	 */
	public synchronized final String file2String(String inFileName) {
		File inFile = new File(inFileName);

		if (!inFile.exists()) {
			System.err.println("No such file: " + inFileName);
			return null;
		} else if (inFile.isDirectory()) {
			System.err.println(inFileName + " is a directory, but must be a file");
			return null;
		}

		long filesize = inFile.length();

		StringBuffer fileData = new StringBuffer();
		long totalRead = 0L;
		try {
			filesize = inFile.length();
			BufferedReader reader = new BufferedReader(new FileReader(inFile));
			char[] buf = new char[DomHelper.FILE_READ_BYTESIZE];

			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				totalRead += numRead;
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[DomHelper.FILE_READ_BYTESIZE];
			}
			// The reported size is often longer than the real one, but never
			// more than the the buffer size
			if (totalRead + DomHelper.FILE_READ_BYTESIZE < filesize) {
				System.err.print("File read error, reported size is ");
				System.err.print(filesize);
				System.err.print(" but only read ");
				System.err.println(totalRead);
			} else {
				System.out.print("File " + inFileName + " read:");
				System.out.println(totalRead);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fileData.toString();
	}

	/**
	 * Creates an String representation of an Element with some Considerations
	 * for text-list and text values
	 * 
	 * @param sourceElement
	 * @return The text list
	 */
	public synchronized final String getElementString(Element sourceElement) {
		String elementName = sourceElement.getTagName();
		StringBuffer result = new StringBuffer();
		if (!elementName.equals("textlist") && !elementName.equals("text")) {
			result.append(elementName);
			result.append(this.getAttributeString(sourceElement.getAttributes()));
		}

		NodeList children = sourceElement.getChildNodes();

		if (children != null) {
			for (int i = 0; i < children.getLength(); i++) {
				Node curNode = children.item(i);
				short nodeType = curNode.getNodeType();
				if (nodeType == Node.ELEMENT_NODE) {
					Element curElement = (Element) curNode;
					result.append(this.getElementString(curElement));
				} else if (nodeType == Node.TEXT_NODE) {
					result.append(curNode.getNodeValue());
					result.append("\n");
				}
			}
		}

		// Remove too much empty lines
		char newLine = new String("\n").charAt(0);
		for (int i = result.length() - 1; i > 0; i--) {
			char curChar = result.charAt(i);
			char previousChar = result.charAt(i - 1);

			// If both are new lines we remove one
			if (newLine == curChar && newLine == previousChar) {
				result.deleteCharAt(i);
			}
		}

		return result.toString();
	}

	/**
	 * Removes double File separators or Mid-File path separators
	 * 
	 * @param input
	 *            : the InputString
	 * @return The cleaned-up file name
	 */
	public synchronized final String removeInvalidCharsFromFileName(String input) {
		StringBuffer b = new StringBuffer();
		b.append(input);
		return this.removeInvalidCharsFromFileName(b);
	}

	/**
	 * Removes double File separators or Mid-File path separators
	 * 
	 * @param b
	 *            - The buffer - buffer will be modified!
	 * @return The cleaned-up file name
	 */
	public synchronized final String removeInvalidCharsFromFileName(StringBuffer b) {

		String invalidChars = "#%<>{}~:*?\\/|";
		for (int i = b.length() - 1; i > 1; i--) {
			// We walk backwards through the file and remove invalid stuff
			// we don't check the first
			// two chars since they actually might have a path separator
			// e.g. c:
			char curChar = b.charAt(i);
			// Make sure we dont have a drive sign in here
			if (curChar == File.pathSeparatorChar) {
				b.replace(i, i + 1, File.separator);
				// Invalid chars removal, but care needed
				// on Mac : is a valid file separator
				// So we check if the current Char is in the list of invalid
				// chars
			} else if (curChar != File.separatorChar && (invalidChars.indexOf(curChar) > -1)) {
				b.replace(i, i + 1, "_");
			}
			// Remove duplicate File separators
			if (curChar == File.separatorChar && b.charAt(i - 1) == File.separatorChar) {
				b.deleteCharAt(i);
			}
		}
		return b.toString();
	}

	/**
	 * Parses a string containing XML and returns a DocumentFragment
	 * 
	 * @param doc
	 *            The document the fragment will belong to
	 * @param fragmentSource
	 *            the XML fragment (should be proper XML, but doesn't need a
	 *            common root
	 * @return the XML Fragment created containing the nodes of the parsed XML.
	 * 
	 */
	public synchronized final DocumentFragment string2DocumentFragment(Document doc, String fragmentSource) {
		// Wrap the fragment in an arbitrary element
		String fragment = "<fragment>" + fragmentSource + "</fragment>";
		try {
			// Create a DOM builder and parse the fragment
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			InputSource source = new InputSource(new StringReader(fragment));
			Document d = factory.newDocumentBuilder().parse(source);

			// Import the nodes of the new document into doc so that they
			// will be compatible with doc
			Node node = doc.importNode(d.getDocumentElement(), true);

			// Create the document fragment node to hold the new nodes
			DocumentFragment docfrag = doc.createDocumentFragment();

			// Move the nodes into the fragment
			while (node.hasChildNodes()) {
				docfrag.appendChild(node.removeChild(node.getFirstChild()));
			}

			// Return the fragment
			return docfrag;
		} catch (SAXException e) {
			// A parsing error occurred; the xml input is not valid
		} catch (ParserConfigurationException e) {
			// No action
		} catch (IOException e) {
			// No action
		}
		return null;
	}

	/**
	 * Creates a DOM object based on a String in XML format. Returns null on
	 * failure
	 * 
	 * @param sourceString
	 *            Something in XML format
	 * @return a proper DOM
	 */
	public synchronized final Document string2Dom(String sourceString) {
		// Create a DOM builder and parse the source
		Document d = null;
		if (sourceString != null) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false); // Will blow if set to true
			factory.setNamespaceAware(true);

			InputSource source = new InputSource(new StringReader(sourceString));

			try {
				DocumentBuilder docb = factory.newDocumentBuilder();
				d = docb.parse(source);

			} catch (Exception e) {
				e.printStackTrace();
				d = null;
			}
		}

		if (d == null) {
			System.out.println("DOM generation failed:\n" + sourceString);
		}
		return d;
	}

	/**
	 * Writes a string into a file, deletes the file if it exists
	 * 
	 * @param outFileName
	 *            the name of the file to write to
	 * @param stuff
	 *            text to write
	 */
	public synchronized final void string2File(String outFileName, String stuff) {
		// Remove the file
		File outFile = new File(outFileName);

		try {
			if (outFile.exists()) {
				if (outFile.isDirectory()) {
					// Not a good idea to send a file into a directory :-)
					System.err.print("Output Error: " + outFileName + " is a directory");
					return;
				}

				// We delete the file now
				outFile.delete();
			}
		} catch (Exception e) {
			// Do nothing if deletion fails
		}
		// Make sure the directory tree exists
		String sep = File.separator;
		int max = outFileName.lastIndexOf(sep);
		// max = -1 => local file, max = 0 => root specified
		if (max > 0) {
			String dirName = outFileName.substring(0, max);
			File outDir = new File(dirName);
			outDir.mkdirs();
		}

		// Write out
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(outFile));
			w.write(stuff);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (w != null) {
					w.close();
				}
			} catch (Exception e2) {
				// No action here
			}
		}
	}

	/**
	 * Extracts results from an Document using XPath and saves the result in a
	 * file - helper if we don't have namespaces
	 * 
	 * @param doc
	 *            the document with the input
	 * @param xPathString
	 *            the XPath expression
	 * @param outFileName
	 *            - where to store
	 */
	public synchronized final void xpath2File(Document doc, String xPathString, String outFileName) {
		this.xpath2File(doc, xPathString, outFileName, null);
	}

	/**
	 * Extracts results from an Document using XPath and saves the result in a
	 * file
	 * 
	 * @param doc
	 *            the document with the input
	 * @param xPathString
	 *            the XPath expression
	 * @param outFileName
	 *            - where to store
	 * @param additionalNamespaces
	 *            - additional Namespaces
	 */
	public synchronized final void xpath2File(Document doc, String xPathString, String outFileName,
			Map<String, String> additionalNamespaces) {
		String xPathResult = this.xpath2String(doc, xPathString, additionalNamespaces);
		this.string2File(outFileName, xPathResult);
	}

	/**
	 * @param doc
	 *            the input document
	 * @param xPathString
	 *            the XPath expression
	 * @return NodeList as result of the XPath expression
	 */
	public synchronized final NodeList xpath2NodeList(Document doc, String xPathString) {
		return this.xpath2NodeList(doc, xPathString, null);
	}

	/**
	 * Extracts results from an Document using XPath and returns the result as
	 * String
	 * 
	 * @param doc
	 *            the document with the input
	 * @param xPathString
	 *            the XPath expression
	 * @param additionalNamespaces
	 *            Optional Namespaces for the extraction
	 * @return the String with the result
	 */
	public synchronized final NodeList xpath2NodeList(Document doc, String xPathString, Map<String, String> additionalNamespaces) {

		Object exprResult = null;
		XPath xpath = XPathFactory.newInstance().newXPath();

		// We need that otherwise the transformations fail!
		MagicNamespaceContext nsc = new MagicNamespaceContext();
		if (additionalNamespaces != null) {
			nsc.addNamespaces(additionalNamespaces);
		}
		// this makes sure our XPath works
		xpath.setNamespaceContext(nsc);

		try {
			exprResult = xpath.evaluate(xPathString, doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			System.err.println("XPATH failed for " + xPathString);
			System.err.println(e.getMessage());
		}
		// if it failed we abort
		if (exprResult == null) {
			return null;
		}
		// We now extract the Node list of our results. Can be one or more
		// hits which we process all
		NodeList nodes = (NodeList) exprResult;

		return nodes;

	}

	/**
	 * Extracts results from an Document using XPath and returns the result as
	 * String - helper if we don't have namespaces
	 * 
	 * @param doc
	 *            the document with the input
	 * @param xPathString
	 *            the XPath expression
	 * @return the String with the result
	 */
	public synchronized final String xpath2String(Document doc, String xPathString) {
		return this.xpath2String(doc, xPathString, null);
	}

	/**
	 * Extracts results from an Document using XPath and returns the result as
	 * String
	 * 
	 * @param doc
	 *            the document with the input
	 * @param xPathString
	 *            the XPath expression
	 * @param additionalNamespaces
	 *            Optional Namespaces for the extraction
	 * @return the String with the result
	 */
	public synchronized final String xpath2String(Document doc, String xPathString, Map<String, String> additionalNamespaces) {
		StringBuffer b = new StringBuffer();

		NodeList nodes = this.xpath2NodeList(doc, xPathString, additionalNamespaces);

		if (nodes == null || nodes.getLength() == 0) {
			System.out.println("XPath had no results:" + xPathString);
			return null;
		}

		System.out.print("XPath found results for: " + xPathString + ": ");
		System.out.println(nodes.getLength());

		int nodeCount = nodes.getLength();

		for (int i = 0; i < nodeCount; i++) {
			if (i > 0) {
				b.append("\n");
			}
			Node curNode = nodes.item(i);
			b.append(this.dom2String(curNode));

		}

		return b.toString();
	}

	/**
	 * Creates a String out of the attributes
	 * 
	 * @param attributes
	 * @return
	 */
	private String getAttributeString(NamedNodeMap attributes) {
		StringBuffer b = new StringBuffer();
		int max = attributes.getLength();
		if (max == 0) {
			return "";
		}
		b.append(" (");
		for (int i = 0; i < max; i++) {
			Node curNode = attributes.item(i);
			String curName = curNode.getNodeName();
			String curValue = curNode.getNodeValue();
			b.append(curName);
			b.append("=");
			b.append(curValue);
			b.append(", ");
		}
		b.append(")");
		return b.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone() We don't allow cloning here
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * Removes empty lines from Strings. Comes in handy after XSLT
	 * Transformations
	 * 
	 * @param inString
	 * @return
	 */
	private String stripEmptyLines(String inString) {
		StringBuffer b = new StringBuffer();

		Scanner lineScanner = new Scanner(inString);

		while (lineScanner.hasNextLine()) {
			String curLine = lineScanner.nextLine().trim();
			if (!curLine.equals("") && !curLine.replace("\n", "").equals("")) {
				b.append(curLine);
				b.append("\n");
			}
		}

		return b.toString();
	}
}

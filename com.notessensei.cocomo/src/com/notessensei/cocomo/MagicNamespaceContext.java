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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * @author stw
 * 
 */
public class MagicNamespaceContext implements NamespaceContext {

	/**
	 * Holds all Namespaces by prefix. A prefix can be bound to ONE namespace
	 */
	private final Map<String, String>		nameSpacesByPrefix	= new HashMap<String, String>();

	/**
	 * Hold the Namespaces by Namespace URI. A URI can be bound to multiple
	 * prefixes
	 */
	private final Map<String, Set<String>>	nameSpacesByURI		= new HashMap<String, Set<String>>();

	/**
	 * Creates a default namespace object with d and dxl assigned to DXL
	 * namespace
	 */
	public MagicNamespaceContext() {
		// We add the default namespaces for XML and attributes
		this.addNamespace(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
		this.addNamespace(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);

		// We make d / dxl namespaces for Domino/DXL
		this.addNamespace("", "http://www.lotus.com/dxl");
		this.addNamespace("d", "http://www.lotus.com/dxl");
		this.addNamespace("dxl", "http://www.lotus.com/dxl");

		// The XML Namespace and the Attribute namespace
	}

	/**
	 * Adds a namespace to the context. If the context doesn't exist
	 * 
	 * @param prefix
	 * @param uri
	 */
	public synchronized void addNamespace(String prefix, String uri) {
		// Put it in the first map
		this.nameSpacesByPrefix.put(prefix, uri);

		// Check if we have that URL already
		if (this.nameSpacesByURI.containsKey(uri)) {
			// Add to the list
			Set<String> set = this.nameSpacesByURI.get(uri);
			if (!set.contains(prefix)) {
				set.add(prefix);
			}
		} else {
			// Create a new list
			Set<String> set = new HashSet<String>();
			set.add(prefix);
			this.nameSpacesByURI.put(uri, set);
		}

	}

	/**
	 * Adds namespaces in bulk
	 * 
	 * @param newNamespaces
	 *            Map with prefix/uri pairs
	 */
	public synchronized void addNamespaces(Map<String, String> newNamespaces) {

		for (String prefix : newNamespaces.keySet()) {
			this.addNamespace(prefix, newNamespaces.get(prefix));
		}

	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see javax.xml.namespace.NamespaceContext#getNamespaceURI(java.lang.String)
	 */
	public String getNamespaceURI(String prefix) {
		if (this.nameSpacesByPrefix.containsKey(prefix)) {
			return this.nameSpacesByPrefix.get(prefix);
		}
		// Not found so we return the not-found namespace
		return XMLConstants.NULL_NS_URI;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see javax.xml.namespace.NamespaceContext#getPrefix(java.lang.String)
	 */
	public String getPrefix(String namespaceURI) {
		// returns the first prefix
		return this.getPrefixes(namespaceURI).next();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see javax.xml.namespace.NamespaceContext#getPrefixes(java.lang.String)
	 */
	public Iterator<String> getPrefixes(String namespaceURI) {
		// Returns all prefixes as iterator
		Set<String> set = this.nameSpacesByURI.get(namespaceURI);

		if (set == null) {
			return null;
		}

		return set.iterator();
	}
}

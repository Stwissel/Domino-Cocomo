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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ArtefactResult {

	private final Map<String, Integer>	scanresults	= new TreeMap<String, Integer>();

	public int add(String key) {
		return this.add(key, 1);
	}

	/**
	 * Adds the findings to our result list
	 * 
	 * @param key
	 *            - the result item
	 * @param value
	 *            - the numbers found (typically 1 but can be more for line of
	 *            code analysis)
	 * @return
	 */
	public int add(String key, int value) {
		int result = this.scanresults.containsKey(key) ? this.scanresults.get(key).intValue() + value : value;
		this.scanresults.put(key, new Integer(result));
		return result;
	}

	/**
	 * Returns the list of results based on the collection adds a zero if none
	 * of the values is available. Aggregates multiple tags into something easy
	 * to read
	 * 
	 * @param whichones
	 * @return
	 */
	public String getResults(Map<String, Set<String>> whichones) {
		if (this.scanresults.isEmpty()) {
			return "{EMPTY}";
		}
		int grandTotal = 0;
		StringBuffer b = new StringBuffer();
		boolean first = true;

		for (Map.Entry<String, Set<String>> me : whichones.entrySet()) {
			int total = 0;
			for (String which : me.getValue()) {
				if (this.scanresults.containsKey(which)) {
					total += this.scanresults.get(which).intValue();
				}
			}
			if (!first) {
				b.append(",");
			}
			first = false;
			b.append(total);
			grandTotal += total;
		}

		// Write out the sum of all
		b.append(",");
		b.append(grandTotal);
		return b.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (this.scanresults.isEmpty()) {
			return "{EMPTY}";
		}
		StringBuffer b = new StringBuffer();

		for (Map.Entry<String, Integer> me : this.scanresults.entrySet()) {
			b.append(me.getKey());
			b.append("=");
			b.append(me.getValue().toString());
			b.append("\n");
		}

		return b.toString();
	}

}

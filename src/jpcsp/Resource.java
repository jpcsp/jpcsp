/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Resource {
	/**
	 * Ordered list of the inserted resource bundles.
	 */
	protected static LinkedList<ResourceBundle> bundles = new LinkedList<ResourceBundle>();
	protected static LinkedList<ResourceBundle> bundlesEnglish = new LinkedList<ResourceBundle>();

	/**
	 * Returns the bundles.
	 *
	 * @return Returns the bundles.
	 */
	public LinkedList<ResourceBundle> getBundles() {
        return bundles;
	}

	/**
	 * Adds a resource bundle.
	 *
	 * @param basename
	 *            The basename of the resource bundle to add.
	 */
	private static void add(List<ResourceBundle> bundles, String basename) {
		bundles.add(ResourceBundle.getBundle(basename, Locale.ROOT));
	}

	/**
	 * Sets the resource bundles related to a language.
	 *
	 * @param language
	 *            The language (e.g. "en_EN") to be used.
	 */
	public static void setLanguage(String language) {
		bundles.clear();

		// Add language dependent resources
		add(bundles, "jpcsp.languages." + language);

		// Add language independent resources
		add(bundles, "jpcsp.languages.common");

		// Bundles always in English
		bundlesEnglish.clear();
		add(bundlesEnglish, "jpcsp.languages.en_EN");
		add(bundlesEnglish, "jpcsp.languages.common");
	}

	/**
	 * Gets a string from from the current resource bundle.
	 *
	 * @param key
	 *            The key string to locate.
     * @return The matching string.
	 */
	public static String get(String key) {
		return getResource(bundles, key);
	}

	/**
	 * Gets a string from from the English resource bundle.
	 *
	 * @param key
	 *            The key string to locate.
     * @return The matching string, always in English.
	 */
	public static String getEnglish(String key) {
		return getResource(bundlesEnglish, key);
	}

	/**
	 * Returns the value for <code>key</code> by searching the resource
	 * bundles in inverse order or <code>null</code> if no value can be found
	 * for <code>key</code>.
	 */
	protected static String getResource(List<ResourceBundle> bundles, String key) {
		for (ResourceBundle bundle : bundles) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException e) {
				// Ignore exception, skip to the next resource bundle
            }
		}

		return null;
	}
}
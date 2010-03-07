package jpcsp;

import java.util.Iterator;
import java.util.LinkedList;
//import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Resource {
	/**
	 * Ordered list of the inserted resource bundles.
	 */
	protected static LinkedList<ResourceBundle> bundles = new LinkedList<ResourceBundle>();

	/**
	 * Returns the bundles.
	 * 
	 * @return Returns the bundles.
	 */
	public LinkedList<ResourceBundle> getBundles()
	{
		return bundles;
	}
	/**
	 * Adds a resource bundle.
	 * 
	 * @param basename
	 *            The basename of the resource bundle to add.
	 */
	public static void add(String basename)
	{
		bundles = new LinkedList<ResourceBundle>();
		bundles.addFirst(PropertyResourceBundle.getBundle(basename));
	}

	/**
	 * 
	 */
	public static String get(String key)
	{
		return get(key, null);
	}

	/**
	 * Returns the value for the specified resource key.
	 */
	public static String get(String key, String[] params)
	{
		String value = getResource(key);

		// Replaces the placeholders with the values in the array
		if (value != null && params != null)
		{
			StringBuffer result = new StringBuffer();
			String index = null;

			for (int i = 0; i < value.length(); i++)
			{
				char c = value.charAt(i);

				if (c == '{') index = "";
				else if (index != null && c == '}')
				{
					int tmp = Integer.parseInt(index) - 1;

					if (tmp >= 0 && tmp < params.length) result.append(params[tmp]);

					index = null;
				}
				else if (index != null) index += c;
				else result.append(c);
			}

			value = result.toString();
		}

		return value;
	}

	/**
	 * Returns the value for <code>key</code> by searching the resource
	 * bundles in inverse order or <code>null</code> if no value can be found
	 * for <code>key</code>.
	 */
	protected static String getResource(String key)
	{
		Iterator<ResourceBundle> it = bundles.iterator();

		while (it.hasNext())
		{
			try
			{ return it.next().getString(key); }
			catch (MissingResourceException mrex)
			{ /* continue */ }
		}

		return null;
	}
}

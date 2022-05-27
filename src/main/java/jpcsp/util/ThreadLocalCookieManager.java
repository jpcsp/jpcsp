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
package jpcsp.util;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class ThreadLocalCookieManager extends CookieManager {
	private static final ThreadLocal<CookieManager> delegate = new ThreadLocal<CookieManager>();

	@Override
	public void setCookiePolicy(CookiePolicy cookiePolicy) {
		CookieManager cookieManager = delegate.get();
		if (cookieManager != null) {
			cookieManager.setCookiePolicy(cookiePolicy);
		} else {
			super.setCookiePolicy(cookiePolicy);
		}
	}

	@Override
	public CookieStore getCookieStore() {
		CookieManager cookieManager = delegate.get();
		if (cookieManager != null) {
			return cookieManager.getCookieStore();
		}
		return super.getCookieStore();
	}

	@Override
	public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
		CookieManager cookieManager = delegate.get();
		if (cookieManager != null) {
			return cookieManager.get(uri, requestHeaders);
		}
		return super.get(uri, requestHeaders);
	}

	@Override
	public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
		CookieManager cookieManager = delegate.get();
		if (cookieManager != null) {
			cookieManager.put(uri, responseHeaders);
		} else {
			super.put(uri, responseHeaders);
		}
	}

	public static void setCookieManager(CookieManager cookieManager) {
		delegate.set(cookieManager);
	}
}

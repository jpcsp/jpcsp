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
package jpcsp.HLE.modules;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpcsp.Memory;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer64;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.HLE.Modules;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.remote.HTTPConfiguration;
import jpcsp.remote.HTTPConfiguration.HttpServerConfiguration;
import jpcsp.remote.HTTPServer;
import jpcsp.util.ThreadLocalCookieManager;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceHttp extends HLEModule {
    public static Logger log = Modules.getLogger("sceHttp");
    public static final int PSP_HTTP_SYSTEM_COOKIE_HEAP_SIZE = 130 * 1024;
    private boolean isHttpInit;
    private boolean isSystemCookieLoaded;
    private int maxMemSize;
    private SysMemInfo memInfo;
    protected HashMap<Integer, HttpTemplate> httpTemplates = new HashMap<Integer, HttpTemplate>();
    protected HashMap<Integer, HttpConnection> httpConnections = new HashMap<Integer, HttpConnection>();
    protected HashMap<Integer, HttpRequest> httpRequests = new HashMap<Integer, HttpRequest>();
    private CookieManager cookieManager;
    private static final String httpMethods[] = {
    	"GET",
    	"POST",
    	"HEAD",
    	"OPTIONS",
    	"PUT",
    	"DELETE",
    	"TRACE",
    	"CONNECT"
    };

    protected static class HttpRequest {
    	private static final String uidPurpose = "sceHttp-HttpRequest";
    	private int id;
    	private String url;
    	private String path;
    	private int method;
    	private long contentLength;
    	private HttpConnection httpConnection;
    	private URLConnection urlConnection;
    	private HttpURLConnection httpUrlConnection;
    	private HashMap<String, String> headers = new HashMap<String, String>();
    	private byte[] sendData;
    	private int sendDataLength;

    	public HttpRequest() {
    		id = SceUidManager.getNewUid(uidPurpose);
    		Modules.sceHttpModule.httpRequests.put(id, this);
    	}

    	public void delete() {
    		Modules.sceHttpModule.httpRequests.remove(id);
    		SceUidManager.releaseUid(id, uidPurpose);
    		id = -1;
    	}

    	public int getId() {
    		return id;
    	}

    	public String getUrl() {
			if (url != null) {
				return url;
			}
			if (path != null) {
				if (path.startsWith("http:") || path.startsWith("https:")) {
					return path;
				}
				return getHttpConnection().getUrl() + path;
			}

			return null;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public int getMethod() {
			return method;
		}

		public void setMethod(int method) {
			this.method = method;
		}

		public long getContentLength() {
			return contentLength;
		}

		public void setContentLength(long contentLength) {
			this.contentLength = contentLength;
		}

		public HttpConnection getHttpConnection() {
			return httpConnection;
		}

		public void setHttpConnection(HttpConnection httpConnection) {
			this.httpConnection = httpConnection;
		}

		public void send(int data, int dataSize) {
			if (dataSize > 0) {
				sendData = Utilities.extendArray(sendData, dataSize);
				Utilities.readBytes(data, dataSize, sendData, sendDataLength);
				sendDataLength += dataSize;
			}
		}

		public void connect() {
			if (urlConnection != null) {
				// Already connected
				return;
			}

	    	ThreadLocalCookieManager.setCookieManager(Modules.sceHttpModule.cookieManager);

			if (log.isTraceEnabled()) {
				log.trace(String.format("HttpRequest %s send: %s", this, Utilities.getMemoryDump(sendData, 0, sendDataLength)));
			}

			String sendUrl = getUrl();
			Proxy proxy = getProxyForUrl(sendUrl);

			// Replace https with http when using a proxy
			if (proxy != null) {
				if (sendUrl.startsWith("https:")) {
					sendUrl = "http:" + sendUrl.substring(6);
				}
			}

			try {
				if (proxy != null) {
					urlConnection = new URL(sendUrl).openConnection(proxy);
				} else {
					urlConnection = new URL(sendUrl).openConnection();
				}

				String agent = getHttpConnection().getHttpTemplate().getAgent();
				if (agent != null) {
					if (log.isTraceEnabled()) {
						log.trace((String.format("Adding header '%s': '%s'", "User-Agent", agent)));
					}
					urlConnection.setRequestProperty("User-Agent", agent);
				}

				for (String header : headers.keySet()) {
					if (log.isTraceEnabled()) {
						log.trace(String.format("Adding header '%s': '%s'", header, headers.get(header)));
					}
					urlConnection.setRequestProperty(header, headers.get(header));
				}

				if (urlConnection instanceof HttpURLConnection) {
					httpUrlConnection = (HttpURLConnection) urlConnection;
					httpUrlConnection.setRequestMethod(httpMethods[method]);
					httpUrlConnection.setInstanceFollowRedirects(getHttpConnection().isEnableRedirect());
					if (sendDataLength > 0) {
						httpUrlConnection.setDoOutput(true);
						OutputStream os = httpUrlConnection.getOutputStream();
						os.write(sendData, 0, sendDataLength);
						os.close();
					}
				} else {
					httpUrlConnection = null;
				}
				urlConnection.connect();
				setContentLength(urlConnection.getContentLength());
			} catch (MalformedURLException e) {
				log.error("HttpRequest.send", e);
			} catch (IOException e) {
				log.error("HttpRequest.send", e);
			}
		}

		public int readData(int data, int dataSize) {
			byte buffer[] = new byte[dataSize];
			int bufferLength = 0;

			try {
				while (bufferLength < dataSize) {
					int readSize = urlConnection.getInputStream().read(buffer, bufferLength, dataSize - bufferLength);
					if (readSize < 0) {
						break;
					}
					bufferLength += readSize;
				}
			} catch (FileNotFoundException e) {
				log.debug("HttpRequest.readData", e);
			} catch (IOException e) {
				log.error("HttpRequest.readData", e);
			}

			if (bufferLength > 0) {
				IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(data, bufferLength, 1);
				for (int i = 0; i < bufferLength; i++) {
					memoryWriter.writeNext(buffer[i] & 0xFF);
				}
				memoryWriter.flush();
			}

			return bufferLength;
		}

		public String getAllHeaders() {
			if (urlConnection == null) {
				return null;
			}

			StringBuilder allHeaders = new StringBuilder();
			Map<String, List<String>> properties = urlConnection.getHeaderFields();
			for (String key : properties.keySet()) {
				if (key != null) {
					List<String> values = properties.get(key);
					for (String value : values) {
						allHeaders.append(String.format("%s: %s\r\n", key, value));
					}
				}
			}

			return allHeaders.toString();
		}

		public int getStatusCode() {
			int statusCode = 0;

			if (httpUrlConnection != null) {
				try {
					statusCode = httpUrlConnection.getResponseCode();
				} catch (IOException e) {
					log.error("HttpRequest.getStatusCode", e);
				}
			}

			return statusCode;
		}

		private void addHeader(String name, String value) {
			headers.put(name, value);
		}

		@Override
		public String toString() {
			return String.format("HttpRequest id=%d, url='%s', method=%d, contentLength=%d", getId(), getUrl(), getMethod(), contentLength);
		}
    }

    protected static class HttpConnection {
    	private static final String uidPurpose = "sceHttp-HttpConnection";
    	private int id;
    	private HashMap<Integer, HttpRequest> httpRequests = new HashMap<Integer, sceHttp.HttpRequest>();
    	private String url;
    	private HttpTemplate httpTemplate;
    	private boolean enableRedirect;

    	public HttpConnection() {
    		id = SceUidManager.getNewUid(uidPurpose);
    		Modules.sceHttpModule.httpConnections.put(id, this);
    	}

    	public void delete() {
    		// Delete all the HttpRequests
    		for (HttpRequest httpRequest : httpRequests.values()) {
    			httpRequest.delete();
    		}
    		httpRequests.clear();

    		Modules.sceHttpModule.httpConnections.remove(id);
    		SceUidManager.releaseUid(id, uidPurpose);
    		id = -1;
    	}

    	public void addHttpRequest(HttpRequest httpRequest) {
    		httpRequest.setHttpConnection(this);
    		httpRequests.put(httpRequest.getId(), httpRequest);
    	}

    	public int getId() {
    		return id;
    	}

    	public String getUrl() {
    		return url;
    	}

		public void setUrl(String url) {
			this.url = url;
		}

		public int getDefaultPort(String protocol) {
			if ("http".equals(protocol)) {
				return 80;
			}
			if ("https".equals(protocol)) {
				return 443;
			}

			return -1;
		}

		public void setUrl(String host, String protocol, int port) {
			url = String.format("%s://%s", protocol, host);
			if (port != getDefaultPort(protocol)) {
				url += String.format(":%s", port);
			}
		}

		public HttpTemplate getHttpTemplate() {
			return httpTemplate;
		}

		public void setHttpTemplate(HttpTemplate httpTemplate) {
			this.httpTemplate = httpTemplate;
		}

		public boolean isEnableRedirect() {
			return enableRedirect;
		}

		public void setEnableRedirect(boolean enableRedirect) {
			this.enableRedirect = enableRedirect;
		}

		@Override
		public String toString() {
			return String.format("HttpConnection id=%d, url='%s'", getId(), getUrl());
		}
    }

    protected static class HttpTemplate {
    	private static final String uidPurpose = "sceHttp-HttpTemplate";
    	private int id;
    	private HashMap<Integer, HttpConnection> httpConnections = new HashMap<Integer, sceHttp.HttpConnection>();
    	private String agent;
    	private boolean enableRedirect;

    	public HttpTemplate() {
    		id = SceUidManager.getNewUid(uidPurpose);
    		Modules.sceHttpModule.httpTemplates.put(id, this);
    	}

    	public void delete() {
    		// Delete all the HttpConnections
    		for (HttpConnection httpConnection : httpConnections.values()) {
    			httpConnection.delete();
    		}
    		httpConnections.clear();

    		Modules.sceHttpModule.httpTemplates.remove(id);
    		SceUidManager.releaseUid(id, uidPurpose);
    		id = -1;
    	}

    	public void addHttpConnection(HttpConnection httpConnection) {
    		httpConnection.setHttpTemplate(this);
    		httpConnection.setEnableRedirect(isEnableRedirect());
    		httpConnections.put(httpConnection.getId(), httpConnection);
    	}

    	public int getId() {
    		return id;
    	}

		public String getAgent() {
			return agent;
		}

		public void setAgent(String agent) {
			this.agent = agent;
		}

		public boolean isEnableRedirect() {
			return enableRedirect;
		}

		public void setEnableRedirect(boolean enableRedirect) {
			this.enableRedirect = enableRedirect;
		}

		@Override
		public String toString() {
			return String.format("HttpTemplate id=%d, agent='%s'", getId(), getAgent());
		}
    }

	@Override
	public void start() {
		CookieHandler.setDefault(new ThreadLocalCookieManager());
		cookieManager = new CookieManager();

		super.start();
	}

	public void checkHttpInit() {
    	if (!isHttpInit) {
    		throw(new SceKernelErrorException(SceKernelErrors.ERROR_HTTP_NOT_INIT));
    	}
    }

	private static Proxy getProxyForUrl(String url) {
		for (HttpServerConfiguration httpServerConfiguration : HTTPConfiguration.doProxyServers) {
			if (httpServerConfiguration.isMatchingUrl(url)) {
				return HTTPServer.getInstance().getProxy();
			}
		}

		return null;
	}

	public static String patchUrl(String url) {
		for (HttpServerConfiguration httpServerConfiguration : HTTPConfiguration.doProxyServers) {
			if (httpServerConfiguration.isHttps()) {
				if (httpServerConfiguration.isMatchingUrl(url)) {
					// Replace https with http
					return url.replaceFirst("https", "http");
				}
			}
		}

		return url;
	}

	protected HttpRequest getHttpRequest(int requestId) {
    	HttpRequest httpRequest = httpRequests.get(requestId);
    	if (httpRequest == null) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_INVALID_ARGUMENT);
    	}

    	return httpRequest;
    }

    protected HttpConnection getHttpConnection(int connectionId) {
    	HttpConnection httpConnection = httpConnections.get(connectionId);
    	if (httpConnection == null) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_INVALID_ARGUMENT);
    	}

    	return httpConnection;
    }

    protected boolean isHttpTemplateId(int templateId) {
    	return httpTemplates.containsKey(templateId);
    }

    protected HttpTemplate getHttpTemplate(int templateId) {
    	HttpTemplate httpTemplate = httpTemplates.get(templateId);
    	if (httpTemplate == null) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_INVALID_ARGUMENT);
    	}

    	return httpTemplate;
    }

    private int getTempMemory() {
    	if (memInfo == null) {
    		memInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.VSHELL_PARTITION_ID, "sceHttp", SysMemUserForUser.PSP_SMEM_Low, maxMemSize, 0);
    		if (memInfo == null) {
    			return 0;
    		}
    	}

    	return memInfo.addr;
    }

    /**
     * Init the http library.
     *
     * @param heapSize - Memory pool size? Pass 20000
     * @return 0 on success, < 0 on error.
     */
    @HLELogging(level="info")
    @HLEFunction(nid = 0xAB1ABE07, version = 150, checkInsideInterrupt = true)
    public int sceHttpInit(int heapSize) {
        if (isHttpInit) {
            return SceKernelErrors.ERROR_HTTP_ALREADY_INIT;
        }

        maxMemSize = heapSize;
        isHttpInit = true;
        memInfo = null;

        // Allocate memory during sceHttpInit
        int addr = getTempMemory();
        if (addr == 0) {
        	log.warn(String.format("sceHttpInit cannot allocate 0x%X bytes", maxMemSize));
        	return -1;
        }

		Utilities.disableSslCertificateChecks();

        return 0;
    }

    /**
     * Terminate the http library.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xD1C8945E, version = 150, checkInsideInterrupt = true)
    public int sceHttpEnd() {
        checkHttpInit();

        isSystemCookieLoaded = false;
        isHttpInit = false;

        if (memInfo != null) {
        	Modules.SysMemUserForUserModule.free(memInfo);
        	memInfo = null;
        }

        return 0;
    }

    /**
     * Get http request response length.
     *
     * @param requestid - ID of the request created by sceHttpCreateRequest or sceHttpCreateRequestWithURL
     * @param contentlength - The size of the content
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x0282A3BD, version = 150)
    public int sceHttpGetContentLength(int requestId, @BufferInfo(usage=Usage.out) TPointer64 contentLengthAddr){
    	HttpRequest httpRequest = getHttpRequest(requestId);
    	httpRequest.connect();
    	long contentLength = httpRequest.getContentLength();

    	int result;
    	if (contentLength < 0) {
    		// Value in contentLengthAddr is left unchanged when returning an error, checked on PSP.
    		result = SceKernelErrors.ERROR_HTTP_NO_CONTENT_LENGTH;
    	} else {
    		contentLengthAddr.setValue(contentLength);
    		result = 0;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceHttpGetContentLength request %s returning 0x%X, contentLength=0x%X", httpRequest, result, contentLengthAddr.getValue()));
    	}

    	return result;
    }

    /**
     * Set resolver retry
     *
     * @param id - ID of the template or connection 
     * @param count - Number of retries
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x03D9526F, version = 150)
    public int sceHttpSetResolveRetry(int templateId, int count) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x06488A1C, version = 150)
    public int sceHttpSetCookieSendCallback() {
        return 0;
    }

    /**
     * Enable redirect
     *
     * @param id - ID of the template or connection 
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x0809C831, version = 150)
    public int sceHttpEnableRedirect(int id) {
    	if (isHttpTemplateId(id)) {
    		HttpTemplate httpTemplate = getHttpTemplate(id);
    		httpTemplate.setEnableRedirect(true);
    	} else {
    		HttpConnection httpConnection = getHttpConnection(id);
    		httpConnection.setEnableRedirect(true);
    	}

        return 0;
    }

    /**
     * Disable cookie
     *
     * @param id - ID of the template or connection 
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x0B12ABFB, version = 150)
    public int sceHttpDisableCookie(int templateId) {
    	return 0;
    }

    /**
     * Enable cookie
     *
     * @param id - ID of the template or connection 
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x0DAFA58F, version = 150)
    public int sceHttpEnableCookie(int templateId) {
        return 0;
    }

    /**
     * Delete content header
     *
     * @param id - ID of the template, connection or request 
     * @param name - Name of the content
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x15540184, version = 150)
    public int sceHttpDeleteHeader(int templateId, int name) {
        return 0;
    }

    /**
     * Disable redirect
     *
     * @param id - ID of the template or connection 
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x1A0EBB69, version = 150)
    public int sceHttpDisableRedirect(int id) {
    	if (isHttpTemplateId(id)) {
    		HttpTemplate httpTemplate = getHttpTemplate(id);
    		httpTemplate.setEnableRedirect(false);
    	} else {
    		HttpConnection httpConnection = getHttpConnection(id);
    		httpConnection.setEnableRedirect(false);
    	}

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1CEDB9D4, version = 150)
    public int sceHttpFlushCache() {
        return 0;
    }

    /**
     * Set receive timeout
     *
     * @param id - ID of the template or connection 
     * @param timeout - Timeout value in microseconds
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x1F0FC3E3, version = 150)
    public int sceHttpSetRecvTimeOut(int templateId, int timeout) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2255551E, version = 150)
    public int sceHttpGetNetworkPspError(int connectionId, TPointer32 errorAddr) {
    	errorAddr.setValue(0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x267618F4, version = 150)
    public int sceHttpSetAuthInfoCallback(int templateId, TPointer callback, int callbackArg) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2A6C3296, version = 150)
    public int sceHttpSetAuthInfoCB() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2C3C82CF, version = 150)
    public int sceHttpFlushAuthList() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3A67F306, version = 150)
    public int sceHttpSetCookieRecvCallback() {
        return 0;
    }

    /**
     * Add content header
     *
     * @param id - ID of the template, connection or request 
     * @param name - Name of the content
     * @param value - Value of the content
     * @param unknown1 - Pass 0
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x3EABA285, version = 150)
    public int sceHttpAddExtraHeader(int requestId, PspString name, PspString value, int unknown1) {
    	HttpRequest httpRequest = getHttpRequest(requestId);
    	httpRequest.addHeader(name.getString(), value.getString());

    	return 0;
    }

    /**
     * Create a http request.
     *
     * @param connectionid - ID of the connection created by sceHttpCreateConnection or sceHttpCreateConnectionWithURL
     * @param method - One of ::PspHttpMethod
     * @param path - Path to access
     * @param contentlength - Length of the content (POST method only)
     * @return A request ID on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x47347B50, version = 150)
    public int sceHttpCreateRequest(int connectionId, int method, PspString path, long contentLength) {
    	HttpConnection httpConnection = getHttpConnection(connectionId);
    	HttpRequest httpRequest = new HttpRequest();
    	httpRequest.setMethod(method);
    	httpRequest.setPath(path.getString());
    	httpRequest.setContentLength(contentLength);
    	httpConnection.addHttpRequest(httpRequest);

    	return httpRequest.getId();
    }

    /**
     * Set resolver timeout
     *
     * @param id - ID of the template or connection 
     * @param timeout - Timeout value in microseconds
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x47940436, version = 150)
    public int sceHttpSetResolveTimeOut(int templateId, int timeout) {
    	return 0;
    }

    /**
     * Get http request status code.
     *
     * @param requestid - ID of the request created by sceHttpCreateRequest or sceHttpCreateRequestWithURL
     * @param statuscode - The status code from the host (200 is ok, 404 is not found etc)
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x4CC7D78F, version = 150)
    public int sceHttpGetStatusCode(int requestId, @BufferInfo(usage=Usage.out) TPointer32 statusCode) {
    	HttpRequest httpRequest = getHttpRequest(requestId);
    	httpRequest.connect();
    	statusCode.setValue(httpRequest.getStatusCode());

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceHttpGetStatusCode on request %s returning statusCode=%d", httpRequest, statusCode.getValue()));
    	}

    	return 0;
    }

    /**
     * Delete a http connection.
     *
     * @param connectionid - ID of the connection created by sceHttpCreateConnection or sceHttpCreateConnectionWithURL
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x5152773B, version = 150)
    public int sceHttpDeleteConnection(int connectionId) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x54E7DF75, version = 150)
    public int sceHttpIsRequestInCache(int requestId, int unknown1, int unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x59E6D16F, version = 150)
    public int sceHttpEnableCache(int templateId) {
        return 0;
    }

    /**
     * Save cookie
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x76D1363B, version = 150, checkInsideInterrupt = true)
    public int sceHttpSaveSystemCookie() {
        checkHttpInit();

        if (!isSystemCookieLoaded){
            return SceKernelErrors.ERROR_HTTP_SYSTEM_COOKIE_NOT_LOADED;
        }
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7774BF4C, version = 150)
    public int sceHttpAddCookie(PspString url, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer cookieAddr, int length) {
    	String cookie = cookieAddr.getStringNZ(length);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceHttpAddCookie for URL '%s': '%s'", url.getString(), cookie));
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x77EE5319, version = 150)
    public int sceHttpLoadAuthList() {
        return 0;
    }

    /**
     * Enable keep alive
     *
     * @param id - ID of the template or connection 
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x78A0D3EC, version = 150)
    public int sceHttpEnableKeepAlive(int templateId) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x78B54C09, version = 150)
    public int sceHttpEndCache() {
        return 0;
    }

    /**
     * Set connect timeout
     *
     * @param id - ID of the template, connection or request 
     * @param timeout - Timeout value in microseconds
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x8ACD1F73, version = 150)
    public int sceHttpSetConnectTimeOut(int templateId, int timeout) {
    	return 0;
    }

    /**
     * Create a http connection.
     *
     * @param templateid - ID of the template created by sceHttpCreateTemplate
     * @param host - Host to connect to
     * @param protocol - Pass "http"
     * @param port - Port to connect on
     * @param unknown1 - Pass 0
     * @return A connection ID on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x8EEFD953, version = 150)
    public int sceHttpCreateConnection(int templateId, PspString host, PspString protocol, int port, int unknown1) {
    	HttpTemplate httpTemplate = getHttpTemplate(templateId);
    	HttpConnection httpConnection = new HttpConnection();
    	httpConnection.setUrl(host.getString(), protocol.getString(), port);
    	httpTemplate.addHttpConnection(httpConnection);

    	return httpConnection.getId();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x951D310E, version = 150)
    public int sceHttpDisableProxyAuth() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9668864C, version = 150)
    public int sceHttpSetRecvBlockSize() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x96F16D3E, version = 150)
    public int sceHttpGetCookie(PspString url, @BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.out) TPointer cookie, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 cookieLengthAddr, int prepare, int secure) {
        return SceKernelErrors.ERROR_HTTP_NOT_FOUND;
    }

    /**
     * Set send timeout
     *
     * @param id - ID of the template, connection or request 
     * @param timeout - Timeout value in microseconds
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x9988172D, version = 150)
    public int sceHttpSetSendTimeOut(int templateId, int timeout) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9AFC98B2, version = 150)
    public int sceHttpSendRequestInCacheFirstMode() {
        return 0;
    }

    /**
     * Create a http template.
     *
     * @param agent - User agent
     * @param unknown1 - Pass 1
     * @param unknown2 - Pass 0
     * @return A template ID on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x9B1F1F36, version = 150)
    public int sceHttpCreateTemplate(PspString agent, int unknown1, int unknown2) {
    	HttpTemplate httpTemplate = new HttpTemplate();
    	httpTemplate.setAgent(agent.getString());

    	return httpTemplate.getId();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9FC5F10D, version = 150)
    public int sceHttpEnableAuth(int templateId) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA4496DE5, version = 150)
    public int sceHttpSetRedirectCallback(int templateId, @CanBeNull TPointer callbackAddr, int callbackArg) {
        return 0;
    }

    /**
     * Delete a http request.
     *
     * @param requestid - ID of the request created by sceHttpCreateRequest or sceHttpCreateRequestWithURL
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xA5512E01, version = 150)
    public int sceHttpDeleteRequest(int requestId) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA6800C34, version = 150)
    public int sceHttpInitCache() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAE948FEE, version = 150)
    public int sceHttpDisableAuth(int templateId) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB0C34B1D, version = 150)
    public int sceHttpSetCacheContentLengthMaxSize() {
        return 0;
    }

    /**
     * Create a http request with url.
     *
     * @param connectionid - ID of the connection created by sceHttpCreateConnection or sceHttpCreateConnectionWithURL
     * @param method - One of ::PspHttpMethod
     * @param url - url to access
     * @param contentlength - Length of the content (POST method only)
     * @return A request ID on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xB509B09E, version = 150)
    public int sceHttpCreateRequestWithURL(int connectionId, int method, PspString url, long contentLength) {
    	HttpConnection httpConnection = getHttpConnection(connectionId);
    	HttpRequest httpRequest = new HttpRequest();
    	httpRequest.setMethod(method);
    	httpRequest.setUrl(url.getString());
    	httpRequest.setContentLength(contentLength);
    	httpConnection.addHttpRequest(httpRequest);

    	return httpRequest.getId();
    }

    /**
     * Send a http request.
     *
     * @param requestid - ID of the request created by sceHttpCreateRequest or sceHttpCreateRequestWithURL
     * @param data - For POST methods specify a pointer to the post data, otherwise pass NULL
     * @param datasize - For POST methods specify the size of the post data, otherwise pass 0
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xBB70706F, version = 150)
    public int sceHttpSendRequest(int requestId, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer data, int dataSize) {
    	HttpRequest httpRequest = getHttpRequest(requestId);
    	httpRequest.send(data.getAddress(), dataSize);

    	return 0;
    }

    /**
     * Abort a http request.
     *
     * @param requestid - ID of the request created by sceHttpCreateRequest or sceHttpCreateRequestWithURL
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xC10B6BD9, version = 150)
    public int sceHttpAbortRequest(int requestId) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC6330B0D, version = 150)
    public int sceHttpChangeHttpVersion() {
        return 0;
    }

    /**
     * Disable keep alive
     *
     * @param id - ID of the template or connection 
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xC7EF2559, version = 150)
    public int sceHttpDisableKeepAlive(int templateId) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC98CBBA7, version = 150)
    public int sceHttpSetResHeaderMaxSize() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCCBD167A, version = 150)
    public int sceHttpDisableCache(int templateId) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCDB0DC58, version = 150)
    public int sceHttpEnableProxyAuth() {
        return 0;
    }

    /**
     * Create a http connection to a url.
     *
     * @param templateid - ID of the template created by sceHttpCreateTemplate
     * @param url - url to connect to
     * @param unknown1 - Pass 0
     * @return A connection ID on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xCDF8ECB9, version = 150)
    public int sceHttpCreateConnectionWithURL(int templateId, PspString url, int unknown1) {
    	HttpTemplate httpTemplate = getHttpTemplate(templateId);
    	HttpConnection httpConnection = new HttpConnection();
    	httpConnection.setUrl(url.getString());
    	httpTemplate.addHttpConnection(httpConnection);

    	return httpConnection.getId();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD081EC8F, version = 150)
    public int sceHttpGetNetworkErrno(int requestId, TPointer32 errno) {
    	errno.setValue(0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD70D4847, version = 150)
    public int sceHttpGetProxy() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDB266CCF, version = 150)
    public int sceHttpGetAllHeader(int requestId, @BufferInfo(usage=Usage.out) TPointer32 headerAddr, @BufferInfo(usage=Usage.out) TPointer32 headerLengthAddr) {
    	HttpRequest httpRequest = getHttpRequest(requestId);
    	httpRequest.connect();
    	String allHeaders = httpRequest.getAllHeaders();
    	if (allHeaders == null) {
    		return -1;
    	}

    	int addr = getTempMemory();
    	Utilities.writeStringZ(Memory.getInstance(), addr, allHeaders);
    	headerAddr.setValue(addr);
    	headerLengthAddr.setValue(allHeaders.length());

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceHttpGetAllHeader returning at 0x%08X: %s", addr, Utilities.getMemoryDump(addr, headerLengthAddr.getValue())));
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDD6E7857, version = 150)
    public int sceHttpSaveAuthList() {
        return 0;
    }

    /**
     * Read a http request response.
     *
     * @param requestid - ID of the request created by sceHttpCreateRequest or sceHttpCreateRequestWithURL
     * @param data - Buffer for the response data to be stored
     * @param datasize - Size of the buffer 
     * @return The size read into the data buffer, 0 if there is no more data, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xEDEEB999, version = 150)
    public int sceHttpReadData(int requestId, @BufferInfo(lengthInfo=LengthInfo.returnValue, usage=Usage.out) TPointer data, int dataSize) {
    	HttpRequest httpRequest = getHttpRequest(requestId);
    	httpRequest.connect();
    	int readSize = httpRequest.readData(data.getAddress(), dataSize);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceHttpReadData returning 0x%X: %s", readSize, Utilities.getMemoryDump(data.getAddress(), readSize)));
    	}

    	return readSize;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF0F46C62, version = 150)
    public int sceHttpSetProxy() {
        return 0;
    }

    /**
     * Load cookie
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xF1657B22, version = 150, checkInsideInterrupt = true)
    public int sceHttpLoadSystemCookie() {
        checkHttpInit();

        if (isSystemCookieLoaded) { // The system's cookie list can only be loaded once per session.
            return SceKernelErrors.ERROR_HTTP_ALREADY_INIT;
        } else if (maxMemSize < PSP_HTTP_SYSTEM_COOKIE_HEAP_SIZE){
            return SceKernelErrors.ERROR_HTTP_NO_MEMORY;
        } else {
            isSystemCookieLoaded = true;
            return 0;
        }
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF49934F6, version = 150)
    public int sceHttpSetMallocFunction(TPointer function1, TPointer function2, TPointer function3) {
        return 0;
    }

    /**
     * Delete a http template.
     *
     * @param templateid - ID of the template created by sceHttpCreateTemplate
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xFCF8C055, version = 150)
    public int sceHttpDeleteTemplate(int templateId) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x87F1E666, version = 150)
    public int sceHttp_87F1E666(int templateId, int unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3C478044, version = 150)
    public int sceHttp_3C478044(int templateId, int unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x739C2D79, version = 150)
    public int sceHttpInitExternalCache() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA461A167, version = 150)
    public int sceHttpEndExternalCache() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8046E250, version = 150)
    public int sceHttpEnableExternalCache() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB0257723, version = 150)
    public int sceHttpFlushExternalCache() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x457D221D, version = 150)
    public int sceHttpFlushCookie() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4E4A284A, version = 150)
    public int sceHttpCloneTemplate(int templateId) {
    	HttpTemplate clonedHttpTemplate = new HttpTemplate();
    	HttpTemplate httpTemplate = getHttpTemplate(templateId);
    	clonedHttpTemplate.setAgent(httpTemplate.getAgent());

    	return clonedHttpTemplate.getId();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD80BE761, version = 150)
    public int sceHttp_D80BE761(int templateId) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA909F2AE, version = 150)
    public int sceHttp_A909F2AE1() {
    	return 0;
    }
}
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
package jpcsp.HLE.modules150;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer64;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

@HLELogging
public class sceHttp extends HLEModule {
    public static Logger log = Modules.getLogger("sceHttp");
    public static final int PSP_HTTP_SYSTEM_COOKIE_HEAP_SIZE = 130 * 1024;
    private boolean isHttpInit;
    private boolean isSystemCookieLoaded;
    private int maxMemSize;
    protected HashMap<Integer, HttpTemplate> httpTemplates = new HashMap<Integer, HttpTemplate>();
    protected HashMap<Integer, HttpConnection> httpConnections = new HashMap<Integer, HttpConnection>();
    protected HashMap<Integer, HttpRequest> httpRequests = new HashMap<Integer, HttpRequest>();

    protected static class HttpRequest {
    	private static final String uidPurpose = "sceHttp-HttpRequest";
    	private int id;
    	private String url;
    	private int method;
    	private long contentLength;
    	private HttpConnection httpConnection;
    	private URLConnection urlConnection;
    	private HttpURLConnection httpUrlConnection;
    	private byte[] data;
    	private int dataOffset;
    	private int dataLength;

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
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public void setPath(String path) {
			this.url = path;
		}

		public int getMethod() {
			return method;
		}

		public void setMethod(int method) {
			this.method = method;
		}

		public long getContentLength() {
			readData();
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
			if (log.isTraceEnabled()) {
				log.trace(String.format("HttpRequest %s send: %s", this, Utilities.getMemoryDump(data, dataSize)));
			}

			try {
				urlConnection = new URL(getUrl()).openConnection();
				if (urlConnection instanceof HttpURLConnection) {
					httpUrlConnection = (HttpURLConnection) urlConnection;
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

		private void addData(byte[] buffer, int bufferLength) {
			if (dataOffset + dataLength + bufferLength > data.length) {
				byte[] newData = new byte[dataLength + bufferLength];
				System.arraycopy(data, dataOffset, newData, 0, dataLength);
				dataOffset = 0;
				data = newData;
			}
			System.arraycopy(buffer, 0, data, dataOffset + dataLength, bufferLength);
			dataLength += bufferLength;
		}

		private void readData() {
			if (urlConnection == null) {
				// Request not yet sent
				return;
			}

			if (data != null) {
				// Data already read
				return;
			}

			dataOffset = 0;
			dataLength = 0;
			data = new byte[1024];
			byte[] buffer = new byte[1024];
			try {
				while (true) {
					int readSize = urlConnection.getInputStream().read(buffer);
					if (readSize > 0) {
						addData(buffer, readSize);
					} else if (readSize < 0) {
						break;
					}
				}
			} catch (FileNotFoundException e) {
				log.debug("HttpRequest.readData", e);
			} catch (IOException e) {
				log.error("HttpRequest.readData", e);
			}

			setContentLength(dataLength);
		}

		public int readData(int data, int dataSize) {
			readData();

			int readSize = Math.min(dataSize, dataLength);
			if (readSize > 0) {
				IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(data, readSize, 1);
				for (int i = 0; i < readSize; i++) {
					memoryWriter.writeNext(this.data[dataOffset + i] & 0xFF);
				}
				memoryWriter.flush();

				dataOffset += readSize;
				dataLength -= readSize;
			}

			return readSize;
		}

		public int getStatusCode() {
			int statusCode = 0;

			readData();
			if (httpUrlConnection != null) {
				try {
					statusCode = httpUrlConnection.getResponseCode();
				} catch (IOException e) {
					log.error("HttpRequest.getStatusCode", e);
				}
			}

			return statusCode;
		}

		@Override
		public String toString() {
			return String.format("HttpRequest id=%d, url='%s', method=%d, contentLength=%d", getId(), getUrl(), getMethod(), getContentLength());
		}
    }

    protected static class HttpConnection {
    	private static final String uidPurpose = "sceHttp-HttpConnection";
    	private int id;
    	private HashMap<Integer, HttpRequest> httpRequests = new HashMap<Integer, sceHttp.HttpRequest>();
    	private String url;
    	private HttpTemplate httpTemplate;

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

		@Override
		public String toString() {
			return String.format("HttpTemplate id=%d, agent='%s'", getId(), getAgent());
		}
    }

    @Override
    public String getName() {
        return "sceHttp";
    }

    public void checkHttpInit() {
    	if (!isHttpInit) {
    		throw(new SceKernelErrorException(SceKernelErrors.ERROR_HTTP_NOT_INIT));
    	}
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

    protected HttpTemplate getHttpTemplate(int templateId) {
    	HttpTemplate httpTemplate = httpTemplates.get(templateId);
    	if (httpTemplate == null) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_INVALID_ARGUMENT);
    	}

    	return httpTemplate;
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
    public int sceHttpGetContentLength(int requestId, TPointer64 contentLength){
    	HttpRequest httpRequest = getHttpRequest(requestId);
    	contentLength.setValue(httpRequest.getContentLength());

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceHttpGetContentLength request %s returning contentLength=%d", httpRequest, contentLength.getValue()));
    	}

    	return 0;
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
    public int sceHttpEnableRedirect(int templateId) {
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
    public int sceHttpDisableRedirect(int templateId) {
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
    public int sceHttpSetAuthInfoCallback() {
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
    public int sceHttpAddExtraHeader(int templateId, PspString name, PspString value, int unknown1) {
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
    public int sceHttpCreateRequest(int connectionId, int method, PspString path, int contentLength) {
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
    public int sceHttpGetStatusCode(int requestId, TPointer32 statusCode) {
    	HttpRequest httpRequest = getHttpRequest(requestId);
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
    public int sceHttpIsRequestInCache() {
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
    public int sceHttpAddCookie() {
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
    public int sceHttpGetCookie() {
        return 0;
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
    public int sceHttpSetRedirectCallback(int templateId, TPointer callbackAddr, int callbackArg) {
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
    public int sceHttpSendRequest(int requestId, @CanBeNull TPointer data, int dataSize) {
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
    public int sceHttpGetAllHeader(int requestId, TPointer32 unknownAddr1, TPointer32 unknownAddr2) {
    	unknownAddr1.setValue(0);
    	unknownAddr2.setValue(0);

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
    public int sceHttpReadData(int requestId, TPointer data, int dataSize) {
    	HttpRequest httpRequest = getHttpRequest(requestId);
    	int readSize = httpRequest.readData(data.getAddress(), dataSize);

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
        } else if (maxMemSize <  PSP_HTTP_SYSTEM_COOKIE_HEAP_SIZE){
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
}
package cn.losfer.aimodelsstatus;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class HttpClient {
	public static final int MAX_BODY = 512 * 1024;

	public static class Response {
		public int code;
		public String body = "";
		public long latencyMs;
		public String error;
	}

	public static Response get(String url, String apiKey, int timeoutMs) {
		return execute("GET", url, apiKey, null, timeoutMs);
	}

	public static Response postJson(String url, String apiKey, String json, int timeoutMs) {
		return execute("POST", url, apiKey, json, timeoutMs);
	}

	private static Response execute(String method, String url, String apiKey, String body, int timeoutMs) {
		Response r = new Response();
		long t0 = System.currentTimeMillis();
		HttpURLConnection conn = null;
		try {
			URL u = new URL(url);
			conn = (HttpURLConnection) u.openConnection();
			conn.setRequestMethod(method);
			conn.setConnectTimeout(Math.max(1000, timeoutMs));
			conn.setReadTimeout(Math.max(1000, timeoutMs));
			conn.setInstanceFollowRedirects(true);
			conn.setRequestProperty("Accept", "application/json, text/plain, */*");
			conn.setRequestProperty("Accept-Encoding", "gzip");
			conn.setRequestProperty("User-Agent", "AiProbe/1.0 (Android)");
			if (apiKey != null && !apiKey.isEmpty()) {
				conn.setRequestProperty("Authorization", "Bearer " + apiKey);
				conn.setRequestProperty("api-key", apiKey);
			}
			if (body != null) {
				byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
				conn.setDoOutput(true);
				conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
				conn.setFixedLengthStreamingMode(bytes.length);
				OutputStream os = conn.getOutputStream();
				os.write(bytes);
				os.flush();
				os.close();
			}
			int code;
			try {
				code = conn.getResponseCode();
			} catch (Exception e) {
				code = -1;
				r.error = e.getMessage();
			}
			r.code = code;
			InputStream raw;
			if (code >= 200 && code < 400) {
				raw = conn.getInputStream();
			} else {
				raw = conn.getErrorStream();
				if (raw == null) {
					try { raw = conn.getInputStream(); } catch (Exception ignored) {}
				}
			}
			if (raw != null) {
				String enc = conn.getContentEncoding();
				if (enc != null && enc.toLowerCase().contains("gzip")) {
					raw = new GZIPInputStream(raw);
				}
				r.body = readLimited(raw);
			}
		} catch (Exception e) {
			r.error = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "network error" : e.getMessage());
			if (r.code == 0) r.code = -1;
		} finally {
			r.latencyMs = System.currentTimeMillis() - t0;
			if (conn != null) conn.disconnect();
		}
		return r;
	}

	private static String readLimited(InputStream in) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int n;
		int total = 0;
		try {
			while ((n = in.read(buf)) > 0) {
				int allow = Math.min(n, MAX_BODY - total);
				if (allow > 0) bos.write(buf, 0, allow);
				total += n;
				if (total >= MAX_BODY) break;
			}
		} catch (Exception ignored) {
		} finally {
			try { in.close(); } catch (Exception ignored) {}
		}
		try {
			return bos.toString("UTF-8");
		} catch (Exception e) {
			return "";
		}
	}
}

package cn.losfer.aimodelsstatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Site {
	public String id;
	public String name = "";
	public String baseUrl = "";
	public String apiKey = "";
	public int concurrency = 3;
	public int intervalMs = 200;
	public int timeoutMs = 30000;
	public int maxTokens = 8;
	public String prompt = "";
	// Active chat models, ordered exactly as the user arranged them.
	public final List<String> models = new ArrayList<String>();
	// Models excluded automatically or manually. They can be restored from the UI.
	public final List<String> removedModels = new ArrayList<String>();
	public long createdAt;
	public long lastFetchAt;
	public long lastTestAt;
	public int lastOk;
	public int lastFail;
	public long lastDurationMs;

	public static Site create() {
		Site s = new Site();
		s.id = UUID.randomUUID().toString();
		s.createdAt = System.currentTimeMillis();
		return s;
	}

	public String displayName() {
		if (name != null) {
			String n = name.trim();
			if (!n.isEmpty()) return n;
		}
		String host = host();
		return host.isEmpty() ? "未命名站点" : host;
	}

	public String host() {
		String u = baseUrl == null ? "" : baseUrl.trim();
		if (u.isEmpty()) return "";
		int scheme = u.indexOf("://");
		if (scheme >= 0) u = u.substring(scheme + 3);
		int slash = u.indexOf('/');
		if (slash >= 0) u = u.substring(0, slash);
		int at = u.indexOf('@');
		if (at >= 0) u = u.substring(at + 1);
		return u;
	}

	public String initial() {
		String n = displayName();
		if (n.isEmpty()) return "A";
		int cp = n.codePointAt(0);
		return new String(Character.toChars(cp)).toUpperCase();
	}

	public int avatarColorIndex() {
		int h = (id == null ? displayName() : id).hashCode();
		return Math.abs(h) % 6;
	}

	public String maskedKey() {
		if (apiKey == null || apiKey.isEmpty()) return "未填写 Key";
		String k = apiKey.trim();
		if (k.length() <= 8) return "••••" + k.substring(Math.max(0, k.length() - 2));
		return k.substring(0, 4) + "••••" + k.substring(k.length() - 4);
	}

	public JSONObject toJson() {
		JSONObject o = new JSONObject();
		try {
			o.put("id", id);
			o.put("name", name == null ? "" : name);
			o.put("baseUrl", baseUrl == null ? "" : baseUrl);
			o.put("apiKey", apiKey == null ? "" : apiKey);
			o.put("concurrency", concurrency);
			o.put("intervalMs", intervalMs);
			o.put("timeoutMs", timeoutMs);
			o.put("maxTokens", maxTokens);
			o.put("prompt", prompt == null ? "" : prompt);
			JSONArray arr = new JSONArray();
			for (String m : models) arr.put(m);
			o.put("models", arr);
			JSONArray removed = new JSONArray();
			for (String m : removedModels) removed.put(m);
			o.put("removedModels", removed);
			o.put("createdAt", createdAt);
			o.put("lastFetchAt", lastFetchAt);
			o.put("lastTestAt", lastTestAt);
			o.put("lastOk", lastOk);
			o.put("lastFail", lastFail);
			o.put("lastDurationMs", lastDurationMs);
		} catch (Exception ignored) {
		}
		return o;
	}

	public static Site fromJson(JSONObject o) {
		Site s = new Site();
		if (o == null) return s;
		s.id = o.optString("id", UUID.randomUUID().toString());
		s.name = o.optString("name", "");
		s.baseUrl = o.optString("baseUrl", "");
		s.apiKey = o.optString("apiKey", "");
		s.concurrency = o.optInt("concurrency", 3);
		s.intervalMs = o.optInt("intervalMs", 200);
		s.timeoutMs = o.optInt("timeoutMs", 30000);
		s.maxTokens = o.optInt("maxTokens", 8);
		s.prompt = o.optString("prompt", "");
		JSONArray arr = o.optJSONArray("models");
		if (arr != null) {
			for (int i = 0; i < arr.length(); i++) {
				String m = arr.optString(i, "");
				if (!m.isEmpty()) s.models.add(m);
			}
		}
		JSONArray removed = o.optJSONArray("removedModels");
		if (removed != null) {
			for (int i = 0; i < removed.length(); i++) {
				String m = removed.optString(i, "");
				if (!m.isEmpty() && !s.models.contains(m)) s.removedModels.add(m);
			}
		}
		s.createdAt = o.optLong("createdAt", 0);
		s.lastFetchAt = o.optLong("lastFetchAt", 0);
		s.lastTestAt = o.optLong("lastTestAt", 0);
		s.lastOk = o.optInt("lastOk", 0);
		s.lastFail = o.optInt("lastFail", 0);
		s.lastDurationMs = o.optLong("lastDurationMs", 0);
		return s;
	}
}

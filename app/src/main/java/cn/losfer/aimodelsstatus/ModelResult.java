package cn.losfer.aimodelsstatus;

import org.json.JSONObject;

public class ModelResult {
	public static final int PENDING = 0;
	public static final int RUNNING = 1;
	public static final int OK = 2;
	public static final int FAIL = 3;

	public String model;
	public int state = PENDING;
	public boolean ok;
	public int httpCode;
	public long latencyMs;
	public String reply = "";
	public String error = "";
	public int promptTokens;
	public int completionTokens;
	public int cachedTokens;
	public int totalTokens;

	public JSONObject toJson() {
		JSONObject o = new JSONObject();
		try {
			o.put("model", model);
			o.put("state", state);
			o.put("ok", ok);
			o.put("httpCode", httpCode);
			o.put("latencyMs", latencyMs);
			o.put("reply", reply == null ? "" : reply);
			o.put("error", error == null ? "" : error);
			o.put("promptTokens", promptTokens);
			o.put("completionTokens", completionTokens);
			o.put("cachedTokens", cachedTokens);
			o.put("totalTokens", totalTokens);
		} catch (Exception ignored) {
		}
		return o;
	}

	public static ModelResult fromJson(JSONObject o) {
		ModelResult r = new ModelResult();
		if (o == null) return r;
		r.model = o.optString("model", "");
		r.state = o.optInt("state", PENDING);
		r.ok = o.optBoolean("ok", false);
		r.httpCode = o.optInt("httpCode", 0);
		r.latencyMs = o.optLong("latencyMs", 0);
		r.reply = o.optString("reply", "");
		r.error = o.optString("error", "");
		r.promptTokens = o.optInt("promptTokens", 0);
		r.completionTokens = o.optInt("completionTokens", 0);
		r.cachedTokens = o.optInt("cachedTokens", 0);
		r.totalTokens = o.optInt("totalTokens", r.promptTokens + r.completionTokens);
		if (r.ok) r.state = OK;
		else if (r.state == PENDING && (r.httpCode != 0 || (r.error != null && !r.error.isEmpty()))) r.state = FAIL;
		return r;
	}

	public String detailLine() {
		if (state == RUNNING) return "请求中…";
		if (state == PENDING) return "排队中";
		if (ok) {
			String t = reply == null ? "" : reply.trim().replace('\n', ' ');
			if (t.isEmpty()) t = "HTTP " + httpCode;
			if (t.length() > 80) t = t.substring(0, 80) + "…";
			if (completionTokens > 0 || promptTokens > 0 || cachedTokens > 0 || totalTokens > 0) {
				t += "  ·  输入 " + promptTokens + " / 输出 " + completionTokens
					+ " / 缓存 " + cachedTokens + " / 总 " + totalTokens;
			}
			return t;
		}
		String e = error == null ? "" : error.trim().replace('\n', ' ');
		if (e.isEmpty()) e = "HTTP " + httpCode;
		if (e.length() > 120) e = e.substring(0, 120) + "…";
		return e;
	}
}

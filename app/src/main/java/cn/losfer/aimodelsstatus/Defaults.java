package cn.losfer.aimodelsstatus;

import org.json.JSONObject;

public class Defaults {
	public int concurrency = 3;
	public int intervalMs = 200;
	public int timeoutMs = 30000;
	public int maxTokens = 8;
	public String prompt = "Reply with exactly the two letters OK and nothing else.";

	public JSONObject toJson() {
		JSONObject o = new JSONObject();
		try {
			o.put("concurrency", concurrency);
			o.put("intervalMs", intervalMs);
			o.put("timeoutMs", timeoutMs);
			o.put("maxTokens", maxTokens);
			o.put("prompt", prompt == null ? "" : prompt);
		} catch (Exception ignored) {
		}
		return o;
	}

	public static Defaults fromJson(JSONObject o) {
		Defaults d = new Defaults();
		if (o == null) return d;
		d.concurrency = Math.max(1, o.optInt("concurrency", 3));
		d.intervalMs = Math.max(0, o.optInt("intervalMs", 200));
		d.timeoutMs = Math.max(1000, o.optInt("timeoutMs", 30000));
		d.maxTokens = Math.max(1, o.optInt("maxTokens", 8));
		d.prompt = o.optString("prompt", d.prompt);
		if (d.prompt == null || d.prompt.trim().isEmpty()) {
			d.prompt = "Reply with exactly the two letters OK and nothing else.";
		}
		return d;
	}

	public void applyTo(Site s) {
		s.concurrency = concurrency;
		s.intervalMs = intervalMs;
		s.timeoutMs = timeoutMs;
		s.maxTokens = maxTokens;
		if (s.prompt == null || s.prompt.trim().isEmpty()) {
			s.prompt = prompt;
		}
	}
}

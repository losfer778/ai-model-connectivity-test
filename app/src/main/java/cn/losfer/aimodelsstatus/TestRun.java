package cn.losfer.aimodelsstatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestRun {
	public String id;
	public String siteId;
	public String siteName;
	public String baseUrl;
	public long startedAt;
	public long finishedAt;
	public long durationMs;
	public int total;
	public int okCount;
	public int failCount;
	public int concurrency;
	public int intervalMs;
	public boolean cancelled;
	public final List<ModelResult> results = new ArrayList<ModelResult>();

	public static TestRun create(Site site, int modelCount) {
		TestRun r = new TestRun();
		r.id = UUID.randomUUID().toString();
		r.siteId = site.id;
		r.siteName = site.displayName();
		r.baseUrl = site.baseUrl;
		r.startedAt = System.currentTimeMillis();
		r.total = modelCount;
		r.concurrency = site.concurrency;
		r.intervalMs = site.intervalMs;
		return r;
	}

	public JSONObject toJson() {
		JSONObject o = new JSONObject();
		try {
			o.put("id", id);
			o.put("siteId", siteId);
			o.put("siteName", siteName);
			o.put("baseUrl", baseUrl);
			o.put("startedAt", startedAt);
			o.put("finishedAt", finishedAt);
			o.put("durationMs", durationMs);
			o.put("total", total);
			o.put("okCount", okCount);
			o.put("failCount", failCount);
			o.put("concurrency", concurrency);
			o.put("intervalMs", intervalMs);
			o.put("cancelled", cancelled);
			JSONArray arr = new JSONArray();
			for (ModelResult m : results) arr.put(m.toJson());
			o.put("results", arr);
		} catch (Exception ignored) {
		}
		return o;
	}

	public static TestRun fromJson(JSONObject o) {
		TestRun r = new TestRun();
		if (o == null) return r;
		r.id = o.optString("id", "");
		r.siteId = o.optString("siteId", "");
		r.siteName = o.optString("siteName", "");
		r.baseUrl = o.optString("baseUrl", "");
		r.startedAt = o.optLong("startedAt", 0);
		r.finishedAt = o.optLong("finishedAt", 0);
		r.durationMs = o.optLong("durationMs", 0);
		r.total = o.optInt("total", 0);
		r.okCount = o.optInt("okCount", 0);
		r.failCount = o.optInt("failCount", 0);
		r.concurrency = o.optInt("concurrency", 0);
		r.intervalMs = o.optInt("intervalMs", 0);
		r.cancelled = o.optBoolean("cancelled", false);
		JSONArray arr = o.optJSONArray("results");
		if (arr != null) {
			for (int i = 0; i < arr.length(); i++) {
				JSONObject item = arr.optJSONObject(i);
				if (item != null) r.results.add(ModelResult.fromJson(item));
			}
		}
		return r;
	}
}

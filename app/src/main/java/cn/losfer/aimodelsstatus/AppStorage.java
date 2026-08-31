package cn.losfer.aimodelsstatus;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AppStorage {
	private static AppStorage instance;
	private final File sitesFile;
	private final File historyFile;
	private final File settingsFile;
	private final List<Site> sites = new ArrayList<Site>();
	private final List<TestRun> history = new ArrayList<TestRun>();
	private Defaults defaults = new Defaults();

	public static synchronized AppStorage get(Context ctx) {
		if (instance == null) {
			instance = new AppStorage(ctx.getApplicationContext());
		}
		return instance;
	}

	private AppStorage(Context ctx) {
		File dir = ctx.getFilesDir();
		sitesFile = new File(dir, "sites.json");
		historyFile = new File(dir, "history.json");
		settingsFile = new File(dir, "settings.json");
		loadAll();
	}

	private void loadAll() {
		sites.clear();
		JSONArray sa = readArray(sitesFile);
		for (int i = 0; i < sa.length(); i++) {
			JSONObject o = sa.optJSONObject(i);
			if (o != null) sites.add(Site.fromJson(o));
		}
		history.clear();
		JSONArray ha = readArray(historyFile);
		for (int i = 0; i < ha.length(); i++) {
			JSONObject o = ha.optJSONObject(i);
			if (o != null) history.add(TestRun.fromJson(o));
		}
		JSONObject so = readObject(settingsFile);
		defaults = Defaults.fromJson(so);
	}

	public synchronized List<Site> sites() {
		return new ArrayList<Site>(sites);
	}

	public synchronized Site findSite(String id) {
		if (id == null) return null;
		for (Site s : sites) {
			if (id.equals(s.id)) return s;
		}
		return null;
	}

	public synchronized void upsertSite(Site site) {
		if (site == null || site.id == null) return;
		for (int i = 0; i < sites.size(); i++) {
			if (site.id.equals(sites.get(i).id)) {
				sites.set(i, site);
				persistSites();
				return;
			}
		}
		sites.add(0, site);
		persistSites();
	}

	public synchronized void deleteSite(String id) {
		if (id == null) return;
		for (int i = sites.size() - 1; i >= 0; i--) {
			if (id.equals(sites.get(i).id)) {
				sites.remove(i);
			}
		}
		persistSites();
	}

	public synchronized List<TestRun> history() {
		return new ArrayList<TestRun>(history);
	}

	public synchronized TestRun findRun(String id) {
		if (id == null) return null;
		for (TestRun r : history) {
			if (id.equals(r.id)) return r;
		}
		return null;
	}

	public synchronized void addRun(TestRun run) {
		if (run == null) return;
		history.add(0, run);
		while (history.size() > 100) {
			history.remove(history.size() - 1);
		}
		persistHistory();
	}

	public synchronized void clearHistory() {
		history.clear();
		persistHistory();
	}

	public synchronized Defaults defaults() {
		return defaults;
	}

	public synchronized void saveDefaults(Defaults d) {
		if (d == null) return;
		this.defaults = d;
		writeObject(settingsFile, d.toJson());
	}

	private void persistSites() {
		JSONArray arr = new JSONArray();
		for (Site s : sites) arr.put(s.toJson());
		writeArray(sitesFile, arr);
	}

	private void persistHistory() {
		JSONArray arr = new JSONArray();
		for (TestRun r : history) arr.put(r.toJson());
		writeArray(historyFile, arr);
	}

	private static JSONArray readArray(File f) {
		String raw = read(f);
		if (raw.isEmpty()) return new JSONArray();
		try {
			return new JSONArray(raw);
		} catch (Exception e) {
			return new JSONArray();
		}
	}

	private static JSONObject readObject(File f) {
		String raw = read(f);
		if (raw.isEmpty()) return new JSONObject();
		try {
			return new JSONObject(raw);
		} catch (Exception e) {
			return new JSONObject();
		}
	}

	private static String read(File f) {
		if (f == null || !f.exists()) return "";
		FileInputStream in = null;
		try {
			in = new FileInputStream(f);
			byte[] buf = new byte[(int) Math.min(f.length(), 8 * 1024 * 1024)];
			int n = in.read(buf);
			if (n <= 0) return "";
			return new String(buf, 0, n, StandardCharsets.UTF_8);
		} catch (Exception e) {
			return "";
		} finally {
			if (in != null) try { in.close(); } catch (Exception ignored) {}
		}
	}

	private static void writeArray(File f, JSONArray arr) {
		write(f, arr == null ? "[]" : arr.toString());
	}

	private static void writeObject(File f, JSONObject o) {
		write(f, o == null ? "{}" : o.toString());
	}

	private static void write(File f, String text) {
		File tmp = new File(f.getAbsolutePath() + ".tmp");
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(tmp);
			out.write(text.getBytes(StandardCharsets.UTF_8));
			out.flush();
			out.close();
			out = null;
			if (f.exists()) f.delete();
			tmp.renameTo(f);
		} catch (Exception ignored) {
		} finally {
			if (out != null) try { out.close(); } catch (Exception ignored) {}
		}
	}
}

package cn.losfer.aimodelsstatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import cn.losfer.aimodelsstatus.ModelResult;
import cn.losfer.aimodelsstatus.Site;

public class OpenAiApi {

	public static String normalizeBase(String raw) {
		if (raw == null) return "";
		String u = raw.trim();
		if (u.isEmpty()) return "";
		u = u.replace('\\', '/');
		if (!u.toLowerCase(Locale.US).startsWith("http://") && !u.toLowerCase(Locale.US).startsWith("https://")) {
			u = "https://" + u;
		}
		while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
		String lower = u.toLowerCase(Locale.US);
		while (lower.endsWith("/v1")) {
			u = u.substring(0, u.length() - 3);
			while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
			lower = u.toLowerCase(Locale.US);
		}
		return u;
	}

	public static String modelsUrl(String base) {
		return normalizeBase(base) + "/v1/models";
	}

	public static String chatUrl(String base) {
		return normalizeBase(base) + "/v1/chat/completions";
	}

	public static List<String> fetchModels(Site site) throws Exception {
		String url = modelsUrl(site.baseUrl);
		HttpClient.Response r = HttpClient.get(url, site.apiKey, site.timeoutMs);
		if (r.code < 200 || r.code >= 300) {
			throw new Exception(errorMessage(r, "拉取模型失败"));
		}
		List<String> ids = parseModelIds(r.body);
		if (ids.isEmpty()) {
			throw new Exception("接口返回了空的模型列表");
		}
		return ids;
	}

	/**
	 * Image-only models cannot be validated through /v1/chat/completions.
	 * Keep multimodal chat models (for example gpt-4o) because they still support chat.
	 */
	public static boolean isImageOnlyModel(String modelId) {
		if (modelId == null) return false;
		String id = modelId.trim().toLowerCase(Locale.US);
		if (id.length() == 0) return false;
		String[] keywords = new String[] {
			"dall-e", "dalle", "gpt-image", "stable-diffusion", "stable_diffusion", "sdxl",
			"image-generation", "image_generation", "imagegen", "imagen", "imag",
			"midjourney", "flux", "playground-v", "kandinsky", "sana-",
			"nano-banana", "seedream", "hunyuanimage", "qwen-image"
		};
		for (int i = 0; i < keywords.length; i++) {
			if (id.indexOf(keywords[i]) >= 0) return true;
		}
		return false;
	}

	public static List<String> removeImageOnlyModels(List<String> models) {
		List<String> kept = new ArrayList<String>();
		if (models == null) return kept;
		for (int i = 0; i < models.size(); i++) {
			String id = models.get(i);
			if (id != null && !id.trim().isEmpty() && !isImageOnlyModel(id)) {
				kept.add(id);
			}
		}
		return kept;
	}

	public static ModelResult probeChat(Site site, String model) {
		ModelResult out = new ModelResult();
		out.model = model;
		out.state = ModelResult.RUNNING;
		String prompt = site.prompt;
		if (prompt == null || prompt.trim().isEmpty()) {
			prompt = "Reply with exactly the two letters OK and nothing else.";
		}
		JSONObject body = new JSONObject();
		try {
			body.put("model", model);
			JSONArray msgs = new JSONArray();
			JSONObject msg = new JSONObject();
			msg.put("role", "user");
			msg.put("content", prompt);
			msgs.put(msg);
			body.put("messages", msgs);
			body.put("max_tokens", Math.max(1, site.maxTokens));
			body.put("temperature", 0);
			body.put("stream", false);
		} catch (Exception ignored) {
		}
		HttpClient.Response r = HttpClient.postJson(chatUrl(site.baseUrl), site.apiKey, body.toString(), site.timeoutMs);
		out.httpCode = r.code;
		out.latencyMs = r.latencyMs;
		if (r.code >= 200 && r.code < 300) {
			fillFromChat(out, r.body);
			if (out.ok) {
				out.state = ModelResult.OK;
				out.error = "";
			} else {
				out.state = ModelResult.FAIL;
				if (out.error == null || out.error.isEmpty()) {
					out.error = "HTTP " + r.code + " 但未解析到 choices";
				}
			}
		} else {
			out.ok = false;
			out.state = ModelResult.FAIL;
			out.error = errorMessage(r, "请求失败");
		}
		return out;
	}

	static List<String> parseModelIds(String raw) {
		LinkedHashSet<String> set = new LinkedHashSet<String>();
		if (raw == null || raw.trim().isEmpty()) return new ArrayList<String>();
		String t = raw.trim();
		try {
			if (t.startsWith("[")) {
				collectIds(new JSONArray(t), set);
			} else if (t.startsWith("{")) {
				JSONObject o = new JSONObject(t);
				if (o.has("data")) collectIds(o.opt("data"), set);
				if (o.has("models")) collectIds(o.opt("models"), set);
				if (o.has("data") == false && o.has("id")) {
					String id = o.optString("id", "");
					if (!id.isEmpty()) set.add(id);
				}
			}
		} catch (Exception ignored) {
		}
		return new ArrayList<String>(set);
	}

	private static void collectIds(Object node, LinkedHashSet<String> set) {
		if (node == null) return;
		if (node instanceof JSONArray) {
			JSONArray arr = (JSONArray) node;
			for (int i = 0; i < arr.length(); i++) {
				Object item = arr.opt(i);
				if (item instanceof JSONObject) {
					String id = ((JSONObject) item).optString("id", "");
					if (id.isEmpty()) id = ((JSONObject) item).optString("name", "");
					if (!id.isEmpty()) set.add(id);
				} else if (item instanceof String) {
					String id = ((String) item).trim();
					if (!id.isEmpty()) set.add(id);
				}
			}
		} else if (node instanceof JSONObject) {
			String id = ((JSONObject) node).optString("id", "");
			if (!id.isEmpty()) set.add(id);
		}
	}

	private static void fillFromChat(ModelResult out, String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			out.ok = false;
			out.error = "空响应";
			return;
		}
		try {
			JSONObject o = new JSONObject(raw);
			if (o.has("error")) {
				out.ok = false;
				out.error = stringifyError(o.opt("error"));
				return;
			}
			JSONObject usage = o.optJSONObject("usage");
			if (usage != null) {
				out.promptTokens = usage.optInt("prompt_tokens", usage.optInt("input_tokens", 0));
				out.completionTokens = usage.optInt("completion_tokens", usage.optInt("output_tokens", 0));
				out.totalTokens = usage.optInt("total_tokens", out.promptTokens + out.completionTokens);
				JSONObject promptDetails = usage.optJSONObject("prompt_tokens_details");
				if (promptDetails == null) promptDetails = usage.optJSONObject("input_tokens_details");
				if (promptDetails != null) {
					out.cachedTokens = promptDetails.optInt("cached_tokens", promptDetails.optInt("cache_read_input_tokens", 0));
				}
			}
			JSONArray choices = o.optJSONArray("choices");
			if (choices == null || choices.length() == 0) {
				out.ok = false;
				out.error = "无 choices";
				return;
			}
			JSONObject c0 = choices.optJSONObject(0);
			String text = "";
			if (c0 != null) {
				JSONObject message = c0.optJSONObject("message");
				if (message != null) text = extractContent(message.opt("content"));
				if (text.isEmpty()) text = c0.optString("text", "");
				if (text.isEmpty()) text = extractContent(c0.opt("delta"));
			}
			out.reply = text;
			out.ok = true;
		} catch (Exception e) {
			out.ok = false;
			out.error = "JSON 解析失败: " + e.getMessage();
		}
	}

	private static String extractContent(Object content) {
		if (content == null) return "";
		if (content instanceof String) return ((String) content).trim();
		if (content instanceof JSONArray) {
			JSONArray arr = (JSONArray) content;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < arr.length(); i++) {
				Object it = arr.opt(i);
				if (it instanceof String) sb.append((String) it);
				else if (it instanceof JSONObject) {
					JSONObject o = (JSONObject) it;
					String t = o.optString("text", "");
					if (t.isEmpty()) t = o.optString("content", "");
					sb.append(t);
				}
			}
			return sb.toString().trim();
		}
		if (content instanceof JSONObject) {
			JSONObject o = (JSONObject) content;
			String t = o.optString("content", "");
			if (t.isEmpty()) t = o.optString("text", "");
			return t.trim();
		}
		return String.valueOf(content);
	}

	private static String stringifyError(Object err) {
		if (err == null) return "error";
		if (err instanceof JSONObject) {
			JSONObject o = (JSONObject) err;
			String msg = o.optString("message", "");
			if (msg.isEmpty()) msg = o.optString("msg", "");
			String type = o.optString("type", "");
			String code = o.optString("code", "");
			StringBuilder sb = new StringBuilder();
			if (!type.isEmpty()) sb.append(type).append(": ");
			sb.append(msg.isEmpty() ? o.toString() : msg);
			if (!code.isEmpty()) sb.append(" (").append(code).append(")");
			return sb.toString();
		}
		return String.valueOf(err);
	}

	private static String errorMessage(HttpClient.Response r, String fallback) {
		String fromBody = parseErrorBody(r.body);
		if (fromBody != null && !fromBody.isEmpty()) {
			return "HTTP " + r.code + " · " + fromBody;
		}
		if (r.error != null && !r.error.isEmpty()) {
			return r.error;
		}
		return fallback + " HTTP " + r.code;
	}

	private static String parseErrorBody(String body) {
		if (body == null || body.trim().isEmpty()) return "";
		String t = body.trim();
		try {
			if (t.startsWith("{")) {
				JSONObject o = new JSONObject(t);
				if (o.has("error")) return stringifyError(o.opt("error"));
				String m = o.optString("message", "");
				if (m.isEmpty()) m = o.optString("msg", "");
				return m;
			}
		} catch (Exception ignored) {
		}
		if (t.length() > 180) t = t.substring(0, 180) + "…";
		return t.replace('\n', ' ');
	}
}

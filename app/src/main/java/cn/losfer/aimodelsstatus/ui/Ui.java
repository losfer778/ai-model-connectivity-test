package cn.losfer.aimodelsstatus.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.losfer.aimodelsstatus.R;

public final class Ui {
	private static final int[] AVATARS = {
		R.color.avatar_1, R.color.avatar_2, R.color.avatar_3,
		R.color.avatar_4, R.color.avatar_5, R.color.avatar_6
	};
	private static final SimpleDateFormat TIME = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
	private static final SimpleDateFormat FULL = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

	private Ui() {}

	public static void toast(Context ctx, String msg) {
		if (ctx == null) return;
		Toast.makeText(ctx.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	public static String formatTime(long ms) {
		if (ms <= 0) return "—";
		return TIME.format(new Date(ms));
	}

	public static String formatFull(long ms) {
		if (ms <= 0) return "—";
		return FULL.format(new Date(ms));
	}

	public static String formatDuration(long ms) {
		if (ms < 0) ms = 0;
		if (ms < 1000) return ms + "ms";
		long sec = ms / 1000;
		if (sec < 60) {
			if (ms % 1000 >= 100) {
				return String.format(Locale.CHINA, "%.1fs", ms / 1000f);
			}
			return sec + "s";
		}
		long min = sec / 60;
		long rem = sec % 60;
		if (min < 60) return min + "m " + rem + "s";
		long h = min / 60;
		return h + "h " + (min % 60) + "m";
	}

	public static String formatLatency(long ms) {
		if (ms <= 0) return "";
		if (ms < 1000) return ms + "ms";
		return String.format(Locale.CHINA, "%.1fs", ms / 1000f);
	}

	public static int avatarColor(Context ctx, int index) {
		int i = Math.abs(index) % AVATARS.length;
		return ContextCompat.getColor(ctx, AVATARS[i]);
	}

	public static void paintAvatar(TextView tv, int color) {
		GradientDrawable d = new GradientDrawable();
		d.setShape(GradientDrawable.OVAL);
		d.setColor(color);
		tv.setBackground(d);
	}

	public static void setVisible(View v, boolean show) {
		if (v != null) v.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public static int parseInt(CharSequence cs, int fallback) {
		if (cs == null) return fallback;
		String s = cs.toString().trim();
		if (s.isEmpty()) return fallback;
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return fallback;
		}
	}

	public static String text(TextView tv) {
		if (tv == null || tv.getText() == null) return "";
		return tv.getText().toString().trim();
	}

	public static ColorStateList tint(int color) {
		return ColorStateList.valueOf(color);
	}
}

package cn.losfer.aimodelsstatus;

import android.app.Application;

import io.github.zeroaicy.util.crash.CrashApphandler;
import io.github.zeroaicy.util.crash.CrashApplication;

public class App extends CrashApplication {
	@Override
	public void onCreate() {
		super.onCreate();
		try {
			CrashApplication.CrashInit(this);
		} catch (Exception ignored) {
			try {
				CrashApphandler.getInstance().init(this);
			} catch (Exception ignored2) {
			}
		}
	}
}

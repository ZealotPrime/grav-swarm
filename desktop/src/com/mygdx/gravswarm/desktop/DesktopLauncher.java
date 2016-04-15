package com.mygdx.gravswarm.desktop;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.gravswarm.GravSwarm;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		Graphics.DisplayMode displayMode = LwjglApplicationConfiguration.getDesktopDisplayMode();
		config.setFromDisplayMode(displayMode);
		new LwjglApplication(new GravSwarm(), config);
	}
}

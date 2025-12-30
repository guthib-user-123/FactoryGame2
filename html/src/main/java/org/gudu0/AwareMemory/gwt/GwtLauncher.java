package org.gudu0.AwareMemory.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import org.gudu0.AwareMemory.Main;

public final class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        return new GwtApplicationConfiguration(1280, 720);
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new Main();
    }
}

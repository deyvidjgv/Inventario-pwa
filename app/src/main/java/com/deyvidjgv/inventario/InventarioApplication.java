package com.deyvidjgv.inventario;

import android.app.Application;
import com.deyvidjgv.inventario.di.AppContainer;

public class InventarioApplication extends Application {
    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
    }

    public AppContainer getContainer() {
        return container;
    }
}

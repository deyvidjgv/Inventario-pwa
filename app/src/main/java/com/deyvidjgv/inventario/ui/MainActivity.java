package com.deyvidjgv.inventario.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.deyvidjgv.inventario.R;
import com.deyvidjgv.inventario.ui.fragment.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottom_navigation);

        toolbar.inflateMenu(R.menu.main_toolbar_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_daily_report) {
                loadFragment(new DailyReportFragment(), "Reporte Diario");
                return true;
            }
            return false;
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new HomeFragment(), "Inicio");
                return true;
            } else if (itemId == R.id.nav_sales) {
                loadFragment(new SalesFragment(), "Vender");
                return true;
            } else if (itemId == R.id.nav_inventory) {
                loadFragment(new InventoryFragment(), "Inventario");
                return true;
            } else if (itemId == R.id.nav_margins) {
                loadFragment(new MarginsFragment(), "Márgenes");
                return true;
            } else if (itemId == R.id.nav_money) {
                loadFragment(new ExpensesFragment(), "Dinero y Gastos");
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "Inicio");
        }
    }

    public void loadFragment(Fragment fragment, String title) {
        toolbar.setTitle(title);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}

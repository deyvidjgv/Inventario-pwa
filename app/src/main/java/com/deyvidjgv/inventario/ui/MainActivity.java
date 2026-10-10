package com.deyvidjgv.inventario.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
                loadFragment(new DailyReportFragment(), "Reporte Diario", true);
                return true;
            }
            return false;
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new HomeFragment(), "Inicio", false);
                return true;
            } else if (itemId == R.id.nav_sales) {
                loadFragment(new SalesFragment(), "Vender", false);
                return true;
            } else if (itemId == R.id.nav_inventory) {
                loadFragment(new InventoryFragment(), "Inventario", false);
                return true;
            } else if (itemId == R.id.nav_margins) {
                loadFragment(new MarginsFragment(), "Márgenes", false);
                return true;
            } else if (itemId == R.id.nav_money) {
                loadFragment(new ExpensesFragment(), "Dinero y Gastos", false);
                return true;
            }
            return false;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (current instanceof HomeFragment) {
                toolbar.setTitle("Inicio");
                bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
            } else if (current instanceof SalesFragment) {
                toolbar.setTitle("Vender");
                bottomNav.getMenu().findItem(R.id.nav_sales).setChecked(true);
            } else if (current instanceof InventoryFragment) {
                toolbar.setTitle("Inventario");
                bottomNav.getMenu().findItem(R.id.nav_inventory).setChecked(true);
            } else if (current instanceof MarginsFragment) {
                toolbar.setTitle("Márgenes");
                bottomNav.getMenu().findItem(R.id.nav_margins).setChecked(true);
            } else if (current instanceof ExpensesFragment) {
                toolbar.setTitle("Dinero y Gastos");
                bottomNav.getMenu().findItem(R.id.nav_money).setChecked(true);
            } else if (current instanceof DailyReportFragment) {
                toolbar.setTitle("Reporte Diario");
            } else if (current instanceof BackupFragment) {
                toolbar.setTitle("Respaldo");
            }
        });

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "Inicio", false);
        }
    }

    public void loadFragment(Fragment fragment, String title) {
        loadFragment(fragment, title, false);
    }

    public void loadFragment(Fragment fragment, String title, boolean addToBackStack) {
        toolbar.setTitle(title);
        if (!addToBackStack) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        var tx = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            tx.addToBackStack(title);
        }
        tx.commit();
    }
}

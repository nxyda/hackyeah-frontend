package com.example.saferoute;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        bottomNavigationView =
                findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {

            openFragment(
                    new MapaFragment()
            );

            bottomNavigationView.setSelectedItemId(
                    R.id.nav_map
            );
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();

            if (itemId == R.id.nav_map) {

                openFragment(
                        new MapaFragment()
                );

            } else if (itemId == R.id.nav_share) {

                openFragment(
                        new UdostepnianieFragment()
                );

            } else if (itemId == R.id.nav_sos) {

                openFragment(
                        new SosFragment()
                );

            } else if (itemId == R.id.nav_phone) {

                openFragment(
                        new TelefonFragment()
                );

            } else if (itemId == R.id.nav_profile) {

                openFragment(
                        new ProfilFragment()
                );

            } else {

                return false;
            }

            return true;
        });
    }

    private void openFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragment_container,
                        fragment
                )
                .commit();
    }

}

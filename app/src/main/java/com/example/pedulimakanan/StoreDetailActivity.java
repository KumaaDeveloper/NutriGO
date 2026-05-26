package com.example.pedulimakanan;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Placeholder for the Store Detail / Order screen.
 * We will build this out fully in the next step.
 * For now it just shows the store name so the app compiles.
 */
public class StoreDetailActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Temporary: show a toast so you know it works
        String namaResto = getIntent().getStringExtra("restoran_nama");
        Toast.makeText(this,
                "Membuka: " + namaResto + "\n(Coming in step 2!)",
                Toast.LENGTH_LONG).show();
        finish(); // Remove this line once we build the real screen
    }
}
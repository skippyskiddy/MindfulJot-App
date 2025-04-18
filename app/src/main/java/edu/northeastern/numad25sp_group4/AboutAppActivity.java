package edu.northeastern.numad25sp_group4;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutAppActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);

        // Initialize views
        initViews();

        // Set version info
        setVersionInfo();

        // Set back button click listener
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvVersion = findViewById(R.id.tv_version);
    }

    private void setVersionInfo() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("Version " + versionName);
        } catch (Exception e) {
            // If version info can't be retrieved, use default value
            tvVersion.setText("Version 1.0.0");
        }
    }
}
package edu.northeastern.numad25sp_group4;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.FirebaseApp;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase if not already initialized
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // request permission for notifications
        StickerNotificationManager.requestNotificationPermission(this);

    }

    public void onClick(View view){
        int theId = view.getId();
        if (theId == R.id.buttonWebService) {
            startActivity(new Intent(MainActivity.this, WebService.class));
        }
        if (theId == R.id.buttonAbout) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
        }
        if (theId == R.id.buttonStickItToEm) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
    }
}
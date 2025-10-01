/*
 * Copyright (c) 2006-2024 LOVE Development Team
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package org.love2d.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FolderSelectorActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "GameHubPrefs";
    private static final String GAMES_FOLDER_URI = "games_folder_uri";
    
    private TextView selectedFolderText;
    private MaterialCardView folderCard;
    private FloatingActionButton selectFolderFab;
    private Button continueButton;
    
    private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                android.net.Uri uri = data.getData();
                if (uri != null) {
                    // Persist permissions
                    getContentResolver().takePersistableUriPermission(uri, 
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    
                    // Save URI to preferences
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().putString(GAMES_FOLDER_URI, uri.toString()).apply();
                    
                    // Update UI
                    updateFolderSelection(uri);
                }
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_folder_selector);
        EdgeToEdgeHelper.applySystemBarsPadding(findViewById(R.id.folderSelectorRoot));

        initializeViews();
        setupListeners();
        checkExistingFolder();
    }

    private void initializeViews() {
        selectedFolderText = findViewById(R.id.selectedFolderText);
        folderCard = findViewById(R.id.folderCard);
        selectFolderFab = findViewById(R.id.selectFolderFab);
        continueButton = findViewById(R.id.continueButton);
    }

    private void setupListeners() {
        selectFolderFab.setOnClickListener(v -> openFolderPicker());
        folderCard.setOnClickListener(v -> openFolderPicker());
        continueButton.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String folderUri = prefs.getString(GAMES_FOLDER_URI, null);
            if (folderUri != null) {
                Intent intent = new Intent(this, GameListActivity.class);
                intent.putExtra("folder_uri", folderUri);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Por favor selecione uma pasta primeiro", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkExistingFolder() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUri = prefs.getString(GAMES_FOLDER_URI, null);
        if (savedUri != null) {
            android.net.Uri uri = android.net.Uri.parse(savedUri);
            updateFolderSelection(uri);
        }
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        folderPickerLauncher.launch(intent);
    }

    private void updateFolderSelection(android.net.Uri uri) {
        try {
            DocumentFile documentFile = DocumentFile.fromTreeUri(this, uri);
            if (documentFile != null) {
                String folderName = documentFile.getName();
                selectedFolderText.setText(folderName != null ? folderName : "Pasta selecionada");
                continueButton.setEnabled(true);
                folderCard.setCardBackgroundColor(getResources().getColor(android.R.color.system_accent1_200, null));
            }
        } catch (Exception e) {
            selectedFolderText.setText("Erro ao acessar pasta");
            continueButton.setEnabled(false);
        }
    }
}
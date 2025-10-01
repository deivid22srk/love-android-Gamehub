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

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "GameHubPrefs";
    private static final String GAMES_FOLDER_URI = "games_folder_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user has selected a folder before
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String folderUri = prefs.getString(GAMES_FOLDER_URI, null);
        
        if (folderUri != null) {
            // User has selected folder, go directly to game list
            Intent intent = new Intent(this, GameListActivity.class);
            intent.putExtra("folder_uri", folderUri);
            startActivity(intent);
        } else {
            // First time user, show folder selector
            Intent intent = new Intent(this, FolderSelectorActivity.class);
            startActivity(intent);
        }
        
        finish();
    }


}

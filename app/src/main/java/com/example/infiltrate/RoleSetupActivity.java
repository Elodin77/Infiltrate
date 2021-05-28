package com.example.infiltrate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.StrictMode;
import android.view.View;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;

public class RoleSetupActivity extends AppCompatActivity {

    ArrayList<Pair<Game.Role,int[]>> roleRestrictions = new ArrayList<Pair<Game.Role,int[]>>();
    ArrayList<String> playerList = new ArrayList<String>();
    RoleListAdapter rvAdapter;
    String errorMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_setup);


        // Get the player list
        if (getIntent().getStringArrayListExtra("PLAYER_LIST") != null) {
            playerList = getIntent().getStringArrayListExtra("PLAYER_LIST");
        }
        rvAdapter = new RoleListAdapter(roleRestrictions);
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.role_list_layout);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(rvAdapter);


        for (Game.Role role : Game.Role.values()) {
            roleRestrictions.add(new Pair<Game.Role,int[]>(role,new int[] {0,0}));
            rvAdapter.notifyItemInserted(roleRestrictions.size() - 1);
        }

    }

    public void onBackBtnClicked(View v) {
        Intent intent = new Intent(RoleSetupActivity.this,PlayerSetupActivity.class);
        intent.putExtra("PLAYER_LIST",playerList);
        startActivity(intent);
    }
    public void onNextBtnClicked(View v) {
        if (isLegalRoleRestrictions()) {
            Intent intent = new Intent(RoleSetupActivity.this, GameActivity.class);

            // Generate the parcelable role restrictions
            int[] minimums = new int[roleRestrictions.size()];
            int[] maximums = new int[roleRestrictions.size()];
            for (Pair<Game.Role, int[]> i : roleRestrictions) {
                minimums[i.first.ordinal()] = i.second[0];
                maximums[i.first.ordinal()] = i.second[1];
            }

            intent.putExtra("ROLE_RESTRICTIONS_MAX", maximums);
            intent.putExtra("ROLE_RESTRICTIONS_MIN", minimums);
            intent.putStringArrayListExtra("PLAYER_LIST", playerList);
            startActivity(intent);
        } else {
            if (errorMessage.equals("")) errorMessage = "Invalid values";
            Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Checks for legal restrictions according to the following criteria:
     * 1. Minimum is less than maximum.
     * 2. Sum of minimum values is no more than the number of players.
     * 3. Sum of maximum values is at least the number of players.
     * 4. There is at least one Infiltrator and one Citizen.
     * 5. Sum of max dead > max living for each side.
     * @return - whether or not the role restrictions are legal
     */
    public boolean isLegalRoleRestrictions() {
        boolean legal = true;
        int minSumAliveCitizen = 0;
        int maxSumAliveCitizen = 0;
        int minSumDeadCitizen = 0;
        int maxSumDeadCitizen = 0;
        int minSumAliveInfiltrator = 0;
        int maxSumAliveInfiltrator = 0;
        int minSumDeadInfiltrator = 0;
        int maxSumDeadInfiltrator = 0;
        boolean existsInfiltrator = false;
        boolean existsCitizen = false;
        for (Pair<Game.Role,int[]> i : roleRestrictions) {
            if (i.second[0]>i.second[1]) {
                legal = false;
                errorMessage = "Minimum is greater than maximum";
            }
            if (i.first.isAlive) {
                if (i.first.isCitizen) {
                    minSumAliveCitizen += i.second[0];
                    maxSumAliveCitizen += i.second[1];
                } else {
                    minSumAliveInfiltrator += i.second[0];
                    maxSumAliveInfiltrator += i.second[1];
                }
            } else {
                if (i.first.isCitizen) {
                    minSumDeadCitizen += i.second[0];
                    maxSumDeadCitizen += i.second[1];
                } else {
                    minSumDeadInfiltrator += i.second[0];
                    maxSumDeadInfiltrator += i.second[1];
                }
            }
            if (i.first.equals(Game.Role.INFILTRATOR)&&i.second[0]>0) existsInfiltrator = true;
            if (i.first.equals(Game.Role.CITIZEN)&&i.second[0]>0) existsCitizen = true;

        }
        int minSumAlive = minSumAliveCitizen + minSumAliveInfiltrator;
        int minSumDead = minSumDeadCitizen + minSumDeadInfiltrator;
        int maxSumAlive = maxSumAliveCitizen + maxSumAliveInfiltrator;
        int maxSumDead = maxSumDeadCitizen + maxSumDeadInfiltrator;

        if (!existsCitizen) errorMessage = "Need at least 1 CITIZEN";
        else if (!existsInfiltrator) errorMessage = "Need at least 1 INFILTRATOR";
        else if (minSumAlive > playerList.size()) errorMessage = "Minimum living roles is too high";
        else if (minSumDead > playerList.size()) errorMessage = "Minimum dead roles is too high";
        else if (maxSumAlive < playerList.size()) errorMessage = "Maximum living roles is too low";
        else if (maxSumDead < playerList.size()) errorMessage = "Maximum dead roles is too low";
        else if (maxSumDeadCitizen < maxSumAliveCitizen) errorMessage = "Maximum dead citizen roles too low";
        else if (maxSumDeadInfiltrator < maxSumAliveInfiltrator) errorMessage = "Maximum dead infiltrator roles too low";

        legal = legal && minSumAlive <= playerList.size() && maxSumAlive >= playerList.size();
        legal = legal && minSumDead  <= playerList.size() && maxSumDead  >= playerList.size();
        legal = legal && maxSumDeadCitizen >= maxSumAliveCitizen && maxSumDeadInfiltrator >= maxSumAliveInfiltrator;
        legal = legal && existsCitizen && existsInfiltrator;
        return legal;
    }
}
package wfa.issp.test;

import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SOP::SettingsActivity";

    // User inputs are strings because the input is given through EditText
    private String referenceObjectSide;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d("Settings activity", "Settings activity created.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        SharedPreferences sharedPref = SettingsActivity.this.getPreferences(Context.MODE_PRIVATE);
        Float Vert2X = sharedPref.getFloat("refObjSide", 0);
        referenceObjectSide = String.valueOf(Vert2X);


        initTextViews();
    }

    public void onApplySettings(View view) {
        Intent intent = new Intent();

        EditText referenceObjectSideEditText = (EditText) findViewById(R.id.edit_text_cm_to_px);
        referenceObjectSide =referenceObjectSideEditText.getText().toString();

        SharedPreferences sharedPref = SettingsActivity.this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (!referenceObjectSide.isEmpty()) {
            if (Float.parseFloat(referenceObjectSide) >= 0) {
                intent.putExtra("referenceObjectSide", Double.parseDouble(referenceObjectSide));
                editor.putFloat("refObjSide", Float.parseFloat(referenceObjectSide));
                editor.commit();
            }
        }

        setResult(RESULT_OK, intent);
        finish();
    }



    private void initTextViews(){

        // initialize TextViews
        TextView referenceObjectSideTextView = (EditText) findViewById(R.id.edit_text_cm_to_px);
        // Get values from MainActivity through Intent
        //referenceObjectSide = getIntent().getStringExtra("referenceObjectSide");


        // Set values to TextViews
        referenceObjectSideTextView.setText(referenceObjectSide);
    }
}

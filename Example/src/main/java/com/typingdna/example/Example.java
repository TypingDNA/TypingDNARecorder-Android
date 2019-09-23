package com.typingdna.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.typingdna.TypingDNARecorderMobile;


/*In order to capture accurate typing patterns you need to enable TypingDNA Overlay Service.
 This service draws an overlay over the activity. In order to draw the overlay you have to add
 "android.permission.SYSTEM_ALERT_WINDOW"  and "android.permission.TYPE_APPLICATION_OVERLAY" in your
 AndroidManifest file. It is also necessary to declare and enable the "com.typingdna.TypingDNAOverlayService"
 Please see the included "AndroidManifest" file. You can permanently disable the overlay service
 by setting  "private static Boolean overlayEnabled" to "false" in TypingDNARecorderMobile class;
 */

public class Example extends AppCompatActivity {

    private TypingDNARecorderMobile tdna;
    private EditText textField;
    private TextView resultOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        //Initializes the typingDNA recorder and starts recording.
        tdna = new TypingDNARecorderMobile(this);
        tdna.start();

        textField = findViewById(R.id.textField);
        resultOutput = findViewById(R.id.resultOutput);

        //Adds a target to the recorder. You can add multiple elements. All the typing evens will be recorded for this component.
        tdna.addTarget(R.id.textField);
    }

    @Override
    protected void onDestroy() {
        //Stops the overlay service and ends the recording of further typing events.
        tdna.stop();
        super.onDestroy();
    }
    @Override
    protected void onPause(){
        //Stops the overlay service and ends the recording of further typing events.
        tdna.pause();
        super.onPause();
    }

    @Override
    protected void onStop(){
        //Stops the overlay service and ends the recording of further typing events.
        tdna.stop();
        super.onStop();
    }

    @Override
    protected void onResume(){
        //Starts recording the typing evens and also starts the overlay service.
        tdna.start();
        super.onResume();
    }

    public void reset(View view) {
        //Resets the history stack of recorded typing events.
        tdna.reset();

        resultOutput.setText("");
        textField.setText("");
        textField.requestFocus();
    }

    public void getTypingPattern(View view){
        int type = 1; // 1,2 for diagram pattern (short identical texts - 2 for extended diagram), 0 for any-text typing pattern (random text)
        switch (view.getId()) {
            case (R.id.getType1):
                type = 1;
                break;
            case (R.id.getType2):
                type = 2;
                break;
            case (R.id.getType0):
                type = 0;
                break;
        }
        int length = 0; // (Optional) the length of the text in the history for which you want the typing pattern, 0 = ignore, (works only if text = "")
        String text = textField.getText().toString(); // (Only for type 1 and type 2) a typed string that you want the typing pattern for
        int textId = 0; // (Optional, only for type 1 and type 2) a personalized id for the typed text, 0 = ignore
        boolean caseSensitive = false; // (Optional, only for type 1 and type 2) Used only if you pass a text for type 1
        Integer targetId = textField.getId(); //(Optional, only for type 1 and type 2) Specifies if pattern is obtain only from text typed in a certain target
        String tp = tdna.getTypingPattern(type, length, text, textId, targetId, caseSensitive);
        if(tp != null){
            resultOutput.setText(tp);
        } else {
            resultOutput.setText("");
        }
    }
}

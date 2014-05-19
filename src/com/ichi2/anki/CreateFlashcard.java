package com.ichi2.anki;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import com.ichi2.async.DeckTask;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Note;
import com.ichi2.themes.StyledOpenCollectionDialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity to create a flashcard.
 * This activity is called via an Intent, for instance by a dictionary app.
 */
public class CreateFlashcard extends Activity {
	final static String[] noteTypeFields = new String[]{"Expression", "Meaning", "Reading"};
	final static String[] basicNoteTypeFields = new String[]{"Front", "Back"};
	final static HashMap<String, String[]> availableNoteTypes ;
	final static String DEFAULT_NOTETYPE = "MyCustomNoteType";
	private StyledOpenCollectionDialog mOpenCollectionDialog;
	private Resources res;

	private ArrayList<HashMap<String, Serializable>> intentNotes;
	private Collection mCol;
	static {
		availableNoteTypes = new HashMap<String, String[]>();
		availableNoteTypes.put("MyCustomNoteType", noteTypeFields);
		availableNoteTypes.put("Basic", basicNoteTypeFields);
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intent_notes_adder);
        final Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String[] defaultNoteTypeFields = chooseDefaultNoteTypeFields((String) extras.get("DEFAULT_NOTE_TYPE"));
        intentNotes = (ArrayList<HashMap<String, Serializable>>) extras.get("NOTES");
        HashMap<String, Serializable> fistNote = intentNotes.get(0);
	showNotePreview(defaultNoteTypeFields, fistNote);
	TextView textSureAdd = (TextView) findViewById(R.id.text_sure_add);
	int newCardCount = intentNotes.size();
	res.getQuantityString(R.plurals.sure_add_cards, newCardCount, newCardCount);
	textSureAdd.setText(intentNotes.size()+ " new notes will be added. Continue?");
        mCol = AnkiDroidApp.getCol();

    }


    private void reloadCollection() {
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_OPEN_COLLECTION, new DeckTask.TaskListener() {

            @Override
            public void onPostExecute(DeckTask.TaskData result) {
                if (mOpenCollectionDialog.isShowing()) {
                    try {
                        mOpenCollectionDialog.dismiss();
                    } catch (Exception e) {
                        Log.e(AnkiDroidApp.TAG, "onPostExecute - Dialog dismiss Exception = " + e.getMessage());
                    }
                }
                mCol = result.getCollection();
                if (mCol == null) {
                    finish();
                } else {
                    initActivity(AnkiDroidApp.getCol());
                }
            }


            @Override
            public void onPreExecute() {
                mOpenCollectionDialog = StyledOpenCollectionDialog.show(CreateFlashcard.this,
                        res.getString(R.string.open_collection), new OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface arg0) {
                                finish();
                            }
                        });
            }


            @Override
            public void onProgressUpdate(DeckTask.TaskData... values) {
            }
        }, new DeckTask.TaskData(AnkiDroidApp.getCurrentAnkiDroidDirectory() + AnkiDroidApp.COLLECTION_PATH));
    }

	private void showNotePreview(String[] defaultNoteTypeFields, HashMap<String, Serializable> fistNote) {
		String html = genHtmlFromIntentNote(fistNote, defaultNoteTypeFields);
	    final WebView webView = (WebView) findViewById(R.id.create_flashcard);
        webView.loadDataWithBaseURL("", html, "text/html", "utf-8", null);
	}

	private String[] chooseDefaultNoteTypeFields(String intentNoteType) {
		/*
		 * Fist choice = Note type coming from intent
		 * Second choice = The default note type of this application
		 */
		if (!availableNoteTypes.containsKey(intentNoteType)) {
			Toast.makeText(this, R.string.could_not_find_notetype + intentNoteType + "\n" + R.string.falling_back_to_default,
					Toast.LENGTH_SHORT).show();
			return availableNoteTypes.get(DEFAULT_NOTETYPE);
		}
		return availableNoteTypes.get(intentNoteType);
	}

    private void initActivity(Collection col) {

    }

	private Note createNote(JSONObject model){
		return new Note(mCol, model);
	}

	public String genHtmlFromIntentNote(HashMap<String, Serializable> intentNote, String[] noteTypeFields) {
		StringBuilder html =  new StringBuilder().append("<html><body>");
		html.append("<p>").append(noteTypeFields[0]).append(": ").append(intentNote.get("SOURCE_TEXT"));
		html.append("<p>").append(noteTypeFields[1]).append(": ").append(intentNote.get("TARGET_TEXT"));
		String[] optionalParameters = (String[]) intentNote.get("OPTIONAL_PARAMETERS");

		if (optionalParameters == null) {
			return html.append("</body></html>").toString();
		}
		for (int i = 2; i < noteTypeFields.length; i++) {
			String fieldContent = optionalParameters[i - 2];
			if (fieldContent == null) {
				fieldContent = "";
			}
			html.append("<p>").append(noteTypeFields[i]).append(": ").append(fieldContent);
		}
		return html.append("</body></html>").toString();
	}

}

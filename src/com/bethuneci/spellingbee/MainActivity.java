package com.bethuneci.spellingbee;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;

//For accessing, reading, and drawing images and reading files
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.graphics.drawable.Drawable;
import android.content.res.AssetManager;

//For randomizing words
import java.util.Random;

//Exceptions
import java.io.IOException;
import android.util.Log;

//Widget classes
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.TextView.OnEditorActionListener;

//For time keeping
import android.os.SystemClock;

//Text to speech
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

//To implement 1 delays
import android.os.Handler;

//Used to build an alert dialog (when the game is over)
import android.app.AlertDialog;
import android.content.DialogInterface;

//Used to load and play sounds effects, and control volume
import android.media.SoundPool;
import android.media.AudioManager;

//Used to provide Settings menu at top-right of app
import android.view.Menu;
import android.view.MenuItem;

/* 
 * Author: Kent Chow
 * 
 * Date: May 27 2013
 * 
 * Description: This class simulates a spelling bee application. It contains lists of various
 * words for the user to spell. The app randomly picks a word and asks the user to spell it.
 * The app also have text to speech and images to assist the user in spelling each word. The
 * app also lets the user choose their difficulty of words and challenges. Challenges are timed
 * sessions in which the user tries to spell as many words as possible. Difficulty ranges for 
 * users ages 5 to 12. To encourage users to spell, a score is obtained by spelling words.
*/

public class MainActivity extends Activity{
	
    private String[] wordList;
    private String currentWord;
    private int numberOfWords;
    private int currentIndex;
    private int wordsSpelled = 0;
    private int hintsUsed;
    private int timeLimit;
    private long score = 0;
    private long timedModeStartTime;
    private boolean ttsInitialized = false;
    private boolean timedMode = false;
    private TableLayout backgroundTableLayout;
    
    // App widgets
    private TextView currentWordTextView;
    private TextView wordsSpelledTextView;
    private TextView scoreTextView;
    private EditText enterWordEditText;
    private Button enterWordButton;
    private Button pronounceButton;
    private Button nextButton;
    private Button hintButton;
    private ImageView wordImageView;
    private Chronometer scoreChronometer;
    
    private TextToSpeech textToSpeech;
    
    // Used to play sound effects
    private SoundPool soundPool;
    private int right_sound_id;
    private int wrong_sound_id;
 
    private Random randomGenerator = new Random();
    private AssetManager assets;
    
    //File and number constants
    private final String ALL_WORDS = "list.txt";
    private final String YEAR5_6 = "Year5-6.txt";
    private final String YEAR7_8 = "Year7-8.txt";
    private final String YEAR9_10 = "Year9-10.txt";
    private final String YEAR11_12 = "Year11-12.txt";
    private final int TIME_MINUTE_1 = 1;
    private final int TIME_MINUTE_3 = 3;
    private final int TIME_MINUTE_5 = 5;
    private final int REGULAR_BACKGROUND_COLOUR = 0xFFF5F5F5;
    private final int TIMED_BACKGROUND_COLOUR = 0xFFE0FFFF;
    
    /* 
	 * Method initiated once app is launched. Setups necessary widgets and handlers.
	*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);	
		
		//Reference each widget
		currentWordTextView = (TextView) findViewById(R.id.currentWordText);
		scoreTextView = (TextView) findViewById(R.id.scoreTextView);
		wordsSpelledTextView = (TextView) findViewById(R.id.questionTextView);
		wordImageView = (ImageView) findViewById(R.id.wordImageView);
		
		//Reference tableLayout to change background colour
		backgroundTableLayout = (TableLayout)findViewById(R.id.tableLayout);
		backgroundTableLayout.setBackgroundColor(REGULAR_BACKGROUND_COLOUR);
		
		//Reference Chronometer and create Listener
		scoreChronometer = (Chronometer) findViewById(R.id.scoreChronometer);
		scoreChronometer.setOnChronometerTickListener(new OnChronometerTickListener() {
			/* 
			 * Handles the chronometer while it is running. Checks if timed mode is on.
			*/
            @Override
            public void onChronometerTick(Chronometer chronometer) {
            	//If time mode is on, change background colour, and reset app for timed mode
            	if (timedMode) {
            		int seconds = (int)((SystemClock.elapsedRealtime() - scoreChronometer.getBase())/1000);
            		//Checks if timed mode is over, and resets app back to normal mode
            		//Stops timer, reset stats, and display stats for timed mode
            		if (seconds > timeLimit * 60) {
            			timedMode = false;
            			textToSpeech.speak("Timed challenge complete", TextToSpeech.QUEUE_FLUSH, null);
            			scoreChronometer.stop();
            			displayStats();
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                    			resetStats();
                    			backgroundTableLayout.setBackgroundColor(REGULAR_BACKGROUND_COLOUR);
                            }
                        }, 5000);
            		}
            	}
            }
        });
		
		//Reference Buttons and set Listeners
		enterWordButton = (Button) findViewById(R.id.enterButton);		
		enterWordButton.setOnClickListener(new buttonListener());
		
		pronounceButton = (Button) findViewById(R.id.pronounceButton);	
		pronounceButton.setOnClickListener(new buttonListener());
		
		nextButton = (Button) findViewById(R.id.nextButton);		
		nextButton.setOnClickListener(new buttonListener());
		
		hintButton = (Button) findViewById(R.id.hintButton);		
		hintButton.setOnClickListener(new buttonListener());
		
		//Reference user enterWordEditText and makes sure it is always focused
		enterWordEditText = (EditText) findViewById(R.id.inputEditText);
		enterWordEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		enterWordEditText.setSingleLine();	
		enterWordEditText.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_DONE) {
		        	enterWordEditText.requestFocus();
		            return true;
		        }
		        else {
		            return false;
		        }
		    }
		});
		
		//Initiate sounds
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		right_sound_id = soundPool.load(this, R.raw.right, 1);
		wrong_sound_id = soundPool.load(this, R.raw.wrong, 1);
		
		//Set default word list
		setWordList(ALL_WORDS);
		
		//Get assets
		assets = getAssets();
		
		//Initiate text to speech
		textToSpeech = new TextToSpeech(this, new initiateTextToSpeech());
		
		//Pick random word to spell
        changeCurrentWord();
	}
	
	/* 
	 * Creates menu for the app. Menu includes options for difficulty, challenges and reset stats.
	*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.difficulty_menu);
	    menu.add(Menu.NONE, Menu.FIRST+1, Menu.NONE, R.string.challenge_menu);
	    menu.add(Menu.NONE, Menu.FIRST+2, Menu.NONE, R.string.reset_menu);
	    return true;
	}
	
	/* 
	 * Handles user input when menu is selected. Determines which item in each menu is selected
	 * and calls necessary methods.
	*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    if (item.getItemId() == Menu.FIRST) {
	        AlertDialog.Builder choicesBuilder = new AlertDialog.Builder(this);
	        choicesBuilder.setTitle("Select Difficulty");
	        //Create menu items and display onto screen
	        choicesBuilder.setItems(R.array.difficulty, 
	                new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int item) {
	                    	//Determines which item from menu is selected
	                    	//Set wordList according to item selected
	                    	if (item == 0)
	                    		setWordList(ALL_WORDS);
	                    	else if (item == 1)
	                    		setWordList(YEAR5_6);
	                    	else if (item == 2)
	                    		setWordList(YEAR7_8);
	                    	else if (item == 3)
	                    		setWordList(YEAR9_10);
	                    	else if (item == 4)
	                    		setWordList(YEAR11_12);
	                    	resetStats();
	                    }
	                }
	        );
	        //Create the dialog and show it
	        AlertDialog choicesDialog = choicesBuilder.create();
	        choicesDialog.show();
	        return true;
	    }
	    else if ((item.getItemId() == Menu.FIRST+1)) {
	    	AlertDialog.Builder choicesBuilder = new AlertDialog.Builder(this);
	        choicesBuilder.setTitle("Select Challenge");
	        //Create menu items and display onto screen
	        choicesBuilder.setItems(R.array.challenges, 
	                new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int item) {
	                    	//Determines which item from menu is selected
	                    	//Set time limit according to item selected
	                    	if (item == 0)
	                    		timeLimit = TIME_MINUTE_1;
	                    	else if (item == 1)
	                    		timeLimit = TIME_MINUTE_3;
	                    	else if (item == 2)
	                    		timeLimit = TIME_MINUTE_5;
	                    	textToSpeech.speak("Timed challenge "+timeLimit+" minute limit", TextToSpeech.QUEUE_FLUSH, null);
	                    	resetStats();
	                    	timedMode = true;
	                    	scoreChronometer.setBase(SystemClock.elapsedRealtime());
	                    	timedModeStartTime = SystemClock.elapsedRealtime();
	                    	backgroundTableLayout.setBackgroundColor(TIMED_BACKGROUND_COLOUR);
	                    }
	                }
	        );
	        //Create the dialog and show it
	        AlertDialog choicesDialog = choicesBuilder.create();
	        choicesDialog.show();
	        return true;
	
	    }
	    else if ((item.getItemId() == Menu.FIRST+2)) {
	    	resetStats();
	    	return true;
	    }	    	
	    return false;
	}
	
	/* 
	 * Private helper method that reads a textfile and loads words into an array.
	 * Method is called each time user changes difficulty settings. Accepts a file
	 * name as a parameter.
	*/
	private void setWordList(String file_name) {
        try
        {
        	//Assign numberOfWords and wordList
            numberOfWords = readNumberOfLines(file_name);
            wordList = new String[numberOfWords];
            InputStream inputStream = getAssets().open(file_name);
            BufferedReader textReader = new BufferedReader(new InputStreamReader(inputStream));    
            for (int i = 0; i < numberOfWords; i++) {
                wordList[i] = textReader.readLine();
            }
            textReader.close();
        }
        catch (IOException e)
        {
        	Log.e("Spelling Bee", "Error Loading File", e);
        }
	}
		
	/* 
	 * Private helper method that reads a textfile and counts the number of lines.
	 * Returns an integer that represents the number of lines in a file. Accepts
	 * a file name as a parameter. No returns.
	*/
    private int readNumberOfLines(String file_name) throws IOException
    {
        int numberOfLines = 0;
        String line;
        InputStream inputStream = getAssets().open(file_name);
        BufferedReader textReader = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = textReader.readLine()) != null) {
            numberOfLines++;
        }
        textReader.close();
        return numberOfLines;
    }
    
	/* 
	 * Private helper method that randomly picks a new word for the user to spell.
	 * Calls methods to update images, chronometer, and text widgets. No returns
	 * and no parameters.
	*/
    private void changeCurrentWord()
    {
    	//Randomly generate an index to select word
    	int index = randomGenerator.nextInt(numberOfWords);
    	while (index == currentIndex) {
    		index = randomGenerator.nextInt(numberOfWords);
    	}
    	currentIndex = index;
        currentWord = wordList[currentIndex];
        enterWordEditText.setText("");
    	currentWordTextView.setText("");
    	//Update the image according to word
        updateImage();
        if (ttsInitialized)
        	textToSpeech.speak("Spell the word " + currentWord, TextToSpeech.QUEUE_ADD, null);
        hintsUsed = 0;
        //If in timed mode, adjust chronometer accordingly
        if (timedMode != true) {
        	scoreChronometer.setBase(SystemClock.elapsedRealtime());
        }
        //Set the chronometer to start counting
        scoreChronometer.start();
    }
  	
	/* 
	 * Private helper method processes user input. Compares user's input and determines
	 * if input is correct. Updates currentWordTextView widget and plays sound according
	 * to result. If user input is correct, program is delayed for 1 second until next 
	 * word is picked. No returns and no paramters.
	*/
  	private void submitAnswer()
  	{
  		String input = enterWordEditText.getText().toString();
  		//Edit out whitespaces from input and display input on screen
  		currentWordTextView.setText(input.trim());
  		//Checks if user's input is correct
  		if (input.trim().equalsIgnoreCase(currentWord)) {
  			currentWordTextView.setTextColor(getResources().getColor(R.color.correct_answer));
  			scoreChronometer.stop();
  			soundPool.play(right_sound_id, 1.0f, 1.0f, 1, 0, 1.0f);
  			//Create a handler for delay of 1 second and then call update methods
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                	updateScore();
                	changeCurrentWord();
                	wordsSpelled++;
                	wordsSpelledTextView.setText("Spelled: "+ wordsSpelled);
                }
            }, 1000);
  		}
  		else {
  			currentWordTextView.setTextColor(getResources().getColor(R.color.wrong_answer));
  	        enterWordEditText.setText("");
  	        soundPool.play(wrong_sound_id, 1.0f, 1.0f, 1, 0, 1.0f);
  		}
  	}  	
  	
	/* 
	 * Private helper method that updates the image according to currentWord. If 
	 * no image associated with word is found, a default image is picked instead.
	 * No returns and no parameters.
	*/
  	private void updateImage()
  	{	
  		InputStream stream;
  		//Try to load image for the current word
  	    try {
  	        stream = assets.open("images/"+currentWord+".jpg");
  	        Drawable image = Drawable.createFromStream(stream, currentWord);
  	        wordImageView.setImageDrawable(image);
  	    }
  	    catch (IOException e) {
  	    	//If no image is found, try to load default image
  	    	try {
  	        stream = assets.open("images/no_image.jpg");
  	        Drawable image = Drawable.createFromStream(stream, currentWord);
  	        wordImageView.setImageDrawable(image);
  	    	}
  	    	catch (IOException error) {
  	    		Log.e("Spelling Bee", "Error Loading File", error);
  	    	}
  	    }
  	}
  	
	/*
	 * Private helper method that implements OnInitListener. Used for determining if text to speech
	 * initiated correctly and sets up text to speech properties.
	*/
  	private class initiateTextToSpeech implements OnInitListener {;
  		@Override
  		public void onInit(int initStatus) {
  			if (initStatus == TextToSpeech.SUCCESS) {
  				textToSpeech.setSpeechRate((float)0.8);
  				textToSpeech.speak("Spell the word " + currentWord, TextToSpeech.QUEUE_FLUSH, null);
  				ttsInitialized = true;
  			}
  		}
  	}
  	
	/*
	 * Private helper method that implements OnClickListener. Used for handling button clicks.
	*/
  	private class buttonListener implements OnClickListener {
  	    @Override
  	    public void onClick(View v) {
  	    	if (v.getId() == R.id.enterButton)
  	    		submitAnswer();
  	    	else if (v.getId() == R.id.pronounceButton)
  	    		textToSpeech.speak(currentWord, TextToSpeech.QUEUE_FLUSH, null);
  	    	else if (v.getId() == R.id.nextButton)
  	    		changeCurrentWord();
  	    	else if (v.getId() == R.id.hintButton)
  	    		displayHint();
  	    }
  	};
  	
	/*
	 * Private helper method that instantiates an alert dialog and displays a hint for the user.
	 * The type of hint displayed is based on the amount hints the user previously used. No returns
	 * and no parameters.
	*/
  	private void displayHint() {
  		//Build a dialog text
  		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hint");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
           }
        });
        
        //Choose which hint to select
        if (hintsUsed == 0)
            builder.setMessage("The length of the word is " + currentWord.length());
        else if (hintsUsed > currentWord.length())
        	builder.setMessage("No more hints!");
        else
        	builder.setMessage("The number "+hintsUsed+" letter of the word is "+currentWord.charAt(hintsUsed-1));
        hintsUsed++;
        
        //Create and show dialog
        AlertDialog hintDialog = builder.create();
        hintDialog.show();
  	}
  	
	/*
	 * Private helper method that updates all user statistics. No returns and no parameters.
	*/
  	private void updateScore() {
  		int seconds;
  		//Based on whether timed mode is on, update the chronometer accordingly
  		if (timedMode != true) {
	  		seconds = (int)((SystemClock.elapsedRealtime() - scoreChronometer.getBase())/1000);
  		}
  		else {
	  		seconds = (int)((SystemClock.elapsedRealtime() - timedModeStartTime)/1000);
	  		timedModeStartTime = SystemClock.elapsedRealtime();
  		}
  		//Calculate the score and display it
  		int actualScore = (currentWord.length()*10 - (seconds + hintsUsed*10));
  		if (actualScore < 0)
  			actualScore = 0;
  		score = score + actualScore; 		
    	scoreTextView.setText("Score: " + score);
  	}
  	
	/*
	 * Private helper method that resets all user statistics. No returns and no parameters.
	*/
  	private void resetStats() {
  		wordsSpelled = 0;
  		score = 0;
  		currentIndex = -1;
  		scoreTextView.setText("Score: ");
  		wordsSpelledTextView.setText("Spelled: ");
        timedMode = false;
        backgroundTableLayout.setBackgroundColor(REGULAR_BACKGROUND_COLOUR);
  		changeCurrentWord();
  	}
  	
	/* 
	 * Private helper method that instantiates an alert dialog and displays user statistics.
	*/
  	private void displayStats() {
  		//Create text for dialog
  		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Statistics");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
           }
        });
        if (wordsSpelled == 0){
        	builder.setMessage("Your score: "+score +"\n"+"You spelled 0 words in a "+timeLimit+" minute time limit\n"+
            		"You took an average of "+(timeLimit*60)+" seconds to spell each word");
        }
        else {
        	builder.setMessage("Your score: "+score +"\n"+"You spelled "+wordsSpelled+" words in a "+timeLimit+" minute time limit");
        }
        //Create the dialog and show it
        AlertDialog displayDialog = builder.create();
        displayDialog.show();
  	}
}

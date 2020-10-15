/* Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.engedu.wordstack;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    private static int WORD_LENGTH = 3;
    public static final int LIGHT_BLUE = Color.rgb(176, 200, 255);
    public static final int LIGHT_GREEN = Color.rgb(200, 255, 200);
    private Random random = new Random();
    private StackedLayout stackedLayout;
    private Stack<LetterTile> placedTiles;
    private String word1, word2;
    private LinearLayout word1LinearLayout, word2LinearLayout;
    private Button undoButton;
    private HashMap<Integer, List<String>> map = new HashMap<>();

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("words.txt");
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while((line = in.readLine()) != null) {
                String word = line.trim();
                int n = word.length();

                List<String> a = map.getOrDefault(n, new ArrayList<String>());
                a.add(word);
                map.put(n, a);

            }

        } catch (IOException e) {
            Toast toast = Toast.makeText(this, "Could not load dictionary", Toast.LENGTH_LONG);
            toast.show();
        }

        undoButton = findViewById(R.id.undoButton);

        LinearLayout verticalLayout = (LinearLayout) findViewById(R.id.vertical_layout);
        stackedLayout = new StackedLayout(this);
        /*
        *
        *   Change the parameters of stack tiles to default size without which it will
        *   be constraint free and will overflow while executing.
        *
        * */
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(LetterTile.TILE_SIZE, LetterTile.TILE_SIZE);
        stackedLayout.setLayoutParams(params);
        verticalLayout.addView(stackedLayout, 3);

        placedTiles = new Stack<>();

        word1LinearLayout = findViewById(R.id.word1);
        //word1LinearLayout.setOnTouchListener(new TouchListener());
        word1LinearLayout.setOnDragListener(new DragListener());
        word2LinearLayout = findViewById(R.id.word2);
        //word2LinearLayout.setOnTouchListener(new TouchListener());
        word2LinearLayout.setOnDragListener(new DragListener());
        stackedLayout.setOnDragListener(new DragListener());
    }

    private String shuffle(String word1, String word2) {

        StringBuilder result = new StringBuilder();

        int p = 0, q = 0;

        while(p<word1.length() && q<word2.length()){
            if(random.nextBoolean()){
                result.append(word1.charAt(p));
                p++;
            }else{
                result.append(word2.charAt(q));
                q++;
            }
        }

        if(p<word1.length()){
            result.append(word1.substring(p));
        }
        if(q<word2.length()){
            result.append(word2.substring(q));
        }

        return result.toString();

    }

    private class TouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN && !stackedLayout.empty()) {
                LetterTile tile = (LetterTile) stackedLayout.peek();
                placedTiles.push(tile);
                tile.moveToViewGroup((ViewGroup) v);
                if (stackedLayout.empty()) {
                    TextView messageBox = (TextView) findViewById(R.id.message_box);
                    messageBox.setText(word1 + " " + word2);
                    undoButton.setEnabled(false);
                }
                return true;
            }
            return false;
        }
    }

    private class DragListener implements View.OnDragListener {

        public boolean onDrag(View v, DragEvent event) {
            int action = event.getAction();
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    v.setBackgroundColor(LIGHT_BLUE);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundColor(LIGHT_GREEN);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundColor(LIGHT_BLUE);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundColor(Color.WHITE);
                    v.invalidate();
                    return true;
                case DragEvent.ACTION_DROP:
                    // Dropped, reassign Tile to the target Layout
                    LetterTile tile = (LetterTile) event.getLocalState();
                    if(v instanceof StackedLayout){
                    // if the view is dragged to stackedLayout
                        onUndo(v);
                        if(!placedTiles.empty())    placedTiles.peek().unfreeze();
                        return true;
                    }

                    if(!placedTiles.empty() && !placedTiles.peek().equals(tile)){
                        placedTiles.peek().freeze();
                        placedTiles.push(tile);
                    }else if(placedTiles.empty()){
                        placedTiles.push(tile);
                    }

                    /*
                    *
                    * check if the tile is being placed in the stackLayout or word1Layout
                    * then if the tile is placed back in stackLayout then unfreeze the last tile place and where it was
                    * if it is placed in wordLayout then check if it is already placed(i.e., if it has came from wordLayout or stackLayout)
                    *
                    * if it came from wordLayout then do not unfreeze or freeze anything
                    * if it came from stackLayout then freeze the placedTiled.peek() and unfreeze the current after placing
                    *
                    * */

                    tile.moveToViewGroup((ViewGroup) v);
                    placedTiles.peek().unfreeze();

                    if (stackedLayout.empty()) {
                        TextView messageBox = (TextView) findViewById(R.id.message_box);
                        messageBox.setText(word1 + " " + word2);
                        undoButton.setEnabled(false);
                        checkSolution();
                    }
                    return true;
            }
            return false;
        }
    }

    private void checkSolution() {

        StringBuilder w1 = new StringBuilder();
        for(int i = 0; i<word1LinearLayout.getChildCount(); i++){
            View v = word1LinearLayout.getChildAt(i);

            if(v instanceof LetterTile){
                Character c = ((LetterTile)v).getLetter();
                w1.append(c);
            }
        }
        StringBuilder w2 = new StringBuilder();
        for(int i = 0; i<word2LinearLayout.getChildCount(); i++){
            View v = word2LinearLayout.getChildAt(i);

            if(v instanceof LetterTile){
                Character c = ((LetterTile)v).getLetter();
                w2.append(c);
            }
        }

        Log.i("word got", w1 + " " + w2);

        StringBuilder s;

        if((w1.toString().equals(word1)|| w1.toString().equals(word2)) && (w2.toString().equals(word1)|| w2.toString().equals(word2))){
            s = new StringBuilder("Correct Solution");
        }else if(map.get(w1.length()).contains(w1.toString()) && map.get(w2.length()).contains(w2.toString())){
            s = new StringBuilder("Great Work, You found another similar words");
        }else{
            s = new StringBuilder("You Lose :(");
        }
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();

    }

    public boolean onStartGame(View view) {
        TextView messageBox = (TextView) findViewById(R.id.message_box);
        messageBox.setText("Game started");
        word1LinearLayout.removeAllViews();
        word2LinearLayout.removeAllViews();
        stackedLayout.removeAllViews();
        placedTiles.clear();
        stackedLayout.clear();
        undoButton.setEnabled(true);

        word1 = map.get(WORD_LENGTH).get(random.nextInt(map.size()));
        word2 = map.get(WORD_LENGTH).get(random.nextInt(map.size()));

        String shuffledWord = shuffle(word1, word2);

        Log.i("worDs", shuffledWord + " " + word1 + word2);

        for(int i = shuffledWord.length()-1; i>=0; i--){
            LetterTile tile = new LetterTile(getApplicationContext(), shuffledWord.charAt(i));
            stackedLayout.push(tile);
        }

        WORD_LENGTH++;

        return true;
    }

    public boolean onUndo(View view) {
        /**
         **
         **  YOUR CODE GOES HERE
         **
         **/

        if(placedTiles.isEmpty())   return false;

        LetterTile tile = placedTiles.pop();
        tile.moveToViewGroup(stackedLayout);

        return true;
    }
}

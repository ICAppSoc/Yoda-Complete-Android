package uk.co.icappsoc.yodafull;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Converts English to Yodish via network connection to YodaSpeak.
 */
public class YodaFragment extends Fragment {

    /** The view that displays Yodish output. */
    TextView yodishText;

    /* Called when the UI is ready to be created. You return the root View for this Fragment. */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        // Get a reference to the TextView used to display Yodish
        yodishText = (TextView) rootView.findViewById(R.id.yodish_text);

        // Get references to the English input and convert button to add logic when the button is clicked
        final EditText englishText = (EditText) rootView.findViewById(R.id.english_text);

        Button convertButton = (Button) rootView.findViewById(R.id.convert_button);
        convertButton.setOnClickListener(new View.OnClickListener() {
            // Called as soon as the button is clicked
            @Override
            public void onClick(View v) {
                // Start a new ConvertToYodishTask,
                // passing it the text we want to convert as a parameter to execute()
                ConvertToYodishTask convertTask = new ConvertToYodishTask();
                convertTask.execute(englishText.getText().toString());
            }
        });

        return rootView;
    }

    public class ConvertToYodishTask extends AsyncTask<String, Void, String> {
        private final String LOG_TAG = ConvertToYodishTask.class.getSimpleName();

        // Called before doInBackground(). Runs on the UI thread.
        @Override
        protected void onPreExecute() {
            yodishText.setText(R.string.yoda_thinking);
        }

        // Runs on a background thread.
        @Override
        protected String doInBackground(String... params) {

            if(params.length < 1){
                // We do not have the necessary input!
                return null;
            }
            // Assumes first string is the entire input.
            String englishIn = params[0];

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the Yodish response as a string.
            String yodishOut = null;

            try {
                // Construct the URL for the YodaSpeak query.
                // The API takes a single parameter, named 'sentence'.
                Uri builtUri = Uri.parse("https://yoda.p.mashape.com/yoda").buildUpon()
                        .appendQueryParameter("sentence", englishIn)
                        .build();
                URL url = new URL(builtUri.toString());

                // Create the request to YodaSpeak, and open the connection.
                // The API requires a header with a Mashape key such that it can track users.
                // Ideally, use your own; if everyone uses mine, it may be restricted and / or I may
                // revoke the key in the future.
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("X-Mashape-Key",
                        "F9v1zCq18Pmshx40bfpKvhiEYzmPp1Zw4najsnVsMGMy1R6VOR");
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String.
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                yodishOut = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code did not manage to get in touch with Yoda, there's no point in
                // displaying the output.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            return yodishOut;
        }

        // Called after and with the result of doInBackground(). Runs on the UI thread.
        @Override
        protected void onPostExecute(String result){
            if(null != result){
                yodishText.setText(result);
            } else {
                yodishText.setText(R.string.yoda_prompt);
            }
        }

    }
}

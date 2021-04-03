package edu.temple.bookshelf;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class BookSearchActivity extends AsyncTask<Void,Void,Void> {

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            URL coverURL = new URL("https://kamorris.com/lab/cis3515/search.php?term=search_term");

            HttpURLConnection httpURLConnection = (HttpURLConnection) coverURL.openConnection();
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            While(line != null){
                line = bufferedReader.readLine();
            }
        } catch (MalformedURLException e){
                e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
            return null;

    }

}

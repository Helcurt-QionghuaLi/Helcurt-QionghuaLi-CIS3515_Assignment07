package edu.temple.bookshelf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.FileNameMap;
import java.util.logging.Handler;


public class MainActivity<Intent, ServiceConnection> extends AppCompatActivity implements BookListFragment.BookSelectedInterface, ControlFragment.ControlInterface {

    EditText textBox;
    CheckBox checkBox;
    boolean autoSave;

    SharedPreferences preferences;

    String internalFilename = "myfile";
    File file;

    LocationManager locationManager;
    LocationListener locationListener;
    Location previousLocation;
    FragmentManager fm;

    private boolean twoPane;
    private  BookDetailsFragment bookDetailsFragment;
    private  ControlFragment controlFragment;
    private Book selectedBook, playingBook;

    private final String TAG_BOOKLIST = "booklist", TAG_BOOKDETAILS = "bookdetails";
    private final String KEY_SELECTED_BOOK = "selectedBook", KEY_PLAYING_BOOK = "playingBook";
    private final String KEY_BOOKLIST = "searchedook";
    private final int BOOK_SEARCH_REQUEST_CODE = 123;

    private AudiobookService.MediaControlBinder mediaControl;
    private boolean serviceConnected;

    Intent serviceIntent;

    BookList bookList;

    Handler progressHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            // Don't update contols if we don't know what bok the service is playing
            if (message.obj != null && playingBook != null) {
                controlFragment.updateProgress((int) (((float) ((AudiobookService.BookProgress) message.obj).getProgress() / playingBook.getDuration()) * 100));
                controlFragment.setNowPlaying(getString(R.string.now_playing, playingBook.getTitle()));
            }

            return true;
        }
    });

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mediaControl = (AudiobookService.MediaControlBinder) iBinder;
            mediaControl.setProgressHandler(progressHandler);
            serviceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceConnected = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceIntent = new Intent (this, AudiobookService.class);

        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        fm = getSupportFragmentManager();

        findViewById(R.id.searchDialogButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, BookSearchActivity.class), BOOK_SEARCH_REQUEST_CODE);
            }
        });

        if (savedInstanceState != null) {
            // Fetch selected book if there was one
            selectedBook = savedInstanceState.getParcelable(KEY_SELECTED_BOOK);

            // Fetch playing book if there was one
            playingBook = savedInstanceState.getParcelable(KEY_PLAYING_BOOK);

            // Fetch previously searched books if one was previously retrieved
            bookList = savedInstanceState.getParcelable(KEY_BOOKLIST);
        }else {
            // Create empty booklist if
            bookList = new BookList();
        }

        twoPane = findViewById(R.id.container2) != null;

        Fragment fragment1;
        fragment1 = fm.findFragmentById(R.id.container_1);

        // I will only ever have a single ControlFragment - if I created one before, reuse it
        if ((controlFragment = (ControlFragment) fm.findFragmentById(R.id.control_container)) == null) {
            controlFragment = new ControlFragment();
            fm.beginTransaction()
                    .add(R.id.control_container, controlFragment)
                    .commit();
        }


        // At this point, I only want to have BookListFragment be displayed in container_1
        if (fragment1 instanceof BookDetailsFragment) {
            fm.popBackStack();
        } else if (!(fragment1 instanceof BookListFragment))
            fm.beginTransaction()
                    .add(R.id.container_1, BookListFragment.newInstance(bookList), TAG_BOOKLIST)
                    .commit();

        /*
        If we have two containers available, load a single instance
        of BookDetailsFragment to display all selected books
         */
        bookDetailsFragment = (selectedBook == null) ? new BookDetailsFragment() : BookDetailsFragment.newInstance(selectedBook);
        if (twoPane) {
            fm.beginTransaction()
                    .replace(R.id.container2, bookDetailsFragment, TAG_BOOKDETAILS)
                    .commit();
        } else if (selectedBook != null) {
            /*
            If a book was selected, and we now have a single container, replace
            BookListFragment with BookDetailsFragment, making the transaction reversible
             */
            fm.beginTransaction()
                    .replace(R.id.container_1, bookDetailsFragment, TAG_BOOKDETAILS)
                    .addToBackStack(null)
                    .commit();
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 123);
        }
        locationManager = getSystemService(LocationManager.class);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (previousLocation !=null) {
                    float speed = location.distanceTo(previousLocation)/((location.getTime()- previousLocation.getTime()) * 1000);
                }
                previousLocation = location;
            }
        };

        private boolean checkGPSPermission() {
            return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED;
        }
        @Override
        protected void onStart() {
            super.onStart();
            if(check    Permission()) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        }

        @Override
        protected void onStop() {
            super.onStop();
            locationManager.removeUpdates(locationListener);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] premission, @NonNull int[] grantResults) {
            if (requestCode == 123) {
                if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                   finish();
                }
            }
        }
         file = new File (getFilesDir(), internalFilename);
        textBox = (EditText) findViewById((R.id.editText);
        checkBox = (CheckBox) findViewById(R.id.checkBox);

        autoSave = preferences.getBoolean(s:"autoSave", b:false);

        if (autoSave && file.exists()) {
            try{
                BufferedReader br = new BufferedReader(new FileReader(file));
                StringBuilder text = new StringBuilder();
                String line;
                while ((line ==br.readLine()) !=null){
                    text.append(line);
                    text.append('n');
                }
                br.close();
                textBox.setText(text.toString());
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoSave = isChecked;
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(s:"autoSave, autoSave");
            editor.apply();
        });
        textBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (autoSave) {
                    try{
                        FileOutputStream outputStream = new FileOutputStream(file);
                        outputStream.write(s.toString().getBytes());
                        outputStream.close();
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }else{
                    file.delete();
                }
            }
        });

    }

    @Override
    public void bookSelected(int index) {
        // Store the selected book to use later if activity restarts
        selectedBook = bookList.get(index);

        if (twoPane)
            /*
            Display selected book using previously attached fragment
             */
            bookDetailsFragment.displayBook(selectedBook);
        else {
            /*
            Display book using new fragment
             */
            fm.beginTransaction()
                    .replace(R.id.container_1, BookDetailsFragment.newInstance(selectedBook), TAG_BOOKDETAILS)
                    // Transaction is reversible
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * Display new books when retrieved from a search
     */
    private void showNewBooks() {
        if ((fm.findFragmentByTag(TAG_BOOKDETAILS) instanceof BookDetailsFragment)) {
            fm.popBackStack();
        }
        ((BookListFragment) fm.findFragmentByTag(TAG_BOOKLIST)).showNewBooks();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SELECTED_BOOK, selectedBook);
        outState.putParcelable(KEY_PLAYING_BOOK, playingBook);
        outState.putParcelable(KEY_BOOKLIST, bookList);
    }

    @Override
    public void onBackPressed() {
        // If the user hits the back button, clear the selected book
        selectedBook = null;
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BOOK_SEARCH_REQUEST_CODE && resultCode == RESULT_OK) {
            bookList.clear();
            bookList.addAll((BookList) data.getParcelableExtra(BookSearchActivity.BOOKLIST_KEY));
            if (bookList.size() == 0) {
                Toast.makeText(this, getString(R.string.error_no_results), Toast.LENGTH_SHORT).show();
            }
            showNewBooks();
        }
    }

    @Override
    public void play() {
        if (selectedBook != null) {
            playingBook = selectedBook;
            controlFragment.setNowPlaying(getString(R.string.now_playing, playingBook.getTitle()));
            if (serviceConnected) {
                mediaControl.play(selectedBook.getId());
            }

            // Make sure that the service doesn't stop
            // if the activity is destroyed while the book is playing
            startService(serviceIntent);
        }
    }

    @Override
    public void pause() {
        if (serviceConnected) {
            mediaControl.pause();
        }
    }

    @Override
    public void stop() {
        if (serviceConnected)
            mediaControl.stop();

        // If no book is playing, then it's fine to let
        // the service stop once the activity is destroyed
        stopService(serviceIntent);
    }

    @Override
    public void changePosition(int progress) {
        if (serviceConnected)
            mediaControl.seekTo((int) ((progress / 100f) * playingBook.getDuration()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }
}

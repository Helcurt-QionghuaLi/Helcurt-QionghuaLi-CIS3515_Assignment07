package edu.temple.bookshelf;

import android.os.Parcel;
import android.os.Parcelable;

public class Book implements Parcelable{
    private String title;
    private String author;
    private int id;
    private int duration;
    private String coverURL;

    public Book(String title, String author, int id, int duration, String coverURL) {
        this.title = title;
        this.author = author;
        this.id = id;
        this.duration = duration;
        this.coverURL = coverURL;
    }

    protected Book(Parcel in) {
        title = in.readString();
        author = in.readString();
        id = in.readInt();
        duration = in.readInt();
        coverURL = in.readString();
    }

    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel in) {

            return new Book(in);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

    public String getTitle() {

        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {

        return author;
    }

    public void setAuthor(String author) {

        this.author = author;
    }

    public int getId() {

        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration){
        this.duration = duration;
    }

    public String getCoverURL() {

        return coverURL;
    }

    public void setCoverURL(String coverURL) {

        this.coverURL = coverURL;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(duration);
        parcel.writeString(title);
        parcel.writeString(author);
        parcel.writeString(coverURL);
    }
}

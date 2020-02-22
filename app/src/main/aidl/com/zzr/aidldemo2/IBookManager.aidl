// IBookManager.aidl
package com.zzr.aidldemo2;
import com.zzr.aidldemo2.Book;
import com.zzr.aidldemo2.IOnNewBookArrivedListener;

// Declare any non-default types here with import statements

interface IBookManager {

    List<Book> getBookList();

    void addBook(in Book book);

    void registerListener(IOnNewBookArrivedListener listener);
    void unRegisterListener(IOnNewBookArrivedListener listener);
}

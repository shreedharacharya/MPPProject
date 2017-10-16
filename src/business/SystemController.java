package business;

import dataaccess.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SystemController implements ControllerInterface {
    public static Auth currentAuth = null;
    public static Action currentAction = null;

    public void login(String id, String password) throws LoginException {
        DataAccess da = new DataAccessFacade();
        HashMap<String, User> map = da.readUserMap();
        if (!map.containsKey(id)) {
            throw new LoginException("ID " + id + " not found");
        }
        String passwordFound = map.get(id).getPassword();
        if (!passwordFound.equals(password)) {
            throw new LoginException("Password incorrect");
        }
        currentAuth = map.get(id).getAuthorization();

    }

    @Override
    public void checkout(String memberId, String isbn) throws CheckException {
        DataAccess da = new DataAccessFacade();
        HashMap<String, LibraryMember> mapMember = da.readMemberMap();
        if (!mapMember.containsKey(memberId)) {
            throw new CheckException("Member with ID :" + memberId + " not found");
        }
        HashMap<String, Book> mapBook = da.readBooksMap();
        if (!mapBook.containsKey(isbn)) {
            throw new CheckException("Book with isbn: " + isbn + " is not found");
        }
        LibraryMember libraryMember = mapMember.get(memberId);
        Book book = mapBook.get(isbn);
        if (!book.isAvailable()) throw new CheckException(" Book is not available");

        int copyNo = book.AvailableCopyNo();
        BookCopy bookCopy = book.getCopy(copyNo);
        bookCopy.setAvailable(false);

        long checkoutLength = book.getMaxCheckoutLength();

        LocalDate checkOutDate = LocalDate.now();


        // update the checkout info of member
        List<CheckOutRecordEntry> checkOutRecordEntries = new ArrayList<>();
        ControllerInterface controller = new SystemController();
        List<CheckOutRecordEntry> checkOutRecordEntryList = controller.getAllCheckoutRecordEntry(memberId);
        if (checkOutRecordEntryList != null) {
            checkOutRecordEntries = checkOutRecordEntryList;
        }

        checkOutRecordEntries.add(new CheckOutRecordEntry(memberId, bookCopy, checkOutDate, checkOutDate.plusDays(checkoutLength), null));


        libraryMember.CheckOut(checkOutRecordEntries);
        mapMember.put(memberId, libraryMember);

        //List<LibraryMember> updatedList = getPreviousLibraryMemberAndUpdate(mapMember, libraryMember, memberId);

        // save the updated member;
        da.loadMemberMap(mapMember);

        book.updateCopies(bookCopy);
        // get the updated books
        mapBook.put(isbn, book);
        //save the updated book
        da.loadBookMap(mapBook);

    }

    @Override
    public void checkIn(String memberId, String isbn, int copyNo) throws CheckException {
        DataAccess da = new DataAccessFacade();
        HashMap<String, LibraryMember> mapMember = da.readMemberMap();
        if (!mapMember.containsKey(memberId)) {
            throw new CheckException("Member with ID :" + memberId + " not found");
        }
        HashMap<String, Book> mapBook = da.readBooksMap();
        if (!mapBook.containsKey(isbn)) {
            throw new CheckException("Book with isbn: " + isbn + " is not found");
        }


        LibraryMember libraryMember = mapMember.get(memberId);
        if (libraryMember.getCheckOutRecord() == null) throw new CheckException("Incorrect Info!!!");

        // update the checkout info of member
        List<CheckOutRecordEntry> checkOutRecordEntries = libraryMember.getCheckOutRecord().getCheckOutRecordEntryList();

        boolean checkedIn = false;
        Book book = mapBook.get(isbn);
        BookCopy returnedBookCopy = new BookCopy(book, copyNo, true);

        for (CheckOutRecordEntry checkOutRecordEntry : checkOutRecordEntries) {
            if (returnedBookCopy.equals(checkOutRecordEntry.getBookCopy()) &&
                    !checkOutRecordEntry.getBookCopy().isAvailable()) {
                checkOutRecordEntry.setReturnDate(LocalDate.now());
                checkOutRecordEntry.setBookCopy(returnedBookCopy);
                book.updateCopies(returnedBookCopy);
                checkedIn = true;
                break;
            }
        }

        if (!checkedIn) throw new CheckException("Incorrect Info!!! ");

        libraryMember.checkIn(checkOutRecordEntries);

        mapMember.put(memberId, libraryMember);

        // save the updated member;
        da.loadMemberMap(mapMember);

        mapBook.put(isbn, book);
        //save the updated book;
        da.loadBookMap(mapBook);

    }


    @Override
    public List<String> allMemberIds() {
        DataAccess da = new DataAccessFacade();
        List<String> retval = new ArrayList<>();
        retval.addAll(da.readMemberMap().keySet());
        return retval;
    }

    @Override
    public List<String> allBookIds() {
        DataAccess da = new DataAccessFacade();
        List<String> retval = new ArrayList<>();
        retval.addAll(da.readBooksMap().keySet());
        return retval;
    }

    @Override
    public List<CheckOutRecordEntry> getAllCheckoutRecordEntry(String memberId) {

        DataAccess da = new DataAccessFacade();
        HashMap<String, LibraryMember> mapMember = da.readMemberMap();
        if (!mapMember.containsKey(memberId)) {
            return null;
        }
        if (mapMember.get(memberId).getCheckOutRecord() == null) return null;

        return mapMember.get(memberId).getCheckOutRecord().getCheckOutRecordEntryList();
    }


}

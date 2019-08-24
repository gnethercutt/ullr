package org.nethercutt.ullr.exception;

public class LuceneException extends Exception {

    private static final long serialVersionUID = 3253349050063308665L;

    public LuceneException(Exception e) {
        super(e);
    }

    public LuceneException(String msg, Exception e) {
        super(msg, e);
    }
}
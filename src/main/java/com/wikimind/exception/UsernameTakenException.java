package com.wikimind.exception;

public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("username '" + username + "' is already taken");
    }
}

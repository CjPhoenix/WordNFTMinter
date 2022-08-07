package me.phoenix.wnm.exceptions;

public class NoDefintionException extends Exception {
    String word;
    public NoDefintionException(String word) {
        this.word = word;
    }

    public String getLocalizedMessage() {
        return "No definition is available for \"" + word + "\"";
    }
}
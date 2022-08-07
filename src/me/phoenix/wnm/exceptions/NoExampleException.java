package me.phoenix.wnm.exceptions;

public class NoExampleException extends Exception {
    String word;
    public NoExampleException(String word) {
        this.word = word;
    }

    public String getLocalizedMessage() {
        return "No example is available for \"" + word + "\"";
    }
}

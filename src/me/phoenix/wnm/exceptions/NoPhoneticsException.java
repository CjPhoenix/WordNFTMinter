package me.phoenix.wnm.exceptions;

public class NoPhoneticsException extends Exception {
    String word;
    public NoPhoneticsException(String word) {
        this.word = word;
    }

    public String getLocalizedMessage() {
        return "No phonetics are available for \"" + word + "\"";
    }
}

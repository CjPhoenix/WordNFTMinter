// Definition.java

package io.quicktype;

public class Definition {
    public String definition, example, synonyms[], antonyms[];

    public Definition(String definition) {
        this.definition = definition;
        this.example = "";
        this.synonyms = new String[0];
        this.antonyms = new String[0];
    }

    public Definition(String definition, String example) {
        this.definition = definition;
        this.example = example;
        this.synonyms = new String[0];
        this.antonyms = new String[0];
    }

    public Definition(String definition, String example, String[] synonyms, String[] antonyms) {
        this.definition = definition;
        this.example = example;
        this.synonyms = synonyms;
        this.antonyms = antonyms;
    }

    public Definition(String definition, String[] synonyms, String[] antonyms) {
        this.definition = definition;
        this.example = "";
        this.synonyms = synonyms;
        this.antonyms = antonyms;
    }
}
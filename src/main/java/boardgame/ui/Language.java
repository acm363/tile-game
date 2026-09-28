package boardgame.ui;

import java.util.Locale;

public enum Language {
    FR(Locale.FRENCH, "Français"),
    EN(Locale.ENGLISH, "English");

    private final Locale locale;
    private final String displayName;

    Language(Locale locale, String displayName) {
        this.locale = locale;
        this.displayName = displayName;
    }

    public Locale locale() {
        return locale;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

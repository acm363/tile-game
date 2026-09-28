package boardgame.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

record LaunchOptions(GameKind kind, List<String> playerNames, List<String> humanNames, int rows, int cols,
                     OptionalInt rounds, OptionalLong seed, boolean gui) {

    static final int DEFAULT_SIZE = 10;
    static final String USAGE = """
            Usage : java -jar tile-game.jar <guerre|agricole> [options] Joueur1 Joueur2 ...
              --gui            affiche le plateau dans une fenêtre Swing
              --human <nom>    ce joueur est joué à la souris (répétable, requiert --gui)
              --rows <n>       nombre de lignes du plateau (défaut 10)
              --cols <n>       nombre de colonnes du plateau (défaut 10)
              --rounds <n>     nombre de tours (défaut : 10 pour guerre, 6 pour agricole)
              --seed <n>       graine aléatoire, pour rejouer une partie à l'identique""";

    static LaunchOptions parse(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Aucun jeu indiqué");
        }
        GameKind kind = GameKind.fromCommand(args[0])
                .orElseThrow(() -> new IllegalArgumentException("Jeu inconnu : " + args[0]));
        List<String> names = new ArrayList<>();
        List<String> humans = new ArrayList<>();
        int rows = DEFAULT_SIZE;
        int cols = DEFAULT_SIZE;
        OptionalInt rounds = OptionalInt.empty();
        OptionalLong seed = OptionalLong.empty();
        boolean gui = false;
        for (int index = 1; index < args.length; index++) {
            String arg = args[index];
            switch (arg) {
                case "--gui" -> gui = true;
                case "--human" -> humans.add(value(args, ++index, arg));
                case "--rows" -> rows = positiveInt(arg, value(args, ++index, arg));
                case "--cols" -> cols = positiveInt(arg, value(args, ++index, arg));
                case "--rounds" -> rounds = OptionalInt.of(positiveInt(arg, value(args, ++index, arg)));
                case "--seed" -> seed = OptionalLong.of(parseLong(arg, value(args, ++index, arg)));
                default -> {
                    if (arg.startsWith("--")) {
                        throw new IllegalArgumentException("Option inconnue : " + arg);
                    }
                    names.add(arg);
                }
            }
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Au moins un joueur est requis");
        }
        for (String human : humans) {
            if (!names.contains(human)) {
                throw new IllegalArgumentException("--human désigne un joueur inconnu : " + human);
            }
        }
        if (!humans.isEmpty() && !gui) {
            throw new IllegalArgumentException("--human requiert --gui");
        }
        return new LaunchOptions(kind, List.copyOf(names), List.copyOf(humans), rows, cols, rounds, seed, gui);
    }

    private static String value(String[] args, int index, String option) {
        return Optional.of(index).filter(i -> i < args.length).map(i -> args[i])
                .orElseThrow(() -> new IllegalArgumentException("Valeur manquante pour " + option));
    }

    private static int positiveInt(String option, String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw new IllegalArgumentException(option + " doit être positif : " + value);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option + " attend un entier : " + value);
        }
    }

    private static long parseLong(String option, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option + " attend un entier : " + value);
        }
    }
}

package boardgame.ui;

import boardgame.board.Position;
import boardgame.engine.Action;
import boardgame.engine.Deploy;
import boardgame.engine.Pass;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class HumanTurn {

    private final List<Action> legalActions;

    public HumanTurn(List<Action> legalActions) {
        this.legalActions = List.copyOf(legalActions);
    }

    public List<Integer> deploySizes() {
        return deploys().map(Deploy::size).distinct().sorted().toList();
    }

    public Set<Position> deployTargets(int size) {
        return deploys().filter(deploy -> deploy.size() == size).map(Deploy::position).collect(Collectors.toSet());
    }

    public Optional<Deploy> deployAt(Position position, int size) {
        Deploy deploy = new Deploy(position, size);
        return legalActions.contains(deploy) ? Optional.of(deploy) : Optional.empty();
    }

    public boolean canPass() {
        return legalActions.contains(new Pass());
    }

    public List<Action> otherActions() {
        return legalActions.stream().filter(action -> !(action instanceof Deploy) && !(action instanceof Pass)).toList();
    }

    private Stream<Deploy> deploys() {
        return legalActions.stream().filter(Deploy.class::isInstance).map(Deploy.class::cast);
    }
}

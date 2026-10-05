package boardgame.ui;

import boardgame.board.Position;
import boardgame.engine.Action;
import boardgame.engine.Attack;
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
        return legal(new Deploy(position, size));
    }

    public Set<Position> attackers() {
        return attacks().map(Attack::from).collect(Collectors.toSet());
    }

    public Set<Position> attackTargets(Position from) {
        return attacks().filter(attack -> attack.from().equals(from)).map(Attack::target).collect(Collectors.toSet());
    }

    public Optional<Attack> attack(Position from, Position target) {
        return legal(new Attack(from, target));
    }

    public boolean canPass() {
        return legalActions.contains(new Pass());
    }

    public List<Action> otherActions() {
        return legalActions.stream()
                .filter(action -> !(action instanceof Deploy) && !(action instanceof Attack) && !(action instanceof Pass))
                .toList();
    }

    private <A extends Action> Optional<A> legal(A action) {
        return legalActions.contains(action) ? Optional.of(action) : Optional.empty();
    }

    private Stream<Deploy> deploys() {
        return legalActions.stream().filter(Deploy.class::isInstance).map(Deploy.class::cast);
    }

    private Stream<Attack> attacks() {
        return legalActions.stream().filter(Attack.class::isInstance).map(Attack.class::cast);
    }
}

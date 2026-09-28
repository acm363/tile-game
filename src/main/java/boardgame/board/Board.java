package boardgame.board;

import boardgame.player.Player;
import boardgame.unit.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Board {

    private final Terrain[][] terrain;
    private final Map<Position, Unit> occupants = new HashMap<>();

    public Board(Terrain[][] terrain) {
        if (terrain.length == 0 || terrain[0].length == 0) {
            throw new IllegalArgumentException("A board needs at least one tile");
        }
        int cols = terrain[0].length;
        this.terrain = new Terrain[terrain.length][];
        for (int row = 0; row < terrain.length; row++) {
            if (terrain[row].length != cols) {
                throw new IllegalArgumentException("A board must be rectangular");
            }
            this.terrain[row] = terrain[row].clone();
        }
    }

    public int rows() {
        return terrain.length;
    }

    public int cols() {
        return terrain[0].length;
    }

    public boolean contains(Position position) {
        return position.row() >= 0 && position.row() < rows() && position.col() >= 0 && position.col() < cols();
    }

    public Terrain terrain(Position position) {
        requireInside(position);
        return terrain[position.row()][position.col()];
    }

    public Optional<Unit> occupant(Position position) {
        requireInside(position);
        return Optional.ofNullable(occupants.get(position));
    }

    public boolean isFree(Position position) {
        return terrain(position).isLand() && !occupants.containsKey(position);
    }

    public List<Position> positions() {
        List<Position> positions = new ArrayList<>(rows() * cols());
        for (int row = 0; row < rows(); row++) {
            for (int col = 0; col < cols(); col++) {
                positions.add(new Position(row, col));
            }
        }
        return positions;
    }

    public List<Position> freePositions() {
        return positions().stream().filter(this::isFree).toList();
    }

    public List<Position> territoriesOf(Player player) {
        return positions().stream()
                .filter(position -> occupants.containsKey(position) && occupants.get(position).owner() == player)
                .toList();
    }

    public List<Position> neighbours(Position position) {
        requireInside(position);
        return position.adjacent().stream().filter(this::contains).toList();
    }

    public void place(Position position, Unit unit) {
        if (!isFree(position)) {
            throw new IllegalStateException("Tile " + position + " is not free");
        }
        if (occupants.containsValue(unit)) {
            throw new IllegalStateException("Unit is already on the board");
        }
        occupants.put(position, unit);
    }

    public Unit remove(Position position) {
        requireInside(position);
        Unit removed = occupants.remove(position);
        if (removed == null) {
            throw new IllegalStateException("Tile " + position + " is empty");
        }
        return removed;
    }

    private void requireInside(Position position) {
        if (!contains(position)) {
            throw new IndexOutOfBoundsException(position + " is outside a " + rows() + "x" + cols() + " board");
        }
    }
}

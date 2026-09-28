package boardgame.engine;

import boardgame.board.Resource;

public record Exchange(Resource resource, int quantity) implements Action {
}

package com.nyangttang.server.game.service;

import com.nyangttang.server.game.entity.MemberRuntime;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.random.RandomGenerator;

@Component
public class CatPathPolicy {
    public record Cell(int x, int y) {}
    private final RandomGenerator random;
    public CatPathPolicy() { this(new Random()); }
    public CatPathPolicy(RandomGenerator random) { this.random = Objects.requireNonNull(random); }

    public Cell choose(MemberRuntime cat) {
        List<Cell> candidates = new ArrayList<>();
        int x = cat.getX(), y = cat.getY();
        if (x < 9) candidates.add(new Cell(x + 1, y));
        if (y < 9) candidates.add(new Cell(x, y + 1));
        if (x > 0) candidates.add(new Cell(x - 1, y));
        if (y > 0) candidates.add(new Cell(x, y - 1));
        if (cat.getPreviousX() != null && cat.getPreviousY() != null && candidates.size() > 1) {
            candidates.removeIf(c -> c.x() == cat.getPreviousX() && c.y() == cat.getPreviousY());
        }
        return candidates.get(random.nextInt(candidates.size()));
    }
}

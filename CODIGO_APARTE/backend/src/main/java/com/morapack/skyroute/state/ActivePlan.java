package com.morapack.skyroute.state;

import org.springframework.stereotype.Component;

import com.morapack.skyroute.algorithm.Individual;
import com.morapack.skyroute.config.World;

@Component
public class ActivePlan {
    private World world;
    private Individual best;

    public void update(World world, Individual best) {
        this.world = world;
        this.best = best;
    }

    public Individual getCurrent() { return best; }
}

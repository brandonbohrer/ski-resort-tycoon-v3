package com.project.tycoon.ecs.components;

import com.project.tycoon.ecs.Component;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

public class TrailComponent implements Component {
    public List<Vector2> tiles = new ArrayList<>();
    public String name = "New Trail";
    public int difficulty = 0; // 0=Green, 1=Blue, 2=Black
    
    public void addTile(int x, int z) {
        tiles.add(new Vector2(x, z));
    }
}

package myplayer_curr;

import aic2025.user.Direction;
import aic2025.user.UnitController;

import java.util.ArrayList;

public class UnitPlayer {

    //This array will be useful
    Direction[] directions = Direction.values();

    public void run(UnitController uc) {
        Unit unit = new Gatherer(uc, false, false, new ArrayList<>());
        String lstType = "G";
        while (true) {
            unit.play();
            if (uc.isOnMap()) {
                uc.println(unit.type);
                if (!lstType.equals(unit.type)) {
                    if (unit.type.equals("T")) {
                        unit = new Tooler(uc, unit.craftedTurret, unit.goingBed, unit.blocked);
                    } else if (unit.type.equals("AT")) {
                        unit = new Attacker(uc, unit.craftedTurret, unit.goingBed, unit.blocked);
                    } else if (unit.type.equals("G")) {
                        unit = new Gatherer(uc, unit.craftedTurret, unit.goingBed, unit.blocked);
                    }
                    lstType = unit.type;
                    unit.play();
                    if (uc.isOnMap()) {
                        if (!lstType.equals(unit.type)) {
                            if (unit.type.equals("T")) {
                                unit = new Tooler(uc, unit.craftedTurret, unit.goingBed, unit.blocked);
                            } else if (unit.type.equals("AT")) {
                                unit = new Attacker(uc, unit.craftedTurret, unit.goingBed, unit.blocked);
                            } else if (unit.type.equals("G")) {
                                unit = new Gatherer(uc, unit.craftedTurret, unit.goingBed, unit.blocked);
                            }
                        }
                        lstType = unit.type;
                    } else {
                        lstType = "G";
                        unit = new Gatherer(uc, unit.craftedTurret, unit.goingBed, unit.blocked);
                    }
                } else lstType = unit.type;
            } else {
                lstType = "G";
                unit = new Gatherer(uc, unit.craftedTurret, unit.goingBed, unit.blocked);
            }
            uc.yield();
        }
    }
}

/*
1) Crear eina només quan la necessitem.
2) Els gatherers que fugeixein si se'ls apropen.
3) Fer boots (i actualitzar pathfinding).
*/
package myplayer_curr;

import aic2025.user.*;

public class UnitPlayer {

    //This array will be useful
    Direction[] directions = Direction.values();

    public void run(UnitController uc) {
        Unit unit = new Gatherer(uc);
        String lstType = "G";
        while (true) {
            unit.play();
            if (uc.isOnMap()) {
                if (!lstType.equals(unit.type)) {
                    if (unit.type.equals("T")) {
                        unit = new Tooler(uc);
                    } else if (unit.type.equals("AT")) {
                        unit = new Attacker(uc);
                    } else if (unit.type.equals("G")) {
                        unit = new Gatherer(uc);
                    }
                    lstType = unit.type;
                    unit.play();
                    if (uc.isOnMap()) {
                        if (!lstType.equals(unit.type)) {
                            if (unit.type.equals("T")) {
                                unit = new Tooler(uc);
                            } else if (unit.type.equals("AT")) {
                                unit = new Attacker(uc);
                            } else if (unit.type.equals("G")) {
                                unit = new Gatherer(uc);
                            }
                        }
                        lstType = unit.type;
                    } else {
                        lstType = "G";
                        unit = new Gatherer(uc);
                    }
                } else lstType = unit.type;
            } else {
                lstType = "G";
                unit = new Gatherer(uc);
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

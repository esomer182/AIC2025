package myplayer;

import aic2025.user.BedInfo;
import aic2025.user.Craftable;
import aic2025.user.Direction;
import aic2025.user.UnitController;

public class UnitPlayer {

    //This array will be useful
    Direction[] directions = Direction.values();

    public void run(UnitController uc) {
        Unit unit = new Gatherer(uc);
        String lstType = "G";
        while (true) {
            unit.play();
            if (lstType != unit.type) {
                if (unit.type == "S") {
                    unit = new Shoveler(uc);
                } else if (unit.type == "AT") {
                    unit = new Attacker(uc);
                } else if (unit.type == "P") {
                    unit = new Pickaxer(uc);
                } else if (unit.type == "AX") {
                    unit = new Axer(uc);
                }
            }
            unit.play();
            uc.yield();
        }
    }
}

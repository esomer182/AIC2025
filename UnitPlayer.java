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
        while (true) {
            unit.play();
            uc.yield();
        }
    }
}
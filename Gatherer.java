package myplayer;

import aic2025.user.*;

public class Gatherer extends Unit{
    Location myLoc;
    Craftable weapon = null;
    Location obj = null;
    int randomObj = 1;
    UnitInfo me;
    public Gatherer (UnitController uc) {
        init(uc);
        getRandomLoc();
        me = uc.getUnitInfo();
    }

    public void play() {
        myLoc = uc.getLocation();
        me = uc.getUnitInfo();
        if (myLoc == obj && randomObj == 1) {
            getRandomLoc();
        }
        sense
    }

    public void getRandomLoc() {
        obj = new Location(getRandomInt(0, mapWidth), getRandomInt(0, mapHeight));
        randomObj = 1;
    }

    public void attack(Location loc){
        if (weapon == null) return;
        Direction dir = myLoc.directionTo(loc);
        Location nwLoc = myLoc.add(dir);
        if(!nwLoc.isEqual(loc)){
            pathfinding.moveTo(loc);
        } else {
            if(uc.canUseCraftable(weapon, nwLoc)){
                uc.useCraftable(weapon, nwLoc);
            }
        }
    }
}

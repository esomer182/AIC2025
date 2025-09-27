package myplayer;

import aic2025.user.*;

public class Attacker extends Unit{
    Location myLoc;
    Craftable weapon = null;
    Location obj = null;
    int randomObj = 1;
    UnitInfo me;
    public Attacker(UnitController uc) {
        init(uc);
        getRandomLoc();
        me = uc.getUnitInfo();
        weapon = me.getCarriedCraftablesArray()[0].getCraftable();

    }

    public void play() {
        myLoc = uc.getLocation();
        me = uc.getUnitInfo();
        obj = null;
        int round = uc.getRound();
        searchEnemy();
        if (obj == null){
            if (weapon == Craftable.AXE && round<1500) changeType ("AX", "AT");
            else if (weapon == Craftable.SHOVEL && round<1500) changeType ("S", "AT");
            else if (weapon == Craftable.PICKAXE && round<1500) changeType ("P", "AT");
            else{
                getRandomLoc();
                pathfinding.moveTo(obj);
            }
        }
        else{
            if (uc.canUseCraftable(weapon, obj)) uc.useCraftable(weapon, obj);
            pathfinding.moveTo(obj);
        }
    }

    public void searchEnemy() {
        StructureInfo[] llits = uc.senseStructures(GameConstants.UNIT_VISION_RANGE, uc.getOpponent(), StructureType.BED);
        if (llits.length>0){
            Location curr = null;
            int currlife = 20;
            for (StructureInfo llit : llits) {
                if (curr == null || llit.getHealth() < currlife) {
                    curr = llit.getLocation();
                    currlife = llit.getHealth();
                }
            }
            obj = curr;
            randomObj = 0;
        }
        else{
            UnitInfo[] enemics = uc.senseUnits(GameConstants.UNIT_VISION_RANGE, uc.getOpponent());
            if (enemics.length>0){
                Location curr = null;
                int currlife = 20;
                for (UnitInfo enemic : enemics) {
                    if (curr == null || enemic.getHealth() < currlife) {
                        curr = enemic.getLocation();
                        currlife = enemic.getHealth();
                    }
                }
                obj = curr;
                randomObj = 0;
            }
        }
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

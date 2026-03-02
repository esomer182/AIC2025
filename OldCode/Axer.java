package myplayer;

import aic2025.user.*;

import java.util.Arrays;

public class Axer extends Unit{
    Location myLoc;
    Craftable weapon = Craftable.AXE;
    Location obj = null;
    int randomObj = 1;
    UnitInfo me;
    Material[] wantedMaterials = {Material.STRING, Material.WOOD, Material.LEATHER};
    int round;
    public Axer (UnitController uc) {
        init(uc);
        getRandomLoc();
        me = uc.getUnitInfo();
    }

    public void play() {
        myLoc = uc.getLocation();
        me = uc.getUnitInfo();
        round = uc.getRound();
        readBuffer();
        updBuffer();
        if (uc.canCraft(Craftable.BED_BLUEPRINT)) {
            uc.craft(Craftable.BED_BLUEPRINT);
            tryToCreate(Craftable.BED_BLUEPRINT);
        }
        if (uc.senseStructures(GameConstants.UNIT_VISION_RANGE, uc.getOpponent(), StructureType.BED).length>0) changeType ("AT", "AX");
        else if (uc.senseUnits(2, uc.getOpponent()).length>0) changeType ("AT", "AX");
        else{
            if (myLoc == obj && randomObj == 1) {
                getRandomLoc();
            }
            searchLocation();
            if (uc.canUseCraftable(weapon, obj)) {
                uc.useCraftable(weapon, obj);
                Location obj2 = obj;
                searchLocation();
                if (obj == obj2) {
                    getRandomLoc();
                }
            }
        }
        pathfinding.moveTo(obj);
    }

    public void searchLocation() {
        MaterialInfo[] materials = uc.senseMaterials(GameConstants.UNIT_VISION_RANGE);
        Location nearest = null;
        for (MaterialInfo material : materials) {
            if (Arrays.asList(wantedMaterials).contains(material.getMaterial())) {
                if (nearest == null || isNearest(myLoc, material.getLocation(), nearest)) {
                    nearest = material.getLocation();
                }
            }
        }
        if (nearest != null) {
            obj = nearest;
            randomObj = 0;

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

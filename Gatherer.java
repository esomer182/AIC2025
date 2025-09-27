package myplayer;

import aic2025.user.*;

import java.util.Arrays;

public class Gatherer extends Unit{
    Location myLoc;
    Craftable weapon = null;
    Location obj = null;
    int randomObj = 1;
    UnitInfo me;
    Material[] wantedMaterials = {Material.WOOD, Material.STONE};
    int round;
    public Gatherer (UnitController uc) {
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
        if (myLoc == obj && randomObj == 1) {
            getRandomLoc();
        }
        searchLocation();
        if (myLoc == obj && uc.canGather()) {
            uc.gather();
            searchLocation();
            if (obj == myLoc) {
                getRandomLoc();
            }
        }
        pathfinding.moveTo(obj);
        if (shovelers < round/100+1 && uc.canCraft(Craftable.SHOVEL)) {
            uc.craft(Craftable.SHOVEL); //Can be improved.
            changeType("S", "G");
            return;
        }
        if (uc.canCraft(Craftable.AXE) && uc.canCraft(Craftable.PICKAXE)) {
            if (pickaxers < axers) {
                uc.craft(Craftable.PICKAXE); //Can be improved.
                changeType("P", "G");
            } else {
                uc.craft(Craftable.AXE); //Can be improved.
                changeType("AX", "G");
            }
        } else if (uc.canCraft(Craftable.AXE)) {
            uc.craft(Craftable.AXE); //Can be improved.
            changeType("AX", "G");
        } else if (uc.canCraft(Craftable.PICKAXE)) {
            uc.craft(Craftable.PICKAXE); //Can be improved.
            changeType("P", "G");
        }
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

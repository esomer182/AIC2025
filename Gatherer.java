package myplayer;

import aic2025.user.*;

import java.util.Arrays;

public class Gatherer extends Unit{
    Location myLoc;
    Location obj = null;
    int randomObj = 1;
    UnitInfo me;
    int round;
    int lstLocRound = 0;
    public Gatherer (UnitController uc) {
        init(uc);
    }

    public void play() {
        updMatWeight();
        //readBuffer();
        //updBuffer();
        if (!uc.isOnMap()){
            boolean spawned = false;
            BedInfo[] beds = uc.getAllBedsInfo();
            for (BedInfo bed : beds){
                if (!bed.isOccupied() && uc.canSpawn(bed.getLocation())){
                    uc.spawn(bed.getLocation());
                    spawned = true;
                    break;
                }
            }
            if (!spawned) return;
        }
        if (craftTool()) return;
        if (uc.getRound() >= GameConstants.MAX_ROUNDS-50) {
            craftAnything();
        }
        tryCraftImportant();
        tryHeal();
        if ((uc.senseStructures(GameConstants.UNIT_VISION_RANGE, uc.getOpponent(), StructureType.BED).length>0
                || uc.senseUnits(2, uc.getOpponent()).length>0) && uc.canCraft(Craftable.SHOVEL)) {
            if (uc.canCraft(Craftable.PICKAXE)) uc.craft(Craftable.PICKAXE);
            else uc.craft(Craftable.SHOVEL);
            changeType ("AT", "G");
            return;
        }
        myLoc = uc.getLocation();
        me = uc.getUnitInfo();
        round = uc.getRound();
        if (round - lstLocRound >= 20 || obj == null || (myLoc.isEqual(obj) && randomObj == 1)) {
            getRandomLoc();
        }
        searchLocation();
        if (myLoc.isEqual(obj) && uc.canGather()) {
            uc.gather();
            if(!searchLocation()) getRandomLoc();
        }
        if (randomObj == 1 && uc.canGather()) {
            uc.gather();
        }
        if (randomObj == 0 && !searchLocation() && round-lstLocRound >= 20) {
            getRandomLoc();
        }
        pathfinding.moveTo(obj);
    }

    public boolean craftTool() {
        if (round <= 1000) {
            if(uc.canCraft(Craftable.AXE)) {
                uc.craft(Craftable.AXE); //Can be improved.
                changeType("T", "G");
            } else if (round > 700 && uc.canCraft(Craftable.PICKAXE)) {
                uc.craft(Craftable.PICKAXE); //Can be improved.
                changeType("T", "G");
            }
        } else {
            if (uc.canCraft(Craftable.PICKAXE)) {
                uc.craft(Craftable.PICKAXE); //Can be improved.
                changeType("T", "G");
                return true;
            } else if (uc.canCraft(Craftable.AXE)) {
                uc.craft(Craftable.AXE); //Can be improved.
                changeType("T", "G");
                return true;
            }
        }
        return false;
    }

    public boolean searchLocation() {
        MaterialInfo[] materials = uc.senseMaterials(uc.getVisionRange());
        Location nearest = null;
        Material mat = null;
        int[] hvMat = uc.getUnitInfo().getCarriedMaterials();
        for (MaterialInfo material : materials) {
            Material nwMat = material.getMaterial();
            if (material.getLocation().equals(obj) && round-lstLocRound >= 20) continue;
            if (nwMat == Material.STONE || nwMat == Material.WOOD) {
                if (mat == null || hvMat[nwMat.ordinal()] < hvMat[mat.ordinal()] || (hvMat[nwMat.ordinal()] == hvMat[mat.ordinal()] && isNearest(myLoc, material.getLocation(), nearest))) {
                    nearest = material.getLocation();
                    mat = nwMat;
                }
            }
        }
        if (nearest != null) {
            if (!nearest.equals(obj)) lstLocRound = round;
            obj = nearest;
            randomObj = 0;
            return true;
        } else {
            for (MaterialInfo material : materials) {
                if (material.getLocation().equals(obj) && round-lstLocRound >= 20) continue;
                if (material.getMaterial() == Material.POTATO) {
                    if (nearest == null || isNearest(myLoc, material.getLocation(), nearest)) {
                        nearest = material.getLocation();
                    }
                }
            }
            if (nearest != null) {
                if (!nearest.equals(obj)) lstLocRound = round;
                obj = nearest;
                randomObj = 0;
                return true;
            } else return false;
        }
    }

    public void getRandomLoc() {
        obj = new Location(getRandomInt(0, mapWidth-1), getRandomInt(0, mapHeight-1));
        randomObj = 1;
        lstLocRound = round;
    }
}

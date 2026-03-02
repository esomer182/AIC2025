package myplayer_curr;

import aic2025.user.*;

public class Gatherer extends Unit{
    Location myLoc;
    Location obj = null;
    int randomObj = 1;
    UnitInfo me;
    int round;
    int lstLocRound = 0;
    public Gatherer (UnitController uc) {
        init(uc);
        type="G";
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
        if (!uc.canAct() || !uc.canMove()) updateBiome();
        tryHeal();
        if (craftTool()) return;
        if (uc.getRound() >= GameConstants.MAX_ROUNDS-100) {
            craftAnything();
        }
        tryCraftImportant();
        destroy();
        if ((uc.senseStructures(GameConstants.UNIT_VISION_RANGE, uc.getOpponent(), StructureType.BED).length>0
                || uc.senseUnits(2, uc.getOpponent()).length>0) && (uc.canCraft(Craftable.SHOVEL) || uc.hasCraftable(Craftable.SHOVEL))) {
            if (uc.canCraft(Craftable.PICKAXE)) uc.craft(Craftable.PICKAXE);
            else if (!uc.hasCraftable(Craftable.SHOVEL)) uc.craft(Craftable.SHOVEL);
            changeType ("AT", "G");
            return;
        }
        myLoc = uc.getLocation();
        me = uc.getUnitInfo();
        round = uc.getRound();
        if (round - lstLocRound >= 20 || obj == null || (myLoc.isEqual(obj) && randomObj == 1)) {
            getRandomLoc();
        }
        boolean found = searchLocation();
        if (myLoc.isEqual(obj) && uc.canGather()) {
            uc.gather();
            found = searchLocation();
            if (!found) getRandomLoc();
        }
        tryGatherAround(randomObj, obj);
        if (randomObj == 0 && !found && round-lstLocRound >= 20) {
            getRandomLoc();
        }
        pathfinding.moveTo(obj);
    }

    public boolean craftTool() {
        if (!uc.canCraft(Craftable.SHOVEL)) return false;
        if (round <= 400) {
            if(uc.canCraft(Craftable.AXE)) {
                uc.craft(Craftable.AXE); //Can be improved.
                changeType("T", "G");
                return true;
            }
        } else {
            Craftable first = Craftable.AXE;
            Craftable second = Craftable.PICKAXE;
            if (uc.getRandomDouble() >= 0.5) {
                first = Craftable.PICKAXE;
                second = Craftable.AXE;
            }
            if (currBiome == Biome.FOREST) {
                first = Craftable.AXE;
                second = Craftable.PICKAXE;
            } else if (currBiome == Biome.CAVE) {
                first = Craftable.PICKAXE;
                second = Craftable.AXE;
            }
            if (uc.canCraft(first)) {
                uc.craft(first);
                changeType("T", "G");
                return true;
            } else if (uc.canCraft(second)) {
                uc.craft(second);
                changeType("T", "G");
                return true;
            }
        }
        return false;
    }

    public boolean searchLocation() {
        if (!uc.canMove() && !uc.canAct()) return false;
        MaterialInfo[] materials = uc.senseMaterials(uc.getVisionRange());
        Location nearest = null;
        Material mat = null;
        int[] hvMat = uc.getUnitInfo().getCarriedMaterials();
        for (MaterialInfo material : materials) {
            Material nwMat = material.getMaterial();
            if ((material.getLocation().equals(obj) && round-lstLocRound >= 20)) continue;
            if ((nwMat == Material.STONE || nwMat == Material.WOOD) && hvMat[nwMat.ordinal()] < 2) {
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
            if (!uc.hasCraftable(Craftable.BAKED_POTATO) && hvMat[Material.POTATO.ordinal()] == 0) {
                for (MaterialInfo material : materials) {
                    if (material.getLocation().equals(obj) && round-lstLocRound >= 20) continue;
                    if (material.getMaterial() == Material.POTATO) {
                        if (nearest == null || isNearest(myLoc, material.getLocation(), nearest)) {
                            nearest = material.getLocation();
                        }
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
        if (!uc.canMove() && !uc.canAct()) return;
        obj = new Location(getRandomInt(0, mapWidth-1), getRandomInt(0, mapHeight-1));
        randomObj = 1;
        lstLocRound = round;
    }
}

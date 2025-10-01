package myplayer_curr;

import aic2025.user.*;

public class Tooler extends Unit{
    Location myLoc;
    Location obj = null;
    int randomObj = 1;
    UnitInfo me;
    int round;
    int lstLocRound = 0;
    public Tooler (UnitController uc) {
        init(uc);
        getRandomLoc();
        me = uc.getUnitInfo();
        type="T";
    }

    public void play() {
        //readBuffer();
        //updBuffer();
        updMatWeight();
        if (!uc.isOnMap() && !type.equals("G")) {
            changeType("G", type);
            return;
        }
        if (type.equals("G")) changeType("T", "G");
        if (!uc.canAct() || !uc.canMove()) updateBiome();
        tryHeal();
        tryCraftImportant();
        tryCraftTools();
        if (uc.getRound() >= GameConstants.MAX_ROUNDS-100) {
            craftAnything();
        }
        tryUpgrade();
        destroy();
        myLoc = uc.getLocation();
        me = uc.getUnitInfo();
        round = uc.getRound();
        if (uc.senseStructures(GameConstants.UNIT_VISION_RANGE, uc.getOpponent(), StructureType.BED).length>0
        || uc.senseUnits(5, uc.getOpponent()).length>0) {
            changeType ("AT", "T");
            return;
        }
        if (round - lstLocRound >= 50 || obj == null || (myLoc.isEqual(obj) && randomObj == 1)) {
            getRandomLoc();
        }
        boolean found = searchLocation();
        if (randomObj != 1 && canGatherTool(obj)) {
            uc.useCraftable(uc.senseMaterialAtLocation(obj).gatheringTool(), obj);
            Location obj2 = obj;
            found = searchLocation();
            if (obj.isEqual(obj2)) {
                getRandomLoc();
            }
        }
        tryGatherAround(randomObj, obj);
        if (randomObj == 0 && !found && round-lstLocRound >= 20) {
            getRandomLoc();
        }
        pathfinding.moveTo(obj);
    }

    public boolean searchLocation() {
        if (!uc.canMove() && !uc.canAct()) return false;
        MaterialInfo[] materials = uc.senseMaterials(uc.getVisionRange());
        Location nearest = null;
        Material currMat = null;
        int[] hvMat = uc.getUnitInfo().getCarriedMaterials();
        for (MaterialInfo material : materials) {
            if (material.getLocation().equals(obj) && round-lstLocRound >= 20) continue;
            int i = material.getMaterial().ordinal();
            if (neededTool[i] == null || !uc.hasCraftable(neededTool[i]) || neededTool[i] == Craftable.SHOVEL) continue;
            int curr = 0;
            if (currMat != null) curr = currMat.ordinal();
            if (matWeight[i] >= 0 && (nearest == null || matWeight[i]-hvMat[i] > matWeight[curr]-hvMat[curr]
                || (matWeight[i]-hvMat[i] == matWeight[curr]-hvMat[curr] && isNearest(myLoc, material.getLocation(), nearest)))) {
                nearest = material.getLocation();
                currMat = material.getMaterial();
            }
        }
        if (nearest != null) {
            if (!nearest.equals(obj)) lstLocRound = round;
            obj = nearest;
            randomObj = 0;
            return true;
        } else {
            if ((uc.hasCraftable(Craftable.AXE) || uc.hasCraftable(Craftable.PICKAXE)) && uc.hasCraftable(Craftable.SHOVEL)) {
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
            } else {
                Material mat = null;
                for (MaterialInfo material : materials) {
                    if (material.getLocation().equals(obj) && round-lstLocRound >= 20) continue;
                    Material nwMat = material.getMaterial();
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
                } else return false;
            }
        }
    }

    public void getRandomLoc() {
        if (!uc.canMove() && !uc.canAct()) return;
        obj = new Location(getRandomInt(0, mapWidth-1), getRandomInt(0, mapHeight-1));
        randomObj = 1;
        lstLocRound = round;
    }

    public void tryCraftTools() {
        if (!uc.canCraft(Craftable.SHOVEL)) return;
        //Maybe put that only craft the tool if wood > leather in case we have an axe.
        if (currBiome == Biome.CAVE && uc.canCraft(Craftable.PICKAXE) && !uc.hasCraftable(Craftable.PICKAXE)) uc.craft(Craftable.PICKAXE);
        else if (currBiome == Biome.FOREST && uc.canCraft(Craftable.AXE) && !uc.hasCraftable(Craftable.AXE)) uc.craft(Craftable.AXE);
        if (uc.canCraft(Craftable.SHOVEL) && !uc.hasCraftable(Craftable.SHOVEL)) {
            uc.craft(Craftable.SHOVEL);
        }
    }
}

package myplayer_curr;

import aic2025.user.*;

import java.util.BitSet;
import java.util.ArrayList;

public class Tooler extends Unit {
    Location myLoc;
    Location obj = null;
    int randomObj = 1;
    UnitInfo me;
    int round;
    int lstLocRound = 0;
    public Tooler (UnitController uc, boolean _craftTurret, boolean _goingBed, ArrayList<Integer> _blocked) {
        init(uc);
        getRandomLoc();
        me = uc.getUnitInfo();
        type="T";
        craftedTurret = _craftTurret;
        goingBed = _goingBed;
        roundAxe = 200 + Math.max(0, uc.getMapHeight()*uc.getMapWidth()-900)/6;
        blocked = _blocked;
        lstLocRound = uc.getRound();
        blockedStr = new BitSet(uc.getMapHeight()*uc.getMapWidth());
        for (int i = 0; i < (int)blocked.size(); i++) {
            int code = blocked.get(i);
            int x = code%64; code /= 64;
            int y = code%64; code /= 64;
            blockedStr.set(x*uc.getMapHeight()+y);
        }
    }

    public void play() {
        //readBuffer();
        //updBuffer();
        //uc.println(uc.getEnergyLeft());
        searchedMaterials = false;
        updBlocked();
        updMatWeight();
        if (!uc.isOnMap() && !type.equals("G")) {
            changeType("G", type);
            return;
        }
        if (type.equals("G")) changeType("T", "G");
        if (!uc.canAct() || !uc.canMove()) updateBiome();
        tryHeal();
        if (!goingBed) {
            tryCraftImportant();
            tryCraftTools();
        }
        if (uc.getRound() >= GameConstants.MAX_ROUNDS-100) {
            craftAnything();
        }
        tryUpgrade();
        /*if (!craftedTurret && !goingBed && uc.getBed() != null && uc.canCraft(Craftable.TURRET_BLUEPRINT)) {
            goingBed = true;
        }
        if (goingBed && uc.getBed() != null) {
            goingBed = false;
        }
        if (goingBed && myLoc.distanceSquared(uc.getBed()) <= 2) {
            uc.craft(Craftable.TURRET_BLUEPRINT);
            craftedTurret = true;
        }
        if (goingBed && myLoc == uc.getBed()) {
            tryToCreate(Craftable.TURRET_BLUEPRINT);
        }*/
        destroy(-1);
        myLoc = uc.getLocation();
        me = uc.getUnitInfo();
        round = uc.getRound();
        if (uc.senseStructures(GameConstants.UNIT_VISION_RANGE, uc.getOpponent(), StructureType.BED).length>0
        || uc.senseUnits(enemyRadius, uc.getOpponent()).length>0) {
            changeType ("AT", "T");
            return;
        }
        if (round - lstLocRound >= uc.getVisionRange()*2) {
            blocked.add(obj.x+obj.y*64+round*(1<<12));
        }
        if (round - lstLocRound >= uc.getVisionRange()*2 || obj == null || (myLoc.isEqual(obj) && randomObj == 1)) {
            getRandomLoc();
        }
        boolean found;
        if (me.getHealth() <= 4) found = searchPotato();
        else found = searchLocation();
        if (randomObj != 1 && canGatherTool(obj)) {
            uc.useCraftable(uc.senseMaterialAtLocation(obj).gatheringTool(), obj);
            Location obj2 = obj;
            if (me.getHealth() <= 4) found = searchPotato();
            else found = searchLocation();
            if (obj.isEqual(obj2)) {
                getRandomLoc();
            }
        }
        tryGatherAround(randomObj, obj);
        if (randomObj == 0 && !found && uc.canSenseLocation(obj)) {
            getRandomLoc();
        }
        if (goingBed) pathfinding.moveTo(uc.getBed());
        else pathfinding.moveTo(obj);
    }

    public boolean searchPotato() {
        if (!uc.canMove() && !uc.canAct()) return false;
        if (!searchedMaterials) {
            searchedMaterials = true;
            materials = uc.senseMaterials(uc.getVisionRange());
        }
        Location nearest = null;
        for (MaterialInfo material : materials) {
            if (isInBlocked(material.getLocation())) continue;
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

    public boolean searchLocation() {
        if (!uc.canMove() && !uc.canAct()) return false;
        if (!searchedMaterials) {
            searchedMaterials = true;
            materials = uc.senseMaterials(uc.getVisionRange());
        }
        Location nearest = null;
        Material currMat = null;
        for (MaterialInfo material : materials) {
            if (isInBlocked(material.getLocation())) continue;
            int i = material.getMaterial().ordinal();
            if (neededTool[i] == null || !uc.hasCraftable(neededTool[i]) || material.getMaterial() == Material.GRASS || (material.getMaterial() == Material.POTATO && hvBakedPotatoes >= 3)) continue;
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
                    if (isInBlocked(material.getLocation())) continue;
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
                    if (isInBlocked(material.getLocation())) continue;
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
        if (currBiome == Biome.CAVE && !uc.hasCraftable(Craftable.PICKAXE)) {
            if (uc.canCraft(Craftable.PICKAXE)) uc.craft(Craftable.PICKAXE);
        } else if (currBiome == Biome.FOREST && !uc.hasCraftable(Craftable.AXE)) {
            if (uc.canCraft(Craftable.AXE)) uc.craft(Craftable.AXE);
        } else {
            if (randomObj == 1 && currBiome == Biome.DEFAULT) {
                if ((hvMat[Material.WOOD.ordinal()] > hvMat[Material.STRING.ordinal()] || uc.getRound() >= roundAxe) && uc.canCraft(Craftable.PICKAXE) && !uc.hasCraftable(Craftable.PICKAXE)) {
                    uc.craft(Craftable.PICKAXE);
                }
                if (uc.canCraft(Craftable.AXE) && !uc.hasCraftable(Craftable.AXE)) {
                    uc.craft(Craftable.AXE);
                }
            }
            if ((hvMat[Material.WOOD.ordinal()] > hvMat[Material.STRING.ordinal()] || uc.getRound() >= roundAxe || !uc.hasCraftable(Craftable.AXE)) && uc.canCraft(Craftable.SHOVEL) && !uc.hasCraftable(Craftable.SHOVEL)) {
                uc.craft(Craftable.SHOVEL);
            }
        }
    }
}
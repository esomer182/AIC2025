package myplayer_curr;

import aic2025.user.*;

import java.util.ArrayList;
import java.util.BitSet;

public class Gatherer extends Unit {
    Location myLoc;
    Location obj = null;
    int randomObj = 1;
    UnitInfo me;
    int round;
    int lstLocRound = 0;
    int axer = -1;
    public Gatherer (UnitController uc, boolean _craftTurret, boolean _goingBed, ArrayList<Integer> _blocked) {
        init(uc);
        type="G";
        craftedTurret = _craftTurret;
        goingBed = _goingBed;
        roundAxe = 200 + Math.max(0, uc.getMapHeight()*uc.getMapWidth()-900)/6;
        uc.println(roundAxe);
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
        searchedMaterials = false;
        updBlocked();
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
        destroy(-1);
        myLoc = uc.getLocation();
        me = uc.getUnitInfo();
        round = uc.getRound();
        UnitInfo[] enemies = uc.senseUnits(enemyRadius, uc.getOpponent());
        if (enemies.length > 0) {
            if (uc.hasCraftable(Craftable.SHOVEL)) {
                changeType("AT", "G");
                return;
            } else if (uc.canCraft(Craftable.PICKAXE)) {
                uc.craft(Craftable.PICKAXE);
                changeType("AT", "G");
                return;
            } else if (uc.canCraft(Craftable.SHOVEL)) {
                uc.craft(Craftable.SHOVEL);
                changeType("AT", "G");
                return;
            } else {
                CraftableInfo[] crafts = enemies[0].getCarriedCraftablesArray();
                for (CraftableInfo craft : crafts) {
                    Craftable c = craft.getCraftable();
                    if (c == Craftable.SHOVEL || c == Craftable.AXE
                    || c == Craftable.PICKAXE || c == Craftable.SWORD) {
                        flee(enemies[0]);
                        break;
                    }
                }
            }
        }
        StructureInfo[] beds = uc.senseStructures(uc.getVisionRange(), uc.getOpponent(), StructureType.BED);
        if (beds.length>0 && uc.canCraft(Craftable.PICKAXE)) {
            uc.craft(Craftable.PICKAXE);
            changeType("AT", "G");
            return;
        }
        if (round - lstLocRound >= uc.getVisionRange()*2) {
            blocked.add(obj.x+obj.y*64+round*(1<<12));
        }
        if (round - lstLocRound >= uc.getVisionRange()*2 || obj == null || (myLoc.isEqual(obj) && randomObj == 1)) {
            getRandomLoc();
        }
        boolean found = searchLocation();
        if (myLoc.isEqual(obj) && uc.canGather()) {
            uc.gather();
            found = searchLocation();
            if (!found) getRandomLoc();
        }
        tryGatherAround(randomObj, obj);
        if (randomObj == 0 && !found && uc.canSenseLocation(obj)) {
            getRandomLoc();
        }
        pathfinding.moveTo(obj);
    }

    public boolean craftTool() {
        if (!uc.canCraft(Craftable.SHOVEL)) return false;
        if (round <= roundAxe) {
            if(uc.canCraft(Craftable.AXE)) {
                uc.craft(Craftable.AXE); //Can be improved.
                changeType("T", "G");
                return true;
            }
        } else {
            if (axer == -1) {
                if (AxeOverPickaxe()) axer = 1;
                else axer = 0;
            }
            if (randomObj == 1) {
                if (uc.canCraft(Craftable.PICKAXE)) {
                    uc.craft(Craftable.PICKAXE);
                    changeType("T", "G");
                    return true;
                }
                if(uc.canCraft(Craftable.AXE)) {
                    uc.craft(Craftable.AXE); //Can be improved.
                    changeType("T", "G");
                    return true;
                }
            } else {
                if (axer == 1) {
                    if(uc.canCraft(Craftable.AXE)) {
                        uc.craft(Craftable.AXE); //Can be improved.
                        changeType("T", "G");
                        return true;
                    }
                } else {
                    if (uc.canCraft(Craftable.PICKAXE)) {
                        uc.craft(Craftable.PICKAXE);
                        changeType("T", "G");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean searchLocation() {
        if (!uc.canMove() && !uc.canAct()) return false;
        if (!searchedMaterials) {
            searchedMaterials = true;
            materials = uc.senseMaterials(uc.getVisionRange());
        }
        Location nearest = null;
        Material mat = null;
        for (MaterialInfo material : materials) {
            Material nwMat = material.getMaterial();
            if (isInBlocked(material.getLocation())) continue;
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
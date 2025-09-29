package myplayer;

import aic2025.engine.Game;
import aic2025.user.*;

public class Attacker extends Unit{
    Location myLoc;
    Craftable weapon = null;
    Location obj = null;
    int randomObj = 1;
    UnitInfo me;
    int lstLocRound = 0;
    int round;

    public Attacker(UnitController uc) {
        init(uc);
        getRandomLoc();
        me = uc.getUnitInfo();
    }

    public void play() {
        updMatWeight();
        //readBuffer();
        //updBuffer();
        if (!uc.isOnMap() && !type.equals("G")) {
            changeType("G", type);
            return;
        }
        if (type.equals("G")) changeType("AT", "G");
        if (uc.hasCraftable(Craftable.SWORD)) {
            weapon = Craftable.SWORD;
            if (!uc.hasCraftable(Craftable.ARMOR) && uc.canCraft(Craftable.ARMOR)) {
                uc.craft(Craftable.ARMOR);
            }
        } else if (uc.canCraft(Craftable.SWORD)) {
            uc.craft(Craftable.SWORD);
            weapon = Craftable.SWORD;
            if (uc.hasCraftable(Craftable.SHOVEL)) uc.destroyCraftable(Craftable.SHOVEL);
            if (uc.hasCraftable(Craftable.PICKAXE)) uc.destroyCraftable(Craftable.PICKAXE);
            if (uc.hasCraftable(Craftable.AXE)) uc.destroyCraftable(Craftable.AXE);
        } else {
            if (uc.hasCraftable(Craftable.SHOVEL)) weapon = Craftable.SHOVEL;
            else if (uc.hasCraftable(Craftable.AXE)) weapon = Craftable.AXE;
            else weapon = Craftable.PICKAXE;
        }
        tryCraftImportant();
        tryUpgrade();
        if (uc.getRound() >= GameConstants.MAX_ROUNDS-50) {
            craftAnything();
        }
        myLoc = uc.getLocation();
        me = uc.getUnitInfo();
        round = uc.getRound();
        if (!searchEnemy()){
            if (weapon != Craftable.SWORD && round<2300) {
                changeType ("T", "AT");
                return;
            } else{
                if (randomObj == 0) getRandomLoc();
                else if (randomObj == 1 && obj.equals(myLoc)) getRandomLoc();
            }
        } else {
            if (uc.canUseCraftable(Craftable.FIREWORK, obj)) uc.useCraftable(Craftable.FIREWORK, obj);
            else if (uc.canUseCraftable(weapon, obj)) uc.useCraftable(weapon, obj);
            else if (uc.canCraft(Craftable.FIREWORK)) uc.craft(Craftable.FIREWORK);
        }
        tryHeal();
        if (uc.canSenseLocation(myLoc) && uc.hasCraftable(uc.senseMaterialAtLocation(myLoc).gatheringTool())
                && uc.canUseCraftable(uc.senseMaterialAtLocation(myLoc).gatheringTool(), myLoc)) {
            uc.useCraftable(uc.senseMaterialAtLocation(myLoc).gatheringTool(), myLoc);
        } else if (randomObj == 1 && uc.canSenseLocation(myLoc) && uc.senseMaterialAtLocation(myLoc) == Material.POTATO && uc.canGather()) {
            uc.gather();
        }
        if (randomObj == 1) searchPotato();
        pathfinding.moveTo(obj);
    }
    public boolean searchEnemy() {
        UnitInfo[] enemics = uc.senseUnits(2, uc.getOpponent());
        if (enemics.length > 0) {
            Location curr = null;
            int currlife = 20;
            for (UnitInfo enemic : enemics) {
                if (curr == null || enemic.getHealth() < currlife) {
                    curr = enemic.getLocation();
                    currlife = enemic.getHealth();
                }
            }
            if (!curr.equals(obj)) lstLocRound = round;
            obj = curr;
            randomObj = 0;
            return true;
        } else{
            StructureInfo[] llits = uc.senseStructures(uc.getVisionRange(), uc.getOpponent(), StructureType.BED);
            if (llits.length > 0) {
                Location curr = null;
                int currlife = 20;
                for (StructureInfo llit : llits) {
                    if (curr == null || llit.getHealth() < currlife) {
                        curr = llit.getLocation();
                        currlife = llit.getHealth();
                    }
                }
                if (!curr.equals(obj)) lstLocRound = round;
                obj = curr;
                randomObj = 0;
                return true;
            } else {
                int rang = uc.getVisionRange();
                if (weapon != Craftable.SWORD && round < 2300) rang = 5;
                enemics = uc.senseUnits(rang, uc.getOpponent());
                if (enemics.length > 0) {
                    Location curr = null;
                    int currlife = 20;
                    for (UnitInfo enemic : enemics) {
                        if (curr == null || enemic.getHealth() < currlife) {
                            curr = enemic.getLocation();
                            currlife = enemic.getHealth();
                        }
                    }
                    if (!curr.equals(obj)) lstLocRound = round;
                    obj = curr;
                    randomObj = 0;
                    return true;
                } else {
                    StructureInfo[] beacons = uc.senseStructures(uc.getVisionRange(), uc.getOpponent(), StructureType.BEACON);
                    if (beacons.length > 0) {
                        Location curr = null;
                        int currlife = 40;
                        for (StructureInfo beacon : beacons) {
                            if (curr == null || beacon.getHealth() < currlife) {
                                curr = beacon.getLocation();
                                currlife = beacon.getHealth();
                            }
                        }
                        if (!curr.equals(obj)) lstLocRound = round;
                        obj = curr;
                        randomObj = 0;
                        return true;
                    } else return false;
                }
            }
        }
    }

    public void searchPotato() {
        MaterialInfo[] materials = uc.senseMaterials(uc.getVisionRange());
        Location nearest = null;
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
        }
    }

    public void getRandomLoc() {
        obj = new Location(getRandomInt(0, mapWidth-1), getRandomInt(0, mapHeight-1));
        randomObj = 1;
        lstLocRound = round;
    }
}

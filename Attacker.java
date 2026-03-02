package myplayer_curr;

import aic2025.user.*;

import java.util.ArrayList;
import java.util.BitSet;

public class Attacker extends Unit {
    Location myLoc;
    Craftable weapon = null;
    Location obj = null;
    int randomObj = 1;
    UnitInfo me;
    int lstLocRound = 0;
    int round;
    boolean isStruct = false;
    int enemyLife = 50;

    public Attacker(UnitController uc, boolean _craftTurret, boolean _goingBed, ArrayList<Integer> _blocked) {
        init(uc);
        getRandomLoc();
        me = uc.getUnitInfo();
        type = "AT";
        craftedTurret = _craftTurret;
        goingBed = _goingBed;
        roundAxe = 200 + Math.max(0, uc.getMapHeight()*uc.getMapWidth()-900)/6;
        blocked = _blocked;
        lstLocRound = uc.getRound();
    }

    public void play() {
        searchedMaterials = false;
        updMatWeight();
        //readBuffer();
        //updBuffer();
        if (!uc.isOnMap() && !type.equals("G")) {
            changeType("G", type);
            return;
        }
        if (type.equals("G")) changeType("AT", "G");
        if (!uc.canAct() || !uc.canMove()) updateBiome();
        tryHeal();
        if (uc.hasCraftable(Craftable.SWORD)) {
            weapon = Craftable.SWORD;
        } else if (uc.canCraft(Craftable.SWORD)) {
            uc.craft(Craftable.SWORD);
            weapon = Craftable.SWORD;
        } else {
            if (AxeOverPickaxe()) {
                if (uc.hasCraftable(Craftable.AXE)) weapon = Craftable.AXE;
                else if (uc.hasCraftable(Craftable.PICKAXE)) weapon = Craftable.PICKAXE;
            } else {
                if (uc.hasCraftable(Craftable.PICKAXE)) weapon = Craftable.PICKAXE;
                else if (uc.hasCraftable(Craftable.AXE)) weapon = Craftable.AXE;
            }
            if (uc.hasCraftable(Craftable.SHOVEL)) weapon = Craftable.SHOVEL;
        }
        if (!uc.hasCraftable(Craftable.ARMOR) && uc.canCraft(Craftable.ARMOR)) {
            uc.craft(Craftable.ARMOR);
        }
        destroy(-1);
        myLoc = uc.getLocation();
        me = uc.getUnitInfo();
        round = uc.getRound();
        if (!searchEnemy() || (round-lstLocRound >= 30 && myLoc.distanceSquared(obj) > 2)){ //maybe remove.
            if (uc.hasCraftable(Craftable.PICKAXE) || uc.hasCraftable(Craftable.AXE)) changeType ("T", "AT");
            else changeType("G", "AT");
            return;
        } else {
            if (!uc.canUseCraftable(weapon, obj)) pathfinding.moveTo(obj);
            attack();
        }
        if (isStruct) {
            tryGatherPotato();
        }
        tryCraftImportant();
        if (uc.getRound() >= GameConstants.MAX_ROUNDS-100) {
            craftAnything();
        }
        tryUpgrade();
        pathfinding.moveTo(obj);
    }
    public boolean searchEnemy() {
        UnitInfo[] enemics = uc.senseUnits(enemyRadius, uc.getOpponent());
        if (enemics.length > 0) {
            Location curr = null;
            enemyLife = 20;
            UnitInfo enemy = null;
            for (UnitInfo enemic : enemics) {
                if (curr == null || (enemic.getHealth() < enemyLife
                        && (myLoc.distanceSquared(curr) > 2 || myLoc.distanceSquared(enemic.getLocation()) <= 2))) { //Si hay alguien pegándome, le pego primero.
                    curr = enemic.getLocation();
                    enemyLife = enemic.getHealth();
                    enemy = enemic;
                }
            }
            if (!curr.equals(obj)) lstLocRound = round;
            obj = curr;
            randomObj = 0;
            isStruct = false;
            destroyNeeded(enemy);
            return true;
        } else{
            StructureInfo[] structs = uc.senseStructures(uc.getVisionRange(), uc.getOpponent(), null);
            if (structs.length > 0) {
                Location curr = null;
                int type = 4;
                enemyLife = 50;
                for (StructureInfo struct : structs) {
                    int myType = -1;
                    if (struct.getType() == StructureType.BED) myType = 1;
                    else if (struct.getType() == StructureType.CHEST) continue; //maybe change bed and chest.
                    else if (struct.getType() == StructureType.BEACON) myType = 3;
                    if (curr == null || myType < type || (myType == type && struct.getHealth() < enemyLife)) {
                        curr = struct.getLocation();
                        enemyLife = struct.getHealth();
                        type = myType;
                    }
                }
                if (!curr.equals(obj)) lstLocRound = round;
                obj = curr;
                randomObj = 0;
                isStruct = true;
                return true;
            } else return false;
        }
    }

    public void tryGatherPotato() {
        if (!uc.canAct()) return;
        if (uc.hasCraftable(Craftable.SHOVEL)) {
            Location myLoc = uc.getLocation();
            for (Direction dir : directions) {
                Location nwObj = myLoc.add(dir);
                if (!uc.canSenseLocation(nwObj)) continue;
                if (uc.senseMaterialAtLocation(nwObj) == Material.POTATO && uc.canUseCraftable(Craftable.SHOVEL, nwObj)) {
                    uc.useCraftable(Craftable.SHOVEL, nwObj);
                    return;
                }
            }
        }
    }

    void destroyNeeded(UnitInfo enemy) {
        CraftableInfo[] crafts = enemy.getCarriedCraftablesArray();
        boolean hasTool = false;
        boolean hasSword = false;
        int potatoes = 0;
        int hisArmor = 0;
        for (CraftableInfo craft : crafts) {
            if (craft.getCraftable() == Craftable.BAKED_POTATO) potatoes++;
            else if ((craft.getCraftable() == Craftable.PICKAXE) || (craft.getCraftable() == Craftable.SHOVEL) || (craft.getCraftable() == Craftable.AXE)) hasTool = true;
            else if (craft.getCraftable() == Craftable.SWORD) hasSword = true;
            else if (craft.getCraftable() == Craftable.ARMOR) hisArmor = 1;
        }
        if (!hasTool && !hasSword) return; //No Threat.
        //From now on, I assume he can hit me.
        boolean meSword = (uc.hasCraftable(Craftable.SWORD) || hvMat[Material.COPPER.ordinal()] >= 3);
        int meArmor = 0;
        if (uc.hasCraftable(Craftable.ARMOR)
                || (uc.hasCraftable(Craftable.SWORD) && hvMat[Material.COPPER.ordinal()] >= 2 && hvMat[Material.LEATHER.ordinal()] >= 2)) meArmor = 1;
        int mePotatoes = hvBakedPotatoes;
        int hisDamage = 1;
        if (hasSword) hisDamage = 3;
        int hisHits; //hits he needs to kill me, I assume he eats all his potatoes.
        if (hisDamage-meArmor != 0) hisHits = (me.getHealth() + 3*mePotatoes)/(hisDamage-meArmor) + potatoes;
        else hisHits = 1000000;
        int meDamage = 1;
        if (meSword) meDamage = 3;
        int myHits; //hits I need to kill him.
        if (meDamage-hisArmor != 0) myHits = (enemy.getHealth() + 3*potatoes)/(meDamage-hisArmor) + mePotatoes;
        else myHits = 1000000;
        int myRounds = (int)Math.ceil(me.getCurrentActionCooldown() + (myHits-1)*((float)me.getWeight()/100));
        int hisRounds = (int)Math.ceil(enemy.getCurrentActionCooldown() + (hisHits-1)*((float)enemy.getWeight()/100));
        int currInd = -1;
        boolean destroyedAllMats = false;
        while ((hisRounds <= myRounds+1 || (hisRounds <= myRounds + 10 && currInd < 2)) && currInd < destroyList.length) {
            if (currInd == -1) {
                destroy(2);
                currInd++;
            } else if (currInd == 2 && !destroyedAllMats){
                destroy(5);
                destroyedAllMats = true;
            } else {
                if (destroyList[currInd] == getMainTool()) {
                    currInd++;
                } else {
                    uc.destroyCraftable(destroyList[currInd]);
                    currInd++;
                }
            }
            myRounds = (int)Math.ceil(me.getCurrentActionCooldown() + (myHits-1)*((float)me.getWeight()/100));
        }
        if (hisRounds < myRounds || (hisRounds == myRounds && uc.getTeam() == Team.B)) {
            attack();
            flee(enemy);
        }
    }

    public void attack() {
        if (!uc.canAct()) return;
        if (isStruct && uc.canUseCraftable(Craftable.TNT, obj)) uc.useCraftable(Craftable.TNT, obj);
        else if (uc.canUseCraftable(Craftable.FIREWORK, obj)) uc.useCraftable(Craftable.FIREWORK, obj);
        else if (isStruct && uc.canCraft(Craftable.TNT)) uc.craft(Craftable.TNT);
        else if ((weapon == Craftable.SWORD || enemyLife <= 2) && uc.canUseCraftable(weapon, obj)) uc.useCraftable(weapon, obj);
        else if (uc.canCraft(Craftable.FIREWORK)) uc.craft(Craftable.FIREWORK);
        else if (uc.canUseCraftable(weapon, obj)) uc.useCraftable(weapon, obj);
    }

    public void getRandomLoc() {
        obj = new Location(getRandomInt(0, mapWidth-1), getRandomInt(0, mapHeight-1));
        randomObj = 1;
        lstLocRound = round;
    }
}
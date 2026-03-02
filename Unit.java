package myplayer_curr;

import aic2025.user.*;

import java.util.ArrayList;
import java.util.BitSet;

public abstract class Unit {
    Direction[] directions = Direction.values();
    Team myTeam, opponentTeam;
    Pathfinding pathfinding;
    UnitController uc;
    MaterialInfo[] materials;
    boolean searchedMaterials = false;
    Material[] whichMat = {Material.DIRT, Material.VOID, Material.GRASS, Material.WATER, Material.STRING, Material.POTATO, Material.WOOD, Material.LEATHER, Material.STONE, Material.COPPER, Material.IRON, Material.GOLD, Material.DIAMOND};
    int[] matWeight = {-10000, -10000, 0, -10000, 5, 1, 5, 0, 2, 1, 3, 4, 5}; //Updates with the game.
    double[][] materialProbability = new double[][]{{0.0F, 0.0F, 0.0F}, {0.0F, 0.0F, 0.0F}, {0.9655F, 0.865F, 0.865F}, {0.0F, 0.0F, 0.0F}, {0.01F, 0.03F, 0.0F}, {0.01F, 0.075F, 0.0F}, {0.03F, 0.06F, 0.0F}, {0.01F, 0.03F, 0.0F}, {0.03F, 0.01F, 0.06F}, {0.01F, 0.0F, 0.06F}, {0.01F, 0.0F, 0.05F}, {0.003F, 0.0F, 0.03F}, {0.0015F, 0.0F, 0.02F}};
    Craftable[] neededTool = {null, null, Craftable.SHOVEL, null, Craftable.AXE, Craftable.SHOVEL, Craftable.AXE, Craftable.AXE, Craftable.PICKAXE, Craftable.PICKAXE, Craftable.PICKAXE, Craftable.PICKAXE, Craftable.PICKAXE};
    Craftable[] destroyList = {Craftable.BOOTS, Craftable.AXE, Craftable.PICKAXE, Craftable.BOAT, Craftable.SHOVEL};
    int[] matLimit = {0, 0, 0, 0, 5, 5, 5, 5, 5, 5, 10, 10, 10};
    int[] hvMat;
    int mapWidth, mapHeight, id;
    int roundAxe = 300;
    int BIOME_RANGE = 10;
    int enemyRadius = 13;
    int pickaxers = 0;
    int shovelers = 0;
    int axers = 0;
    int hvBakedPotatoes = 0;
    Location chestLoc = null;
    ArrayList<Integer> msgs = new ArrayList<>();
    String type = "G";
    Biome currBiome = Biome.DEFAULT;
    boolean craftedTurret = false;
    boolean goingBed = false;
    ArrayList<Integer> blocked;
    BitSet blockedStr;
    public void init(UnitController uc) {
        this.uc = uc;
        myTeam = uc.getTeam();
        opponentTeam = uc.getOpponent();
        mapWidth = uc.getMapWidth();
        mapHeight = uc.getMapHeight();
        id = uc.getID();
        pathfinding = new Pathfinding(uc);
    }

    public boolean isInBlocked(Location loc) {
        return (blockedStr.get(loc.x*uc.getMapHeight()+loc.y));
    }

    public void updBlocked() {
        int thisRound = uc.getRound();
        for (int i = 0; i < (int)blocked.size(); i++) {
            int code = blocked.get(i);
            int x = code%64; code /= 64;
            int y = code%64; code /= 64;
            int round = code;
            if (thisRound-round >= 50) {
                blockedStr.clear(x*uc.getMapHeight()+y);
                blocked.set(i, blocked.get((int)blocked.size()-1));
                blocked.remove((int)blocked.size()-1);
                i--;
            }
        }
    }

    public void changeType(String s, String prev) {
        type = s;
        /*if (type.equals("AX")) {
            msgs.add(3+8);
        } else if (type.equals("P")) {
            msgs.add(2+8);
        } else if (type.equals("S")) {
            msgs.add(1+8);
        } else if (type.equals("AT")) {
            if (prev.equals("AX")) {
                msgs.add(3);
            } else if (prev.equals("P")) {
                msgs.add(2);
            } else if (prev.equals("S")) {
                msgs.add(1);
            }
        }*/
    }

    public void readBuffer() {
        //Code 0: chest location.
        //Code 1: change shoveler.
        //Code 2: change pickaxer.
        //Code 3: change axer.
        //Code 4: Diamonds here.
        //Code 5: Wood here.
        BroadcastInfo msg = uc.pollBroadcast();
        while (msg != null) {
            int ms = msg.getMessage();
            int code = ms%8;
            ms /= 8;
            if (code == 1) {
                if (ms >= 8) shovelers++;
                else shovelers--;
            }
            else if (code == 2) {
                if(ms>= 8) pickaxers++;
                else pickaxers--;
            }
            else if (code == 3) {
                if (ms >= 8) axers++;
                else axers--;
            }
            else if (code == 0) {
                chestLoc = new Location(ms % 64, (ms/64) % 64);
            }
            msg = uc.pollBroadcast();
        }
    }

    public void updBuffer() {
        while (!msgs.isEmpty() && uc.canBroadcastMessage(msgs.get(msgs.size()-1))) {
            uc.broadcastMessage(msgs.get(msgs.size()-1));
            msgs.remove(msgs.size()-1);
        }
    }

    public void updMatWeight() {
        hvMat = uc.getUnitInfo().getCarriedMaterials();
        if (uc.hasCraftable(Craftable.SHOVEL)) matWeight[8] = 1;
        if (uc.hasCraftable(Craftable.AXE) && uc.hasCraftable(Craftable.PICKAXE) && uc.hasCraftable(Craftable.SHOVEL)) matWeight[8] = 0;
        if (uc.hasCraftable(Craftable.PICKAXE)) {
            if (!uc.hasCraftable(Craftable.SWORD)) matWeight[Material.COPPER.ordinal()] = 4;
            else if (!uc.hasCraftable(Craftable.ARMOR)) matWeight[Material.COPPER.ordinal()] = 2;
            else matWeight[Material.COPPER.ordinal()] = 0;
        }
        //CHANGE IF TURRETS.
        if (uc.getRound() == roundAxe+150) {
            matWeight[4] = 2;
            matWeight[6] = 2;
        }
        if (uc.getRound() == 1300) {
            matWeight[10] += 1;
            matWeight[11] += 1;
            matWeight[12] += 1;
        }
        if (uc.getRound() == 2000) {
            matWeight[10] += 1;
            matWeight[11] += 1;
            matWeight[12] += 1;
        }
    }

    public boolean isNearest(Location myLoc, Location nwPos, Location lstPos){
        if(myLoc.distanceSquared(nwPos) < myLoc.distanceSquared(lstPos)) return true;
        return false;
    }

    public int getRandomInt(int l, int r){
        return l + (int)(uc.getRandomDouble() * (r - l + 1));
    }

    public int getDistSquared(int x1, int y1, int x2, int y2){
        int dX = x1-x2;
        int dY = y1-y2;
        return dX * dX + dY * dY;
    }

    void tryToCreate(Craftable craft) {
        for (Direction dir : directions) {
            if (uc.canUseCraftable(craft, uc.getLocation().add(dir))) {
                uc.useCraftable(craft, uc.getLocation().add(dir));
                if (craft == Craftable.CHEST_BLUEPRINT) {
                    msgs.add(chestMessage(uc.getLocation()));
                }
                return;
            }
        }
    }

    void craftAnything() {
        if (!uc.canAct()) return;
        while (uc.canCraft(Craftable.SHOVEL)) {
            uc.craft(Craftable.SHOVEL);
        }
        while (uc.canCraft(Craftable.SWORD)) {
            uc.craft(Craftable.SWORD);
        }
        while (uc.canCraft(Craftable.ARMOR)) {
            uc.craft(Craftable.ARMOR);
        }
        while (uc.canCraft(Craftable.BOAT)) {
            uc.craft(Craftable.BOAT);
        }
        while (uc.canCraft(Craftable.PICKAXE)) {
            uc.craft(Craftable.PICKAXE);
        }
        while (uc.canCraft(Craftable.AXE)) {
            uc.craft(Craftable.AXE);
        }
        while (uc.canCraft(Craftable.BOOTS)) {
            uc.craft(Craftable.BOOTS);
        }
    }

    void tryCraftImportant() {
        if (!uc.canAct()) return;
        CraftableInfo[] craftables = uc.getUnitInfo().getCarriedCraftablesArray();
        for (CraftableInfo crafts : craftables) {
            if (crafts.getCraftable() == Craftable.BED_BLUEPRINT || crafts.getCraftable() == Craftable.CHEST_BLUEPRINT
                    || crafts.getCraftable() == Craftable.BEACON_BLUEPRINT || (uc.getLocation().distanceSquared(uc.getBed()) <= 100 && crafts.getCraftable() == Craftable.COMPOSTER_BLUEPRINT)) {
                tryToCreate(crafts.getCraftable());
            }
        }
        if (uc.canCraft(Craftable.BED_BLUEPRINT)) {
            uc.craft(Craftable.BED_BLUEPRINT);
        }
        if (uc.canCraft(Craftable.BEACON_BLUEPRINT)) {
            uc.craft(Craftable.BEACON_BLUEPRINT);
        }
        if (hvBakedPotatoes < 3 && uc.canCraft(Craftable.BAKED_POTATO)) {
            uc.craft(Craftable.BAKED_POTATO);
            hvBakedPotatoes++;
        }
        if (uc.getLocation().distanceSquared(uc.getBed()) <= 100 && uc.canCraft(Craftable.COMPOSTER_BLUEPRINT)) {
            uc.craft(Craftable.COMPOSTER_BLUEPRINT);
        }
    }

    public void destroy(int lim) {
        for (int i = 0; i < hvMat.length; i++) {
            int limit = matLimit[i];
            if (lim != -1) {
                //I'm fleeing.
                limit = matLimit[i] - lim;
                if (i == Material.COPPER.ordinal()) {
                    limit = 5;
                    if (uc.hasCraftable(Craftable.SWORD)) {
                        limit = 2;
                    }
                    if (uc.hasCraftable(Craftable.ARMOR)) {
                        limit = 0;
                    }
                } else if (i == Material.LEATHER.ordinal()) {
                    limit = 2;
                    if (uc.hasCraftable(Craftable.ARMOR)) limit = 0;
                    limit += hvMat[Material.STRING.ordinal()];
                } else if (i == Material.STRING.ordinal()) {
                    limit += hvMat[Material.LEATHER.ordinal()];
                }
            }
            if (limit < 0) limit = 0;
            while (hvMat[i] > limit) {
                uc.destroyMaterial(whichMat[i]);
                hvMat[i]--;
            }
        }
        //if (currBiome == Biome.CAVE && uc.hasCraftable(Craftable.AXE) && uc.hasCraftable(Craftable.PICKAXE)) uc.destroyCraftable(Craftable.AXE);
    }

    public boolean AxeOverPickaxe() {
        if (currBiome == Biome.CAVE) return false;
        if (uc.getRound() <= roundAxe || currBiome == Biome.FOREST || uc.getRandomDouble() < 0.35) return true;
        else return false;
    }

    public Craftable getMainTool() {
        boolean axe = uc.hasCraftable(Craftable.AXE);
        boolean pickaxe = uc.hasCraftable(Craftable.PICKAXE);
        if (!axe && !pickaxe) return Craftable.SHOVEL;
        if (currBiome == Biome.CAVE && pickaxe) return Craftable.PICKAXE;
        if (currBiome == Biome.FOREST && axe) return Craftable.AXE;
        if (!pickaxe) return Craftable.AXE;
        if (!axe) return Craftable.PICKAXE;
        if (AxeOverPickaxe()) return Craftable.AXE;
        else return Craftable.PICKAXE;
    }

    public void flee(UnitInfo enemy) {
        Direction away = uc.getLocation().directionTo(enemy.getLocation()).opposite();
        pathfinding.moveTo(uc.getLocation().add(away));
    }

    public void tryUpgrade() {
        if (!uc.canAct()) return;
        if (uc.getRound() >= 2000) return;
        int diamonds = hvMat[Material.DIAMOND.ordinal()];
        int gold = hvMat[Material.GOLD.ordinal()];
        int iron = hvMat[Material.IRON.ordinal()];
        Upgrade upgrade = null;
        if (diamonds > 2 && diamonds > 1 + uc.getCraftedBeacons()) {
            upgrade = Upgrade.DIAMOND;
        } else if (gold > 1 + uc.getCraftedBeacons() || gold-diamonds >= 2) {
            upgrade = Upgrade.GOLD;
        } else if (iron > 1 + uc.getCraftedBeacons() || iron-diamonds >= 2) {
            upgrade = Upgrade.IRON;
        }
        if (upgrade == null) return;
        //uc.println("bef upgrade");
        //uc.println(uc.getEnergyLeft());
        int bstWeight = 0;
        Craftable which = null;
        Craftable[] upgradejable = {Craftable.SWORD, Craftable.ARMOR, Craftable.SHOVEL, Craftable.PICKAXE, Craftable.AXE};
        for (Craftable craft : upgradejable) {
            if (!uc.hasCraftable(craft)) continue;
            int weightDiff = craft.getWeight(uc.getUpgrade(craft)) - craft.getWeight(upgrade);
            if (weightDiff == 0) continue;
            if (uc.canUpgrade(craft, upgrade) && (which == null || weightDiff > bstWeight)) {
                which = craft;
                bstWeight = weightDiff;
            }
        }
        if (which != null) uc.upgrade(which, upgrade);
        //uc.println("Aft upgrade");
        //uc.println(uc.getEnergyLeft());
    }

    public void tryHeal() {
        if (!uc.canAct()) return;
        int health = uc.getUnitInfo().getHealth();
        if (health <= 4) {
            if (uc.hasCraftable(Craftable.BAKED_POTATO) && uc.canUseCraftable(Craftable.BAKED_POTATO, uc.getLocation())) {
                uc.useCraftable(Craftable.BAKED_POTATO, uc.getLocation());
                hvBakedPotatoes--;
            }
        }
    }

    public boolean canGatherTool(Location obj) {
        if (!uc.canSenseLocation(obj)) return false;
        Craftable tool = uc.senseMaterialAtLocation(obj).gatheringTool();
        if (tool != null && uc.canUseCraftable(tool, obj)) return true;
        else return false;
    }

    public void updateBiome() {
        /*if (!searchedMaterials) {
            searchedMaterials = true;
            materials = uc.senseMaterials(uc.getVisionRange());
        }
        double probBasic = 1;
        double probC = 1;
        double probF = 1;
        Location myLoc = uc.getLocation();
        for (MaterialInfo matInf : materials) {
            if (matInf.getLocation().distanceSquared(myLoc) > BIOME_RANGE) continue;
            Material mat = matInf.getMaterial();
            if (mat == Material.VOID || mat == Material.WATER || mat == Material.DIRT || mat == null) continue;
            probBasic *= mat.spawnProbability(Biome.DEFAULT);
            probC *= mat.spawnProbability(Biome.CAVE);
            probF *= mat.spawnProbability(Biome.FOREST);
        }
        if (probF > 2*probC && probF > 2*probBasic) currBiome = Biome.FOREST;
        else if (probC > 2*probF && probC > 2*probBasic) currBiome = Biome.CAVE;
        else currBiome = Biome.DEFAULT;*/
    }

    public void tryGatherAround(int randomObj, Location inObj) {
        if (!uc.canAct()) return;
        Location myLoc = uc.getLocation();
        Material currMat = null;
        Location fin = null;
        for (Direction dir : directions) {
            Location obj = myLoc.add(dir);
            if (!uc.canSenseLocation(obj)) continue;
            Material material = uc.senseMaterialAtLocation(obj);
            int i = material.ordinal();
            if (!canGatherTool(obj)) continue;
            int curr = 0;
            if (currMat != null) curr = currMat.ordinal();
            if (material == Material.POTATO && uc.getUnitInfo().getHealth() <= 4 && uc.canUseCraftable(Craftable.SHOVEL, obj)) {
                uc.useCraftable(Craftable.SHOVEL, obj);
                return;
            }
            if (currMat == null || matWeight[i]-hvMat[i] > matWeight[curr]-hvMat[curr]
                    || matWeight[i]-hvMat[i] == matWeight[curr]-hvMat[curr]) {
                currMat = material;
                fin = obj;
            }
        }
        if (fin != null) {
            if (currMat != Material.GRASS && currMat != Material.POTATO) uc.useCraftable(currMat.gatheringTool(), fin);
            else {
                Material myMat = uc.senseMaterialAtLocation(myLoc);
                if ((randomObj == 1 || myLoc.equals(inObj)) && uc.canGather() && ((myMat == Material.WOOD || myMat == Material.STONE) && hvMat[myMat.ordinal()] < 2)) {
                    uc.gather();
                } else {
                    uc.useCraftable(currMat.gatheringTool(), fin);
                }
            }
        } else {
            Material myMat = uc.senseMaterialAtLocation(myLoc);
            if (myMat == Material.POTATO && uc.getUnitInfo().getHealth() <= 4 && uc.canGather()) {
                uc.gather();
            }
            if ((randomObj == 1 || myLoc.equals(inObj)) && myMat != Material.DIRT && uc.canGather() &&
                    (myLoc.distanceSquared(uc.getBed()) <= 49 || ((myMat == Material.WOOD || myMat == Material.STONE) && hvMat[myMat.ordinal()] < 2))) {
                uc.gather();
            }
        }
    }

    boolean havetoGoToChest(UnitController uc){
        int[] materials = uc.getUnitInfo().getCarriedMaterials();
        int a = 0;
        for(int x : materials){
            if(x == Material.COPPER.ordinal()){
                a+= 4;
            }
            if(x == Material.IRON.ordinal()){
                a += 3;
            }
            if(x == Material.GOLD.ordinal()){
                a+= 8;
            }
            if(x == Material.DIAMOND.ordinal()){
                a+= 10;
            }
            if(x == Material.STONE.ordinal()){
                a+= 3;
            }
            if(x == Material.WOOD.ordinal()){
                a+=2;
            }
            if(x == Material.STRING.ordinal()){
                a+=2;
            }
            if(x == Material.LEATHER.ordinal()){
                a+=1;
            }
            if(x == Material.POTATO.ordinal()){
                a+=1;
            }
        }
        return a>=20;
    }
    void goToChest(){
        if(chestLoc == null){
            return;
        }
        if(uc.canSenseLocation(chestLoc)) {
            LeaveInChest();
        }
        pathfinding.moveTo(chestLoc);
    }
    void LeaveInChest(){
        int[] materials = uc.getUnitInfo().getCarriedMaterials();
        for(int x : materials) {
            while (uc.canMoveItemFromChest(true, true, x, 0)) {
                uc.moveItemFromChest(true, true, x,0);
            }
        }
    }
    int chestMessage(Location Loc){
        int Cript = 0;
        Cript+= Loc.x * 8;
        Cript+= Loc.y * 8 * 64;
        return Cript;
    }
    void createChest(){
        if(!uc.canCraft(Craftable.CHEST_BLUEPRINT)) return;
        uc.craft(Craftable.CHEST_BLUEPRINT);
        if(uc.canSenseLocation(uc.getBed())){
            tryToCreate(Craftable.CHEST_BLUEPRINT);
        } else {
            pathfinding.moveTo(uc.getBed());
        }
    }

    abstract void play();
}
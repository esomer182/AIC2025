package myplayer_curr;

import aic2025.user.*;

import java.util.ArrayList;

public abstract class Unit {
    Direction[] directions = Direction.values();
    Team myTeam, opponentTeam;
    Pathfinding pathfinding;
    UnitController uc;
    Material[] whichMat = {Material.DIRT, Material.VOID, Material.GRASS, Material.WATER, Material.STRING, Material.POTATO, Material.WOOD, Material.LEATHER, Material.STONE, Material.COPPER, Material.IRON, Material.GOLD, Material.DIAMOND};
    int[] matWeight = {-10000, -10000, 0, -10000, 5, 1, 5, 0, 2, 1, 3, 4, 5}; //Updates with the game.
    double[][] materialProbability = new double[][]{{0.0F, 0.0F, 0.0F}, {0.0F, 0.0F, 0.0F}, {0.9655F, 0.865F, 0.865F}, {0.0F, 0.0F, 0.0F}, {0.01F, 0.03F, 0.0F}, {0.01F, 0.075F, 0.0F}, {0.03F, 0.06F, 0.0F}, {0.01F, 0.03F, 0.0F}, {0.03F, 0.01F, 0.06F}, {0.01F, 0.0F, 0.06F}, {0.01F, 0.0F, 0.05F}, {0.003F, 0.0F, 0.03F}, {0.0015F, 0.0F, 0.02F}};
    Craftable[] neededTool = {null, null, Craftable.SHOVEL, null, Craftable.AXE, Craftable.SHOVEL, Craftable.AXE, Craftable.AXE, Craftable.PICKAXE, Craftable.PICKAXE, Craftable.PICKAXE, Craftable.PICKAXE, Craftable.PICKAXE};
    int mapWidth, mapHeight, id;
    int pickaxers = 0;
    int shovelers = 0;
    int axers = 0;
    Location chestLoc = null;
    ArrayList<Integer> msgs = new ArrayList<>();
    String type = "G";
    Biome currBiome = Biome.DEFAULT;
    public void init(UnitController uc) {
        this.uc = uc;
        myTeam = uc.getTeam();
        opponentTeam = uc.getOpponent();
        mapWidth = uc.getMapWidth();
        mapHeight = uc.getMapHeight();
        id = uc.getID();
        pathfinding = new Pathfinding(uc);
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
        if (uc.hasCraftable(Craftable.SHOVEL)) matWeight[8] = 1;
        if (uc.hasCraftable(Craftable.AXE) && uc.hasCraftable(Craftable.PICKAXE)) matWeight[8] = -10000;
        if (uc.getRound() == 700) {
            matWeight[4] = 2;
            matWeight[6] = 2;
        }
        if (uc.getRound() == 1300) {
            matWeight[9] += 1;
            matWeight[10] += 1;
            matWeight[11] += 1;
            matWeight[12] += 1;
        }
        if (uc.getRound() == 2000) {
            matWeight[9] += 1;
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
        while (uc.canCraft(Craftable.BOOTS)) {
            uc.craft(Craftable.BOOTS);
        }
        while (uc.canCraft(Craftable.BAKED_POTATO)) {
            uc.craft(Craftable.BAKED_POTATO);
        }
        while (uc.canCraft(Craftable.ARMOR)) {
            uc.craft(Craftable.ARMOR);
        }
        while (uc.canCraft(Craftable.BOAT)) {
            uc.craft(Craftable.BOAT);
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
        if (!uc.hasCraftable(Craftable.BOOTS) && uc.canCraft(Craftable.BOOTS)) {
            uc.craft(Craftable.BOOTS);
        }
        if (!uc.hasCraftable(Craftable.SPYGLASS) && uc.canCraft(Craftable.SPYGLASS)) {
            uc.craft(Craftable.SPYGLASS);
        }
        if (!uc.hasCraftable(Craftable.BAKED_POTATO) && uc.canCraft(Craftable.BAKED_POTATO)) {
            uc.println(uc.hasCraftable(Craftable.BAKED_POTATO));
            uc.craft(Craftable.BAKED_POTATO);
        }
        if (uc.getLocation().distanceSquared(uc.getBed()) <= 100 && uc.canCraft(Craftable.COMPOSTER_BLUEPRINT)) {
            uc.craft(Craftable.COMPOSTER_BLUEPRINT);
        }
        craftables = uc.getUnitInfo().getCarriedCraftablesArray();
        for (CraftableInfo crafts : craftables) {
            if (crafts.getCraftable() == Craftable.BED_BLUEPRINT || crafts.getCraftable() == Craftable.CHEST_BLUEPRINT
            || crafts.getCraftable() == Craftable.BEACON_BLUEPRINT || (uc.getLocation().distanceSquared(uc.getBed()) <= 100 && crafts.getCraftable() == Craftable.COMPOSTER_BLUEPRINT)) {
                tryToCreate(crafts.getCraftable());
            }
        }
    }

    public void destroy() {
        if (uc.getRound() >= 2000) return;
        int hvMat[] = uc.getUnitInfo().getCarriedMaterials();
        for (int i = 0; i < hvMat.length; i++) {
            int limit = 5;
            if (i >= Material.IRON.ordinal()) limit = 10;
            else if (i == Material.STONE.ordinal()) limit = 3;
            else if (i == Material.LEATHER.ordinal() && uc.hasCraftable(Craftable.BOOTS)) limit = 2;
            while (hvMat[i] > limit) {
                uc.destroyMaterial(whichMat[i]);
                hvMat[i]--;
            }
        }
    }

    public void tryUpgrade() {
        if (!uc.canAct()) return;
        if (uc.getRound() >= 2000) return;
        int[] hvMat = uc.getUnitInfo().getCarriedMaterials();
        int diamonds = hvMat[Material.DIAMOND.ordinal()];
        int gold = hvMat[Material.GOLD.ordinal()];
        int iron = hvMat[Material.IRON.ordinal()];
        if (diamonds > 2 && diamonds > 1 + uc.getCraftedBeacons()) {
            Upgrade lowest = null;
            Craftable which = null;
            Craftable[] upgradejable = {Craftable.SHOVEL, Craftable.SWORD, Craftable.ARMOR, Craftable.BOAT, Craftable.AXE, Craftable.PICKAXE};
            for (Craftable craft : upgradejable) {
                if (uc.canUpgrade(craft, Upgrade.DIAMOND) && (lowest == null || uc.getUpgrade(craft).ordinal() < lowest.ordinal())) {
                    lowest = uc.getUpgrade(craft);
                    which = craft;
                }
            }
            if (which != null && uc.canUpgrade(which, Upgrade.DIAMOND)) uc.upgrade(which, Upgrade.DIAMOND);
        } else if ((gold > 2 && gold > 1 + uc.getCraftedBeacons()) || gold-diamonds >= 2) {
            Upgrade lowest = null;
            Craftable which = null;
            Craftable[] upgradejable = {Craftable.SHOVEL, Craftable.SWORD, Craftable.ARMOR, Craftable.BOAT, Craftable.AXE, Craftable.PICKAXE};
            for (Craftable craft : upgradejable) {
                if (uc.canUpgrade(craft, Upgrade.GOLD) && (lowest == null || uc.getUpgrade(craft).ordinal() < lowest.ordinal())) {
                    lowest = uc.getUpgrade(craft);
                    which = craft;
                }
            }
            if (which != null && uc.canUpgrade(which, Upgrade.GOLD)) uc.upgrade(which, Upgrade.GOLD);
        } else if (iron > 2 && iron > 1 + uc.getCraftedBeacons() || iron-diamonds >= 2) {
            Upgrade lowest = null;
            Craftable which = null;
            Craftable[] upgradejable = {Craftable.SHOVEL, Craftable.SWORD, Craftable.ARMOR, Craftable.BOAT, Craftable.AXE, Craftable.PICKAXE};
            for (Craftable craft : upgradejable) {
                if (uc.canUpgrade(craft, Upgrade.IRON) && (lowest == null || uc.getUpgrade(craft).ordinal() < lowest.ordinal())) {
                    lowest = uc.getUpgrade(craft);
                    which = craft;
                }
            }
            if (which != null && uc.canUpgrade(which, Upgrade.IRON)) uc.upgrade(which, Upgrade.IRON);
        }
    }

    public void tryHeal() {
        if (!uc.canAct()) return;
        if (uc.getUnitInfo().getHealth() <= 4) {
            uc.println("Health is less than 4");
            if (uc.hasCraftable(Craftable.BAKED_POTATO)) {
                if (uc.canUseCraftable(Craftable.BAKED_POTATO, uc.getLocation())) {
                    uc.useCraftable(Craftable.BAKED_POTATO, uc.getLocation());
                }
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
        /*MaterialInfo[] mats = uc.senseMaterials(uc.getVisionRange());
        double probBasic = 1;
        double probC = 1;
        double probF = 1;
        for (MaterialInfo matInf : mats) {
            Material mat = matInf.getMaterial();
            if (mat == Material.VOID || mat == Material.WATER || mat == Material.DIRT || mat == null) continue;
            probBasic *= mat.getMaterial().spawnProbability(Biome.DEFAULT);
            probC *= mat.getMaterial().spawnProbability(biome);
            double[] probs = materialProbability[mat.ordinal()];
            probBasic *= probs[Biome.DEFAULT.ordinal()];
            probC *= probs[Biome.CAVE.ordinal()];
            probF *= probs[Biome.FOREST.ordinal()];
        }
        if (probF > probC && probF > probBasic) currBiome = Biome.FOREST;
        else if (probC > probF && probC > probBasic) currBiome = Biome.CAVE;
        else currBiome = Biome.DEFAULT;
        */
    }

    public void tryGatherAround(int randomObj, Location inObj) {
        if (!uc.canAct()) return;
        Location myLoc = uc.getLocation();
        Material currMat = null;
        Location fin = null;
        int[] hvMat = uc.getUnitInfo().getCarriedMaterials();
        for (Direction dir : directions) {
            Location obj = myLoc.add(dir);
            if (!uc.canSenseLocation(obj)) continue;
            Material material = uc.senseMaterialAtLocation(obj);
            int i = material.ordinal();
            if (!canGatherTool(obj)) continue;
            int curr = 0;
            if (currMat != null) curr = currMat.ordinal();
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

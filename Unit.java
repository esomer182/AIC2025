package myplayer;

import aic2025.user.*;

import java.util.ArrayList;

public abstract class Unit {
    Direction[] directions = Direction.values();
    Team myTeam, opponentTeam;
    Pathfinding pathfinding;
    UnitController uc;
    int[] matWeight = {-10000, -10000, 0, -10000, 5, 1, 5, 0, 2, 1, 3, 4, 5}; //Updates with the game.
    Craftable[] neededTool = {null, null, Craftable.SHOVEL, null, Craftable.AXE, Craftable.SHOVEL, Craftable.AXE, Craftable.AXE, Craftable.PICKAXE, Craftable.PICKAXE, Craftable.PICKAXE, Craftable.PICKAXE, Craftable.PICKAXE};
    int mapWidth, mapHeight, id;
    int pickaxers = 0;
    int shovelers = 0;
    int axers = 0;
    Location chestLoc = null;
    ArrayList<Integer> msgs = new ArrayList<>();
    String type = "G";
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
        CraftableInfo[] craftables = uc.getUnitInfo().getCarriedCraftablesArray();
        for (CraftableInfo crafts : craftables) {
            if (crafts.getCraftable() == Craftable.BED_BLUEPRINT || crafts.getCraftable() == Craftable.CHEST_BLUEPRINT
            || crafts.getCraftable() == Craftable.BEACON_BLUEPRINT || (uc.getLocation().distanceSquared(uc.getBed()) <= 100 && crafts.getCraftable() == Craftable.COMPOSTER_BLUEPRINT)) {
                tryToCreate(crafts.getCraftable());
            }
        }
    }

    public void tryUpgrade() {
        int[] hvMat = uc.getUnitInfo().getCarriedMaterials();
        int diamonds = hvMat[Material.DIAMOND.ordinal()];
        int gold = hvMat[Material.DIAMOND.ordinal()];
        int iron = hvMat[Material.DIAMOND.ordinal()];
        if (diamonds > 2 && diamonds > 1 + uc.getCraftedBeacons()) {
            Upgrade lowest = null;
            Craftable craft = null;
            Craftable[] upgradejable = {Craftable.AXE, Craftable.PICKAXE, Craftable.SHOVEL, Craftable.SWORD, Craftable.ARMOR, Craftable.BOAT};
            for (Craftable craft : upgradejable) {
                if (uc.canUpgrade(craft, Upgrade.DIAMOND) && (lowest == null || uc.getUpgrade(craft) < lowest)) {

                }
            }
        } else if (gold > 2 && gold > 1 + uc.getCraftedBeacons()) {

        } else if (iron > 2 && iron > 1 + uc.getCraftedBeacons()) {

        }
    }

    public void tryHeal() {
        if (uc.getUnitInfo().getHealth() <= 4) {
            uc.println("Health is less than 4");
            if (uc.hasCraftable(Craftable.BAKED_POTATO)) {
                uc.useCraftable(Craftable.BAKED_POTATO, uc.getLocation());
            } else if (uc.canCraft(Craftable.BAKED_POTATO)) {
                uc.craft(Craftable.BAKED_POTATO);
                if (uc.canUseCraftable(Craftable.BAKED_POTATO, uc.getLocation())) {
                    uc.useCraftable(Craftable.BAKED_POTATO, uc.getLocation());
                }
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

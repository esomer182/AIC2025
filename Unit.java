package myplayer;

import aic2025.user.*;

import java.util.ArrayList;

public abstract class Unit {
    Direction[] directions = Direction.values();
    Team myTeam, opponentTeam;
    UnitController uc;
    Pathfinding pathfinding;
    int mapWidth, mapHeight, id;
    String type = "G";
    int pickaxers = 0;
    int shovelers = 0;
    int axers = 0;
    Location chestLoc = null;
    ArrayList<Integer> msgs;
    public void init(UnitController uc) {
        this.uc = uc;
        pathfinding = new Pathfinding(uc);
        myTeam = uc.getTeam();
        opponentTeam = uc.getOpponent();
        mapWidth = uc.getMapWidth();
        mapHeight = uc.getMapHeight();
        id = uc.getID();
    }

    public void changeType(String s, String prev) {
        type = s;
        if (type.equals("AX")) {
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
        }
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
                chestLoc.x = ms%64;
                ms /= 64;
                chestLoc.y = ms%64;
                ms /= 64;
            }
        }
    }

    public void updBuffer() {
        while (!msgs.isEmpty() && uc.canBroadcastMessage(msgs.get(msgs.size()-1))) {
            uc.broadcastMessage(msgs.get(msgs.size()-1));
            msgs.remove(msgs.size()-1);
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
    void goToChest(UnitController uc, Location chestLoc){
        if(chestLoc == null){
            return;
        }
        if(uc.canSenseLocation(chestLoc)) {
            LeaveInChest(uc);
        }
        pathfinding.moveTo(chestLoc);
    }
    void LeaveInChest(UnitController uc){
        int[] materials = uc.getUnitInfo().getCarriedMaterials();
        for(int x : materials) {
            if (uc.canMoveItemFromChest(true, true, x, 0)) {
                uc.moveItemFromChest(true, true, x,0);
            }
        }
    }
    int chestMessage(UnitController uc, Location Loc){
        int Cript = 0;
        Cript+= Loc.x * 8;
        Cript+= Loc.y * 8 * 64;
        return Cript;
    }
    void createChest(UnitController uc){
        if(!uc.canCraft(Craftable.CHEST_BLUEPRINT)) return;
        uc.craft(Craftable.CHEST_BLUEPRINT);
        if(uc.canSenseLocation(uc.getBed())){
            tryToCreate(Craftable.CHEST_BLUEPRINT);
            msgs.add(chestMessage(uc, uc.getLocation()));
        } else {
            pathfinding.moveTo(uc.getBed());
        }
    }

    void tryToCreate(Craftable craft) {
        for (Direction dir : directions) {
            if (uc.canUseCraftable(craft, uc.getLocation().add(dir))) {
                uc.useCraftable(craft, uc.getLocation().add(dir));
                return;
            }
        }
    }

    abstract void play();
}

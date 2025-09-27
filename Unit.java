package myplayer;

import aic2025.user.*;

public abstract class Unit {
    Direction[] directions = Direction.values();
    Team myTeam, opponentTeam;
    UnitController uc;
    Pathfinding pathfinding;
    int mapWidth, mapHeight, id;

    public void init(UnitController uc) {
        this.uc = uc;
        pathfinding = new Pathfinding(uc);
        myTeam = uc.getTeam();
        opponentTeam = uc.getOpponent();
        mapWidth = uc.getMapWidth();
        mapHeight = uc.getMapHeight();
        id = uc.getID();
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

    abstract void play();
}
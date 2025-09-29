package myplayer;

import aic2025.user.*;

public class Pathfinding{
    UnitController uc;

    final int INF = 1000000;

    boolean rotateRight = true; //if I should rotate right or left
    Location lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToEnemy = INF; //minimum distance I've been to the enemy while going around an obstacle
    Location prevTarget = null; //previous target

    public Pathfinding (UnitController other) {
        uc = other;
    }

    void moveTo(Location target){
        if (target == null || !uc.canMove()) return;

        //different target? ==> previous data does not help!
        if (prevTarget == null || !target.isEqual(prevTarget)) resetPathfinding();

        //If I'm at a minimum distance to the target, I'm free!
        Location myLoc = uc.getLocation();
        int d = myLoc.distanceSquared(target);
        if (d <= minDistToEnemy) resetPathfinding();

        //Update data
        prevTarget = target;
        minDistToEnemy = Math.min(d, minDistToEnemy);

        //If there's an obstacle I try to go around it [until I'm free] instead of going to the target directly
        Direction dir = myLoc.directionTo(target);
        if (lastObstacleFound != null) dir = myLoc.directionTo(lastObstacleFound);

        //This should not happen for a single unit, but whatever
        if (canMove(dir)) resetPathfinding();

        //I rotate clockwise or counterclockwise (depends on 'rotateRight'). If I try to go out of the map I change the orientation
        //Note that we have to try at most 16 times since we can switch orientation in the middle of the loop. (It can be done more efficiently)
        for (int i = 0; i < 16; ++i){
            if (canMove(dir)){
                move(dir);
                return;
            }
            if (!uc.canMove()) break;
            Location newLoc = myLoc.add(dir);
            if (uc.isOutOfMap(newLoc)) rotateRight = !rotateRight;
                //If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
            else lastObstacleFound = myLoc.add(dir);
            if (rotateRight) dir = dir.rotateRight();
            else dir = dir.rotateLeft();
        }

        if (dir != Direction.ZERO && canMove(dir)) move(dir);
    }

    //clear some of the previous data
    void resetPathfinding(){
        lastObstacleFound = null;
        minDistToEnemy = INF;
    }

    boolean canMove(Direction dir) {
        Location nwLoc = uc.getLocation().add(dir);
        if (uc.canSenseLocation(nwLoc)) {
            StructureInfo enem = uc.senseStructureAtLocation(nwLoc);
            if (enem != null && enem.getType() == StructureType.BED && enem.getTeam() == uc.getOpponent()) return false; //No em moc a llits enemics.
            if (uc.senseMaterialAtLocation(nwLoc) == Material.WATER) {
                if (!uc.hasCraftable(Craftable.BOAT) && uc.canCraft(Craftable.BOAT) && (uc.hasCraftable(Craftable.PICKAXE) || uc.hasCraftable(Craftable.AXE) || uc.getUnitInfo().getCarriedMaterials()[6] >= 5)) {
                    uc.craft(Craftable.BOAT);
                }
            }
        }
        return (uc.canMove(dir) || (uc.getLocation().distanceSquared(prevTarget) > 2 && uc.canUseCraftable(Craftable.BOOTS, nwLoc)));
    }

    void move(Direction dir){
        if (uc.canMove(dir)) uc.move(dir);
        else if (uc.getLocation().distanceSquared(prevTarget) > 2 && uc.canUseCraftable(Craftable.BOOTS, uc.getLocation().add(dir))) {
            uc.useCraftable(Craftable.BOOTS, uc.getLocation().add(dir));
        }
    }
}

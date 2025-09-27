package myplayer;

public class Chest {
    bool havetoGoToChest(UnitController uc){
        auto materials = uc.getCarriedMaterials();
        int a = 0;
        for(auto x : materials){
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
    void goToChest(UnitController uc){
        if(chestLocation == null){
            return;
        }
        if(uc.canSenseLocation(chestLocation) {
            LeaveInChest(uc);
        }
        Pathfinding.moveTo(chestLocation);
    }
    void LeaveInChest(UnitController uc){
        auto materials = uc.getCarriedMaterials();
        for(auto x : materials) {
            if (canMoveItemFromChest(true, true, x)) {
                MoveItemFromChest(true, true, x);
            }
        }
    }
    boolean sentChestMessage(UnitController uc, Location Loc){
        int Cript = 0;
        Cript+= Location.x << 3;
        Cript+= Location.y << 9;
        chestLocation = Location;
        if(!uc.canBroadcastMessage(Cript)) return false;
        uc.broadcastmessage(Cript);
        return true;
    }
    void createChest(UnitControler uc){
        if(!uc.canCraft(Craftable.CHEST_BLUEPRINT) && uc.canUseCraftable(Craftable.CHEST, uc.getLocation())) return
        if(uc.canSenseLocation(uc.getBed())){
            uc.craft(Craftable.CHEST_BLUEPRINT);
            uc.useCraftable(Craftable.CHEST_BLUEPRINT, uc.getLocation());
            sentChestMessage(uc, uc.getLocation());
        }
        else{
            Pathfinding.moveTo(uc.getBed());
        }
    }
}

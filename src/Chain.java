import java.lang.reflect.Array;
import java.util.ArrayList;

public class Chain {
    private ArrayList<Block> blocks;

    public Chain() {
        this.blocks = new ArrayList<>();
    }

    public void addBlock(Block block) {
        this.blocks.add(block);
    }

    public ArrayList<Block> getBlocks() {
        return this.blocks;
    }

    public void print() {
        for (int i = 0; i < blocks.size(); i++) {
            if (i < blocks.size() - 1) {
                System.out.print("[BLOCK " + blocks.get(i).blockHeight +  " (" + blocks.get(i).migrationPlan.size() + " migrations)]---");
            }
            else{
                System.out.println("[BLOCK" + " (" + blocks.get(i).migrationPlan.size() + " migrations)]");
            }
        }
    }
}

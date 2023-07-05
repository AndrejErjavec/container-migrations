import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.TimerTask;

public class Chain {
    private ArrayList<Block> blocks;

    public Chain() {
        this.blocks = new ArrayList<>();
    }

    public void addBlock(Block block) {
        this.blocks.add(block);
    }

    public Block produceEmptyBlock() {
        return new Block(this.blocks.size() + 1, new ArrayList<>());
    }

    public Block currentBlock() {
        if (blocks.size() == 0) return null;
        return blocks.get(blocks.size() - 1);
    }

    public Block previousBlock() {
        if (blocks.size() < 2) return null;
        return blocks.get(blocks.size() - 2);
    }

    public void print() {
        if (this.blocks.size() == 0) {
            System.out.println("empty");
            return;
        }
        for (int i = 0; i < blocks.size(); i++) {
            if (i < blocks.size() - 1) {
                System.out.print("[BLOCK " + blocks.get(i).blockHeight +  " (" + blocks.get(i).migrationPlan.size() + " migrations)]---");
            }
            else{
                System.out.println("[BLOCK " + blocks.get(i).blockHeight + " (" + blocks.get(i).migrationPlan.size() + " migrations)]");
            }
        }
    }
}

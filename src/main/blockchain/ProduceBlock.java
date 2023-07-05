package main.blockchain;

import main.network.Migration;

import java.util.ArrayList;
import java.util.TimerTask;

public class ProduceBlock extends TimerTask {
    private ArrayList<Migration> migrationPlan;
    private Chain chain;

    public ProduceBlock(ArrayList<Migration> migrationPlan, Chain chain) {
        this.migrationPlan = migrationPlan;
        this.chain = chain;
    }

    @Override
    public void run() {
        Block block = new Block(chain.currentBlock().blockHeight + 1, migrationPlan);
        chain.addBlock(block);
    }
}

import java.util.ArrayList;
import java.util.UUID;

public class Block {
    public String id;
    public int blockHeight;
    public ArrayList<Migration> migrationPlan;

    public Block(int blockHeight, ArrayList<Migration> migrationPlan) {
        this.id = UUID.randomUUID().toString();
        this.blockHeight = blockHeight;
        this.migrationPlan = migrationPlan;
    }

    public void addMigrationPlan(ArrayList<Migration> migrationPlan) {
        this.migrationPlan = migrationPlan;
    }

    public void printMigrationPlan() {
        migrationPlan.forEach(migration -> {
            migration.print();
        });
    }

}

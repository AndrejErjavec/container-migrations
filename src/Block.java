import java.util.ArrayList;
import java.util.UUID;

public class Block {
    public String id;
    public int blockHeight;
    public ArrayList<Migration> migrationPlan;

    public Block(ArrayList<Migration> migrationPlan) {
        this.id = UUID.randomUUID().toString();
        this.migrationPlan = migrationPlan;
    }

    public Block(int blockHeight) {
        this.id = UUID.randomUUID().toString();
        this.blockHeight = blockHeight;
        this.migrationPlan = new ArrayList<>();
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

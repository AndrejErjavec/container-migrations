import java.util.ArrayList;
import java.util.UUID;

public class Block {
    public String id;
    public ArrayList<Migration> migrationPlan;

    public Block(ArrayList<Migration> migrationPlan) {
        this.id = UUID.randomUUID().toString();
        this.migrationPlan = migrationPlan;
    }

}

public class Migration {
    public String container;
    public String source;
    public String destination;

    public Migration(String container, String source, String destination) {
        this.container = container;
        this.source = source;
        this.destination = destination;
    }

    public void print() {
        System.out.println("CT: " + container + " | " + source + " --> Node: " + destination);
    }
}

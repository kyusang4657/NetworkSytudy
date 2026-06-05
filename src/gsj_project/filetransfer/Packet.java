package gsj_project.filetransfer;

public class Packet {
    private String type;
    private String filename;
    private int block;
    private String data;
    private String message;

    public Packet(String type, String filename, int block, String data, String message) {
        this.type = type;
        this.filename = filename;
        this.block = block;
        this.data = data;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public String getFilename() {
        return filename;
    }

    public int getBlock() {
        return block;
    }

    public String getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}

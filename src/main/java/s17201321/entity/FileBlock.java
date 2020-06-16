package s17201321.entity;

/**
 * The FileBlock is a class is used to storage file blocks
 * @author 17201321-吴新悦
 */
public class FileBlock {
    private String filename;
    private byte[] data;
    private int No;
    private String validate;

    public FileBlock(){

    }

    public FileBlock(String filename, byte[] data, int no,String validate) {
        this.filename = filename;
        this.data = data;
        this.No = no;
        this.validate = validate;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getNo() {
        return No;
    }

    public void setNo(int no) {
        No = no;
    }

    public String getValidate() {
        return validate;
    }

    public void setValidate(String validate) {
        this.validate = validate;
    }
}

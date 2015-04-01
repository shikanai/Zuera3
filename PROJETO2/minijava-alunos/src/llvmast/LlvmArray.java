package llvmast;
public class LlvmArray extends LlvmType{
    public int length;
    public LlvmType content;
    
    public LlvmArray(int length, LlvmType content){
	this.length = length;
	this.content = content;
    }

    public String toString(){
	return "[" + length + " x " + content + "]";
    }
}
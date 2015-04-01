package llvmast;

public class LlvmPointer extends LlvmType{
    public LlvmType content;
    
    public LlvmPointer(LlvmType content){
	this.content = content;
    }

	public String toString(){
	return content + " *";
    }
}
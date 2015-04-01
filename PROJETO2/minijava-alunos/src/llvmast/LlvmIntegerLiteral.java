package llvmast;
public class LlvmIntegerLiteral extends LlvmValue{
    public int value;
    public LlvmIntegerLiteral(int value){
	type = LlvmPrimitiveType.I32;
	this.value = value;
    }

    public String toString(){
	return ""+ value;
    }
}
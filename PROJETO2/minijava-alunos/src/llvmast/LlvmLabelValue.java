package llvmast;
public class LlvmLabelValue extends LlvmValue{
    public String value;
    public LlvmLabelValue(String value){
	type = LlvmPrimitiveType.LABEL;
	this.value = value;
    }

    public String toString(){
	return ""+ value;
    }
}
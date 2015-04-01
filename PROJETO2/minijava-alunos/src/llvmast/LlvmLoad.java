package llvmast;
public class LlvmLoad extends LlvmInstruction{
    public LlvmValue lhs;
    public LlvmValue address; // includes its type

    public LlvmLoad(LlvmValue lhs, LlvmValue address){
	this.lhs=lhs;
	this.address=address;
    }
    
    public String toString(){
	return "  " + lhs + " = load " + address.type + " " + address;
    }
}

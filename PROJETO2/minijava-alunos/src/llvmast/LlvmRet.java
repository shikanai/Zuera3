package llvmast;
public class LlvmRet extends LlvmInstruction{
    public LlvmValue v;
    public LlvmRet(LlvmValue v){
	this.v = v;
    }

    public String toString(){
	return "  ret " + v.type + " " + v;
    }

}
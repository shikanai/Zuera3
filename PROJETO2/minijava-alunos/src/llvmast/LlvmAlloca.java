package llvmast;
import java.util.*;
public  class LlvmAlloca extends LlvmInstruction{
    public LlvmValue lhs;
    public LlvmType type;
    public List<LlvmValue> numbers;

    public LlvmAlloca(LlvmValue lhs, LlvmType type, List<LlvmValue> numbers){
	this.lhs = lhs;
	this.type = type;
	this.numbers = numbers;
    }

    public String toString(){
	String nrs = "";
	for(LlvmValue v : numbers)
	    nrs = nrs + ", " + v.type + " " + v;
	return "  " + lhs + " = alloca " + type + nrs;
    }
}

package llvmast;
import  java.util.*;
public class LlvmBitcast extends LlvmInstruction{
    public LlvmValue lhs;
    public LlvmValue source;
    public LlvmType toType;

    public LlvmBitcast(LlvmValue lhs, LlvmValue source, LlvmType toType){
	this.lhs = lhs;
	this.source = source;
	this.toType = toType;
    }
    
    public String toString(){
    	return "  " + lhs + " = bitcast " + source.type + " " + source +" to " + toType;
    }

}

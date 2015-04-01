package llvmast;
import java.util.*;

public  class LlvmCall extends LlvmInstruction{
    public LlvmRegister lhs;
    public LlvmType type;
    public LlvmPointer fnType = null;
    public List<LlvmType> fnTypeList = null;
    public String fnName;
    public List<LlvmValue> args;

    public LlvmCall(LlvmRegister lhs, LlvmType type, LlvmPointer fnType, String fnName, List<LlvmValue> args){
	this.lhs = lhs;
	this.type = type;
	this.fnType = fnType;
	this.fnName = fnName;
	this.args = args;
    }

    public LlvmCall(LlvmRegister lhs, LlvmType type, String fnName, List<LlvmValue> args){
	this.lhs = lhs;
	this.type = type;
	this.fnName = fnName;
	this.args = args;
    }

    public LlvmCall(LlvmRegister lhs, LlvmType type, List<LlvmType> fnType, String fnName, List<LlvmValue> args) {
    	this.lhs = lhs;
    	this.type = type;
    	this.fnTypeList = fnType;
    	this.fnName = fnName;
    	this.args = args;	
    	}

	public String toString(){

	String arguments = "";
	for(int i = 0; i<args.size(); i++){
	    arguments = arguments + args.get(i).type + " " + args.get(i);
	    if(i+1<args.size()) 
		arguments = arguments + ", ";

	}	

	String fnTypeResult = "";
	if (fnTypeList != null)
	{
		fnTypeResult += "(";
		for(int i = 0; i<fnTypeList.size(); i++){
			fnTypeResult = fnTypeResult + fnTypeList.get(i);
		    if(i+1<fnTypeList.size()) 
			fnTypeResult = fnTypeResult + ", ";
		}
		fnTypeResult += ")*";
	} else 
	{
		if (fnType != null)
			fnTypeResult = fnType.toString();
	}

	return "  " + lhs + " = " + "call " + type + " " + fnTypeResult + " " + fnName +  "(" + arguments + ")"; 
    }
}

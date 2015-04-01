package llvmast;
import java.util.*;
public class LlvmDefine extends LlvmInstruction{
    public String name;
    public LlvmType resultType;
    public List<LlvmValue> args;
    
    public LlvmDefine(String name, LlvmType resultType, List<LlvmValue> args){
	this.name = name;
	this.resultType = resultType;
	this.args = args;
    }

    public String toString(){
	String arguments = "";
	for(int i = 0; i<args.size(); i++){
	    arguments = arguments + args.get(i).type + " " + args.get(i);
	    if(i+1<args.size()) 
		arguments = arguments + ", ";
	    
	}
	return "define " + resultType + " " + name + "(" + arguments + ") {";
    }
}

package llvmast;
import java.util.*;
public class LlvmExternalDeclaration extends LlvmInstruction{
    public String name;
    public LlvmType resultType;
    public List<LlvmType> parameterTypes;

    public LlvmExternalDeclaration(String name,LlvmType resultType, List<LlvmType> parameterTypes){
	this.name = name;
	this.resultType = resultType;
	this.parameterTypes = parameterTypes;
    }
    
    public String toString(){
	String argTypes = "";
	for(int i = 0; i<parameterTypes.size(); i++){
	    argTypes = argTypes + parameterTypes.get(i);
	    if(i+1<parameterTypes.size()) 
		argTypes = argTypes + ", ";

	}
	return "declare " + resultType + " " + name + " (" + argTypes + ")";
    }
}